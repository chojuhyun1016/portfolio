package org.example.order.common.core.messaging.code;

/**
 * DlqType 공통 인터페이스
 * - 서비스별 DlqType Enum이 이 인터페이스를 구현해야 함.
 */
public interface DlqType {
    String getText();
}

