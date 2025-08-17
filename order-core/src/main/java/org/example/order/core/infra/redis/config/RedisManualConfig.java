package org.example.order.core.infra.redis.config;

import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import lombok.RequiredArgsConstructor;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.example.order.core.infra.redis.props.RedisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;

/**
 * Redis 수동(Manual) 구성
 */
@Configuration
@Profile("!test")
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnExpression(
        "'${spring.redis.enabled:false}'=='true' && " +
                "'${spring.redis.uri:}'.length() > 0 && " +
                "'${spring.redis.host:}'.length() == 0 && " +
                "'${spring.redis.port:}'.length() == 0"
)
public class RedisManualConfig {

    private final RedisProperties props;
    private final Environment env;

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory() {
        if (props.getUri() == null || props.getUri().isBlank()) {
            throw new IllegalStateException("Manual mode requires 'spring.redis.uri'.");
        }

        // timeouts
        int commandTimeout = props.getCommandTimeoutSeconds() != null ? props.getCommandTimeoutSeconds() : 3;
        int shutdownTimeout = props.getShutdownTimeoutSeconds() != null ? props.getShutdownTimeoutSeconds() : 3;

        // lettuce pool config
        GenericObjectPoolConfig<?> pool = new GenericObjectPoolConfig<>();
        pool.setMaxTotal(props.getPoolMaxActive() != null ? props.getPoolMaxActive() : 64);
        pool.setMaxIdle(props.getPoolMaxIdle() != null ? props.getPoolMaxIdle() : 32);
        pool.setMinIdle(props.getPoolMinIdle() != null ? props.getPoolMinIdle() : 8);
        pool.setMaxWaitMillis(props.getPoolMaxWait() != null ? props.getPoolMaxWait() : 2000L);

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder clientBuilder =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(pool)
                        .commandTimeout(Duration.ofSeconds(commandTimeout))
                        .shutdownTimeout(Duration.ofSeconds(shutdownTimeout))
                        .clientResources(ClientResources.create());

        // ✅ clientName 안전 처리 및 디폴트 자동추론
        String clientName = resolveClientName();

        if (StringUtils.hasText(clientName)) {
            clientBuilder.clientName(clientName);
        }

        // URI 파싱
        RedisURI redisURI = RedisURI.create(URI.create(props.getUri()));

        if (redisURI.isSsl()) {
            clientBuilder.useSsl();
        }

        // RedisURI -> Standalone 매핑
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        standalone.setHostName(redisURI.getHost());
        standalone.setPort(redisURI.getPort());

        // username(ACL) — 리플렉션으로 setUsername(String) 존재 시 호출 (버전 호환)
        if (redisURI.getUsername() != null && !redisURI.getUsername().isBlank()) {
            try {
                Method m = RedisStandaloneConfiguration.class.getMethod("setUsername", String.class);
                m.invoke(standalone, redisURI.getUsername());
            } catch (Exception ignored) {
                // setUsername 미지원 버전은 무시
            }
        }

        // password
        if (redisURI.getPassword() != null && redisURI.getPassword().length > 0) {
            standalone.setPassword(RedisPassword.of(new String(redisURI.getPassword())));
        }

        // database
        if (redisURI.getDatabase() >= 0) {
            standalone.setDatabase(redisURI.getDatabase());
        }

        return new LettuceConnectionFactory(standalone, clientBuilder.build());
    }

    /**
     * Auto 구성과 동일한 우선순위로 clientName 결정
     */
    private String resolveClientName() {
        if (StringUtils.hasText(props.getClientName())) {
            return props.getClientName().trim();
        }

        if (Boolean.FALSE.equals(props.getEnableDefaultClientName())) {
            return null; // 호출 생략
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
