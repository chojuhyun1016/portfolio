package org.example.order.common.support.logging;

/**
 * CorrelateKeyResolver
 * ------------------------------------------------------------------------
 * 목적
 * - @Correlate 가 사용할 도메인 키 추출 전략 인터페이스.
 * - 각 서비스/모듈은 자유롭게 구현하여 빈으로 등록해 사용(resolverBean 속성으로 지정).
 * <p>
 * 예)
 *
 * @Bean("orderKeyResolver") CorrelateKeyResolver orderKeyResolver() {
 * return args -> PathValueExtractor.extract(args, "key", "value.id", "headers.orderId");
 * }
 */
@FunctionalInterface
public interface CorrelateKeyResolver {
    /**
     * @param args 메서드 인자들
     * @return 추출한 키(없으면 null 반환)
     */
    String resolve(Object[] args);
}
