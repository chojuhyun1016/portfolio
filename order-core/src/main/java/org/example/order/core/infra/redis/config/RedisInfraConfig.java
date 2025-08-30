package org.example.order.core.infra.redis.config;

import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.example.order.core.infra.redis.props.RedisProperties;
import org.example.order.core.infra.redis.repository.RedisRepository;
import org.example.order.core.infra.redis.repository.impl.RedisRepositoryImpl;
import org.example.order.core.infra.redis.support.RedisSerializerFactory;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
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
 * Redis 단일 인프라 구성 (설정 기반 + @Import 조립)
 *
 * 전역 스위치:
 *   - spring.redis.enabled=true 일 때만 전체 모듈 동작
 *
 * 엔드포인트 규칙:
 *   - spring.redis.uri 가 있으면 우선 (rediss:// 지원)
 *   - 없으면 spring.redis.host/port 를 사용
 *   - 둘 다 없으면 명확한 예외 발생 (로컬 디폴트로 숨은 연결 금지)
 *
 * 빈 등록:
 *   - RedisConnectionFactory (Lettuce, Pooling)
 *   - RedisTemplate<String,Object> (JSON Serializer)
 *   - RedisRepository (기본 연산) — 위 템플릿 존재 시
 *
 * 주의:
 *   - 컴포넌트 스캔 미사용. 설정 클래스에서만 조건부 등록
 *   - 기존 @Component 달린 구현체들은 제거/평문화하고, 여기서 @Bean 으로만 등록
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(prefix = "spring.redis", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RedisInfraConfig {

    private final RedisProperties props;
    private final Environment env;

    /* -----------------------------------------------------------
     * ConnectionFactory (Lettuce, Pooling)
     * ----------------------------------------------------------- */
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory() {
        // 필수 엔드포인트 검증
        final boolean hasUri = StringUtils.hasText(props.getUri());
        final boolean hasHost = StringUtils.hasText(props.getHost());

        if (!hasUri && !hasHost) {
            throw new IllegalStateException(
                    "Redis is enabled but no endpoint provided. Please set either 'spring.redis.uri' or 'spring.redis.host/port'."
            );
        }

        // timeouts
        int commandTimeout = defaultIfNull(props.getCommandTimeoutSeconds(), 3);
        int shutdownTimeout = defaultIfNull(props.getShutdownTimeoutSeconds(), 3);

        // lettuce pool config (commons-pool2)
        GenericObjectPoolConfig<?> pool = new GenericObjectPoolConfig<>();
        pool.setMaxTotal(defaultIfNull(props.getPoolMaxActive(), 64));
        pool.setMaxIdle(defaultIfNull(props.getPoolMaxIdle(), 32));
        pool.setMinIdle(defaultIfNull(props.getPoolMinIdle(), 8));
        pool.setMaxWaitMillis(defaultIfNull(props.getPoolMaxWait(), 2000L));

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder clientBuilder =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(pool)
                        .commandTimeout(Duration.ofSeconds(commandTimeout))
                        .shutdownTimeout(Duration.ofSeconds(shutdownTimeout))
                        .clientResources(ClientResources.create());

        // clientName 우선순위에 따른 안전 결정
        String clientName = resolveClientName();
        if (StringUtils.hasText(clientName)) {
            clientBuilder.clientName(clientName);
        }

        if (hasUri) {
            // 1) URI 우선 (rediss:// 지원)
            RedisURI redisURI = RedisURI.create(URI.create(props.getUri()));

            if (redisURI.isSsl()) {
                clientBuilder.useSsl();
            }

            RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
            standalone.setHostName(redisURI.getHost());
            standalone.setPort(redisURI.getPort());

            // username(ACL) — setUsername 이 있는 Spring Data Redis 버전에서만 반영
            if (StringUtils.hasText(redisURI.getUsername())) {
                try {
                    Method m = RedisStandaloneConfiguration.class.getMethod("setUsername", String.class);
                    m.invoke(standalone, redisURI.getUsername());
                } catch (Exception ignored) {
                    // 호환용: 메서드 없는 버전은 무시
                }
            }

            // password
            if (redisURI.getPassword() != null && redisURI.getPassword().length > 0) {
                standalone.setPassword(RedisPassword.of(new String(redisURI.getPassword())));
            }

            // database index
            if (redisURI.getDatabase() >= 0) {
                standalone.setDatabase(redisURI.getDatabase());
            }

            return new LettuceConnectionFactory(standalone, clientBuilder.build());
        }

        // 2) host/port 기반
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

    /* -----------------------------------------------------------
     * RedisTemplate<String,Object>
     * ----------------------------------------------------------- */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);

        // JSON Serializer (기본 신뢰 패키지 사용)
        String trusted = StringUtils.hasText(props.getTrustedPackage())
                ? props.getTrustedPackage()
                : "org.example.order";

        var json = RedisSerializerFactory.create(trusted);
        var str = new StringRedisSerializer();

        template.setKeySerializer(str);
        template.setValueSerializer(json);
        template.setHashKeySerializer(str);
        template.setHashValueSerializer(json);

        template.afterPropertiesSet();
        return template;
    }

    /* -----------------------------------------------------------
     * RedisRepository (기본 연산)
     * ----------------------------------------------------------- */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean
    public RedisRepository redisRepository(RedisTemplate<String, Object> redisTemplate) {
        return new RedisRepositoryImpl(redisTemplate);
    }

    /* ---------------- helpers ---------------- */
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
     * 6) "order-core"
     */
    private String resolveClientName() {
        if (StringUtils.hasText(props.getClientName())) {
            return props.getClientName().trim();
        }
        if (Boolean.FALSE.equals(props.getEnableDefaultClientName())) {
            return null; // Lettuce clientName 호출 자체 생략 → 예외 방지
        }
        if (StringUtils.hasText(props.getDefaultClientName())) {
            return props.getDefaultClientName().trim();
        }
        String appName = env.getProperty("spring.application.name");
        if (StringUtils.hasText(appName)) {
            return appName.trim();
        }
        try {
            String host = InetAddress.getLocalHost().getHostName();
            if (StringUtils.hasText(host)) return host;
        } catch (Exception ignored) {}
        return "order-core";
    }
}
