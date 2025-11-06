package org.example.order.batch.service.retry;

import java.util.Map;

/**
 * OrderDeadLetterService
 * ------------------------------------------------------------------------
 * 목적
 * - DLQ 메시지 타입별 재처리 진입점.
 * 설계
 * - 라우팅/분류는 Facade에서 수행하고, Service는 타입별 메서드만 제공한다.
 * - 불필요한 제네릭 진입점(retry(Object, Map))은 제공하지 않는다.
 */
public interface OrderDeadLetterService {

    void retryLocal(Object rawMessage, Map<String, String> headers);

    void retryApi(Object rawMessage, Map<String, String> headers);

    void retryCrud(Object rawMessage, Map<String, String> headers);
}
