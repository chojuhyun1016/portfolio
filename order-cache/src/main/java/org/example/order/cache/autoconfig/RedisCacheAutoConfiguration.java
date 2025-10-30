package org.example.order.cache.autoconfig;

import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.example.order.cache.autoconfig.props.CacheToggleProperties;
import org.example.order.cache.core.props.OrderRedisProperties;
import org.example.order.cache.core.redis.RedisRepository;
import org.example.order.cache.core.redis.impl.RedisRepositoryImpl;
import org.example.order.cache.core.support.RedisSerializerFactory;
import org.example.order.cache.feature.order.repository.OrderCacheRepository;
import org.example.order.cache.feature.order.repository.impl.OrderCacheRepositoryImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;

/**
 * RedisCacheAutoConfiguration
 * ------------------------------------------------------------------------
 * 역할
 * - Redis 기반의 캐시 인프라 자동 구성.
 * - RedisConnectionFactory, RedisTemplate, Repository 계층 구성.
 * <p>
 * 활성화 조건 (명시적)
 * - order.cache.enabled=true              (전역 캐시 토글)
 * - order.cache.redis.enabled=true        (Redis 구현 토글)
 * <p>
 * 설정 소스
 * - order.cache.redis.* 만 지원 (B안: spring.redis.* 제거)
 * <p>
 * 비고
 * - endpoint 미설정시 명시적으로 실패(로컬 디폴트 연결 금지).
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties({OrderRedisProperties.class, CacheToggleProperties.class})
@ConditionalOnExpression("'${order.cache.enabled:false}'=='true' && '${order.cache.redis.enabled:false}'=='true'")
public class RedisCacheAutoConfiguration {

    private static final String DEFAULT_CLIENT_NAME = "order-cache";

    private final OrderRedisProperties orderProps;
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
        final boolean hasUri = StringUtils.hasText(orderProps.getUri());
        final boolean hasHost = StringUtils.hasText(orderProps.getHost());

        if (!hasUri && !hasHost) {
            throw new IllegalStateException(
                    "Redis is enabled but no endpoint provided. "
                            + "Set 'order.cache.redis.uri' or 'order.cache.redis.host/port'."
            );
        }

        int commandTimeout = coalesce(orderProps.getCommandTimeoutSeconds(), 3);
        int shutdownTimeout = coalesce(orderProps.getShutdownTimeoutSeconds(), 3);

        GenericObjectPoolConfig<?> pool = new GenericObjectPoolConfig<>();
        pool.setMaxTotal(coalesce(orderProps.getPoolMaxActive(), 64));
        pool.setMaxIdle(coalesce(orderProps.getPoolMaxIdle(), 32));
        pool.setMinIdle(coalesce(orderProps.getPoolMinIdle(), 8));
        pool.setMaxWait(Duration.ofMillis(coalesce(orderProps.getPoolMaxWait(), 2000L)));

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder clientBuilder =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(pool)
                        .commandTimeout(Duration.ofSeconds(commandTimeout))
                        .shutdownTimeout(Duration.ofSeconds(shutdownTimeout))
                        .clientResources(clientResources);

        String clientName = resolveClientName(orderProps);

        if (StringUtils.hasText(clientName)) {
            clientBuilder.clientName(clientName);
        }

        if (hasUri) {
            RedisURI redisURI = RedisURI.create(URI.create(orderProps.getUri()));

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
                } catch (Exception ignored) { /* no-op */ }
            }

            if (redisURI.getPassword() != null && redisURI.getPassword().length > 0) {
                standalone.setPassword(RedisPassword.of(new String(redisURI.getPassword())));
            }

            if (redisURI.getDatabase() >= 0) {
                standalone.setDatabase(redisURI.getDatabase());
            }

            return new LettuceConnectionFactory(standalone, clientBuilder.build());
        }

        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration(orderProps.getHost(), coalesce(orderProps.getPort(), 6379));

        if (StringUtils.hasText(orderProps.getPassword())) {
            standalone.setPassword(RedisPassword.of(orderProps.getPassword()));
        }

        if (orderProps.getDatabase() != null) {
            standalone.setDatabase(orderProps.getDatabase());
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

        final String trusted = StringUtils.hasText(orderProps.getTrustedPackage())
                ? orderProps.getTrustedPackage()
                : OrderRedisProperties.DEFAULT_TRUSTED_PACKAGE;

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

    private String resolveClientName(OrderRedisProperties p) {
        if (StringUtils.hasText(p.getClientName())) {
            return p.getClientName().trim();
        }

        if (Boolean.FALSE.equals(p.getEnableDefaultClientName())) {
            return null;
        }

        if (StringUtils.hasText(p.getDefaultClientName())) {
            return p.getDefaultClientName().trim();
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
        } catch (Exception ignored) { /* no-op */ }

        return DEFAULT_CLIENT_NAME;
    }

    private static <T> T coalesce(T v, T def) {
        return v != null ? v : def;
    }
}
