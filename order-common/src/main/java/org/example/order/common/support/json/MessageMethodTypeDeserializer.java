package org.example.order.common.support.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;

import org.example.order.common.core.messaging.code.MessageMethodType;

/**
 * MessageMethodTypeDeserializer
 * ------------------------------------------------------------------------
 * 목적
 * - "PUT" 같은 문자열과 {"text":"PUT","code":"PUT"} 같은 객체, 두 가지 JSON 형태를 모두 수용.
 * - 모듈(레이어) 오염 없이 common.support.json 안에 존재하며, core의 enum 에서 @JsonDeserialize 로 참조.
 */
public final class MessageMethodTypeDeserializer extends JsonDeserializer<MessageMethodType> {

    @Override
    public MessageMethodType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();

        if (t == JsonToken.VALUE_STRING) {
            String v = p.getValueAsString();

            return fromString(v, ctxt);
        }

        if (t == JsonToken.START_OBJECT) {
            JsonNode node = p.readValueAsTree();
            String code = node.hasNonNull("code") ? node.get("code").asText() : null;
            String text = node.hasNonNull("text") ? node.get("text").asText() : null;

            if (code != null && !code.isBlank()) {
                return fromString(code, ctxt);
            }

            if (text != null && !text.isBlank()) {
                return fromText(text, ctxt);
            }
        }

        throw JsonMappingException.from(ctxt,
                "Cannot deserialize MessageMethodType from token: " + t);
    }

    private MessageMethodType fromString(String v, DeserializationContext ctxt) throws JsonMappingException {
        if (v == null || v.isBlank()) {
            return null;
        }

        try {
            return MessageMethodType.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return fromText(v, ctxt);
        }
    }

    private MessageMethodType fromText(String text, DeserializationContext ctxt) throws JsonMappingException {
        for (MessageMethodType e : MessageMethodType.values()) {
            if (text.equals(e.getText())) {
                return e;
            }
        }

        throw JsonMappingException.from(ctxt, "Unknown MessageMethodType text: " + text);
    }
}
