package org.example.order.common.support.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.example.order.common.core.messaging.code.DlqType;

import java.io.IOException;
import java.util.Objects;

/**
 * DlqTypeStringDeserializer
 * ------------------------------------------------------------------------
 * 목적
 * - JSON 문자열("ORDER_API"/"ORDER_LOCAL" 등)을 DlqType 인터페이스의 경량 구현체로 역직렬화.
 * - common 모듈 안에서 완결되므로 DDD 의존 역전(common -> core) 없음.
 * <p>
 * 동작
 * - 직렬화 시: enum(DlqOrderType 등)이면 Jackson이 문자열로 출력(기존 그대로).
 * - 역직렬화 시: 문자열을 SimpleDlqType 객체로 감싸 DlqType 인터페이스로 세팅.
 * <p>
 * 주의
 * - 서비스 내부에서 DlqType의 '구체 enum 상수 비교' 대신 getText() 값을 비교 권장.
 */
public class DlqTypeStringDeserializer extends JsonDeserializer<DlqType> {

    /**
     * DlqType 경량 구현체 (common 내부)
     */
    private static final class SimpleDlqType implements DlqType {

        private final String text;

        private SimpleDlqType(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)  {
                return true;
            }

            if (!(o instanceof SimpleDlqType other)) {
                return false;
            }

            return Objects.equals(text, other.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text);
        }
    }

    @Override
    public DlqType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();

        if (text == null || text.isBlank()) {
            return null;
        }

        return new SimpleDlqType(text);
    }
}
