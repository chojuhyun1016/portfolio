package org.example.order.api.common.autoconfigure;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.example.order.common.kafka.MdcToHeaderProducerInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * API 측 Kafka Producer에 MDC→헤더 인터셉터를 자동 적용한다.
 * <p>
 * - 컨텍스트에 생성된 DefaultKafkaProducerFactory를 후처리하면서
 * producer 설정의 "interceptor.classes"에 공용 인터셉터를 중복 없이 추가한다.
 * - Spring Boot/Spring Kafka 버전에 덜 민감(부트의 커스터마이저 타입 유무와 무관).
 * - 기존 사용자 정의 설정은 보존한다.
 */
@AutoConfiguration
@ConditionalOnClass({DefaultKafkaProducerFactory.class, MdcToHeaderProducerInterceptor.class})
public class CommonKafkaProducerAutoConfiguration {

    /**
     * DefaultKafkaProducerFactory 초기화 후 설정 맵을 갱신하여
     * interceptor.classes 에 인터셉터를 추가한다.
     */
    @Bean
    public BeanPostProcessor kafkaProducerFactoryInterceptorInjector() {
        final String interceptorClass = MdcToHeaderProducerInterceptor.class.getName();

        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DefaultKafkaProducerFactory<?, ?> factory) {
                    try {
                        Map<String, Object> current = factory.getConfigurationProperties();
                        Map<String, Object> updated = new HashMap<>(current);

                        Object existing = updated.get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
                        String val = (existing == null ? "" : String.valueOf(existing).trim());

                        final String newVal;

                        if (!StringUtils.hasText(val)) {
                            // 값이 없으면 신규 세팅
                            newVal = interceptorClass;
                        } else if (!val.contains(interceptorClass)) {
                            // 값은 있지만 인터셉터가 없으면 append(중복 방지)
                            newVal = val + "," + interceptorClass;
                        } else {
                            // 이미 포함돼 있으면 변경 없음
                            newVal = val;
                        }

                        // 실제 변화가 있을 때만 갱신
                        if (!newVal.equals(val)) {
                            updated.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, newVal);
                            factory.updateConfigs(updated);
                        }
                    } catch (Throwable ignore) {
                        // 후처리 실패로 발행 자체가 막히면 안 되므로 안전하게 무시
                    }
                }
                return bean;
            }
        };
    }
}
