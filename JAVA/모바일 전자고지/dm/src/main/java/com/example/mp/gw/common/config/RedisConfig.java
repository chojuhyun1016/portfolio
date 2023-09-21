package com.example.mp.gw.common.config;


import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : RedisConfig.java
 * @Description : Redis 연동관련 Configuration 파일
 * 
 * @author 조주현
 * @since 2021. 3. 8.
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일		 수정자             수정내용
 *  -----------  -------------    ---------------------------
 *  2021. 3. 8.	 조주현             최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
@Configuration
public class RedisConfig
{
    @Value("${spring.redis.host1}")
    private String redisHost1;

    @Value("${spring.redis.port1}")
    private int redisPort1;

    @Value("${spring.redis.host2}")
    private String redisHost2;

    @Value("${spring.redis.port2}")
    private int redisPort2;

    @Value("${spring.redis.host3}")
    private String redisHost3;

    @Value("${spring.redis.port3}")
    private int redisPort3;

    // 패스워드 있으면 설정
	@Value("${spring.redis.password}")
	private String redisPwd;


    @Bean
    public RedisConnectionFactory redisConnectionFactory()
    {
    	/* Sentinel 사용하지 않고 바로 Redis 접속하는 경우
    	log.info("======================> RedisConnectionFactory Start");
    	
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisHost);
        redisStandaloneConfiguration.setPort(redisPort);
        // 패스워드 있으면 설정
        redisStandaloneConfiguration.setPassword(redisPwd);
        
        
        LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
	        														.commandTimeout(Duration.ofSeconds(1))
	        														.shutdownTimeout(Duration.ofSeconds(1))
        														.build();

    	log.debug("======================> RedisConnectionFactory End");
        return new LettuceConnectionFactory(redisStandaloneConfiguration,lettuceClientConfiguration);
        */

        log.info("======================> RedisConnectionFactory Start");

        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration()
        		.master("mymaster") 
        		.sentinel(redisHost1, redisPort1) 
        		.sentinel(redisHost2, redisPort2) 
        		.sentinel(redisHost3, redisPort3);

        redisSentinelConfiguration.setPassword(redisPwd);

        LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
																	.commandTimeout(Duration.ofSeconds(10))
																	.shutdownTimeout(Duration.ZERO)
																.build();

		LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisSentinelConfiguration, lettuceClientConfiguration);

        log.info("======================> RedisConnectionFactory End");

        return lettuceConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate()
    {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return redisTemplate;
    }
}
