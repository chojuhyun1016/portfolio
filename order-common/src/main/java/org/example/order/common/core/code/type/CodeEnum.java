package org.example.order.common.core.code.type;

/**
 * 코드 Enum 인터페이스 표준화
 *
 * - 모든 코드형 Enum은 이 인터페이스를 구현하여 일관된 getText(), getCode()를 제공해야 함
 */
public interface CodeEnum {
    /**
     * Enum의 사용자 표시용 텍스트 반환
     */
    String getText();

    /**
     * Enum의 코드(기본: Enum 상수명)를 반환
     */
    default String getCode() {
        return toString();
    }
}
