package org.example.order.cache.autoconfig;

import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.example.order.cache.core.props.RedisProperties;
import org.example.order.cache.core.redis.RedisRepository;
import org.example.order.cache.core.redis.impl.RedisRepositoryImpl;
import org.example.order.cache.core.support.RedisSerializerFactory;
import org.example.order.cache.feature.order.repository.OrderCacheRepository;
import org.example.order.cache.feature.order.repository.impl.OrderCacheRepositoryImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;

/**
 * Cache 단일 오토컨피그 (Redis + 도메인 캐시 빈)
 * <p>
 * 전역 스위치:
 * - spring.redis.enabled=true 일 때만 전체 모듈 동작
 * <p>
 * 엔드포인트 규칙:
 * - spring.redis.uri 가 있으면 우선 (rediss:// 지원)
 * - 없으면 spring.redis.host/port 를 사용
 * - 둘 다 없으면 명확한 예외 발생 (로컬 디폴트 연결 금지)
 * <p>
 * 빈 등록:
 * - RedisConnectionFactory (Lettuce, Pooling)
 * - RedisTemplate<String,Object> (JSON Serializer)
 * - RedisRepository (기본 연산)
 * - OrderCacheRepository (도메인 캐시 리포)
 * <p>
 * 주의:
 * - 컴포넌트 스캔 미사용. 조건부 등록만 수행
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(prefix = "spring.redis", name = "enabled", havingValue = "true", matchIfMissing = false)
public class CacheAutoConfiguration {

    private final RedisProperties props;
    private final Environment env;

    /* ======================================================================
     * Lettuce ClientResources (수명주기 관리)
     * ==================================================================== */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public ClientResources lettuceClientResources() {
        return ClientResources.create();
    }

    /* ======================================================================
     * RedisConnectionFactory (Lettuce, Pooling)
     * ==================================================================== */
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory(ClientResources clientResources) {
        final boolean hasUri = StringUtils.hasText(props.getUri());
        final boolean hasHost = StringUtils.hasText(props.getHost());

        if (!hasUri && !hasHost) {
            throw new IllegalStateException(
                    "Redis is enabled but no endpoint provided. Please set either 'spring.redis.uri' or 'spring.redis.host/port'."
            );
        }

        int commandTimeout = defaultIfNull(props.getCommandTimeoutSeconds(), 3);
        int shutdownTimeout = defaultIfNull(props.getShutdownTimeoutSeconds(), 3);

        // commons-pool2 2.12.x API (Duration 기반)
        GenericObjectPoolConfig<?> pool = new GenericObjectPoolConfig<>();
        pool.setMaxTotal(defaultIfNull(props.getPoolMaxActive(), 64));
        pool.setMaxIdle(defaultIfNull(props.getPoolMaxIdle(), 32));
        pool.setMinIdle(defaultIfNull(props.getPoolMinIdle(), 8));
        pool.setMaxWait(Duration.ofMillis(defaultIfNull(props.getPoolMaxWait(), 2000L)));

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder clientBuilder =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(pool)
                        .commandTimeout(Duration.ofSeconds(commandTimeout))
                        .shutdownTimeout(Duration.ofSeconds(shutdownTimeout))
                        // 누수 방지: 외부 빈으로 수명주기 관리
                        .clientResources(clientResources);

        String clientName = resolveClientName();

        if (StringUtils.hasText(clientName)) {
            clientBuilder.clientName(clientName);
        }

        if (hasUri) {
            RedisURI redisURI = RedisURI.create(URI.create(props.getUri()));

            if (redisURI.isSsl()) {
                clientBuilder.useSsl();
            }

            RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
            standalone.setHostName(redisURI.getHost());
            standalone.setPort(redisURI.getPort());

            if (StringUtils.hasText(redisURI.getUsername())) {
                try {
                    Method m = RedisStandaloneConfiguration.class.getMethod("setUsername", String.class);
                    m.invoke(standalone, redisURI.getUsername());
                } catch (Exception ignored) {
                }
            }

            if (redisURI.getPassword() != null && redisURI.getPassword().length > 0) {
                standalone.setPassword(RedisPassword.of(new String(redisURI.getPassword())));
            }

            if (redisURI.getDatabase() >= 0) {
                standalone.setDatabase(redisURI.getDatabase());
            }

            return new LettuceConnectionFactory(standalone, clientBuilder.build());
        }

        String host = props.getHost();
        int port = defaultIfNull(props.getPort(), 6379);

        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);

        if (StringUtils.hasText(props.getPassword())) {
            standalone.setPassword(RedisPassword.of(props.getPassword()));
        }

        if (props.getDatabase() != null) {
            standalone.setDatabase(props.getDatabase());
        }

        return new LettuceConnectionFactory(standalone, clientBuilder.build());
    }

    /* ======================================================================
     * RedisTemplate<String, Object>
     * ==================================================================== */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);

        String trusted = StringUtils.hasText(props.getTrustedPackage())
                ? props.getTrustedPackage()
                : "org.example.order.cache";

        var json = RedisSerializerFactory.create(trusted);
        var str = new StringRedisSerializer();

        template.setKeySerializer(str);
        template.setValueSerializer(json);
        template.setHashKeySerializer(str);
        template.setHashValueSerializer(json);

        template.afterPropertiesSet();

        return template;
    }

    /* ======================================================================
     * RedisRepository (기본 연산)
     * ==================================================================== */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean
    public RedisRepository redisRepository(RedisTemplate<String, Object> redisTemplate) {
        return new RedisRepositoryImpl(redisTemplate);
    }

    /* ======================================================================
     * OrderCacheRepository (도메인 캐시 리포지토리)
     * ==================================================================== */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean
    public OrderCacheRepository orderCacheRepository(RedisTemplate<String, Object> redisTemplate) {
        log.info("[order-cache] OrderCacheRepository initialized");
        return new OrderCacheRepositoryImpl(redisTemplate);
    }

    /* ------------------------ helpers ------------------------ */

    private static Integer defaultIfNull(Integer v, Integer def) {
        return v != null ? v : def;
    }

    private static Long defaultIfNull(Long v, Long def) {
        return v != null ? v : def;
    }

    /**
     * clientName 결정 로직 (우선순위):
     * 1) spring.redis.client-name
     * 2) enableDefaultClientName=false → 사용 안 함
     * 3) spring.redis.default-client-name
     * 4) spring.application.name
     * 5) 호스트네임
     * 6) "order-cache"
     */
    private String resolveClientName() {
        if (StringUtils.hasText(props.getClientName())) {
            return props.getClientName().trim();
        }

        if (Boolean.FALSE.equals(props.getEnableDefaultClientName())) {
            return null;
        }

        String fallback = env.getProperty("spring.redis.default-client-name");

        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }

        String appName = env.getProperty("spring.application.name");

        if (StringUtils.hasText(appName)) {
            return appName.trim();
        }

        try {
            String host = InetAddress.getLocalHost().getHostName();
            if (StringUtils.hasText(host)) {
                return host;
            }
        } catch (Exception ignored) {
        }

        return "order-cache";
    }
}
