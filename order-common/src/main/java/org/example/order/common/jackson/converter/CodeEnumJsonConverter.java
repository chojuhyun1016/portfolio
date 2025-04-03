package org.example.order.common.jackson.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import lombok.NoArgsConstructor;
import org.example.order.common.application.dto.CodeEnumDto;
import org.example.order.common.code.CodeEnum;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

@JsonComponent
public class CodeEnumJsonConverter {

    public static class Serializer extends JsonSerializer<CodeEnum> {
        @Override
        public void serialize(final CodeEnum value,
                              final JsonGenerator gen,
                              final SerializerProvider serializers) throws IOException {
            gen.writeObject(CodeEnumDto.toDto(value));
        }
    }

    @NoArgsConstructor
    public static class Deserializer extends JsonDeserializer<Enum> implements ContextualDeserializer {
        private Class<Enum> target;

        public Deserializer(Class<Enum> target) {
            this.target = target;
        }

        @Override
        public Enum<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            if (jsonParser.currentToken() == JsonToken.VALUE_STRING) {
                // 일반 enum
                return Enum.valueOf(target, jsonParser.getValueAsString());
            } else {
                // CodeEnum 상속 enum
                final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
                final JsonNode valueNode = node.get("code");

                if (valueNode == null) {
                    return null;
                }

                for (Enum<?> t : target.getEnumConstants()) {
                    if (t.toString().equals(valueNode.textValue())) {
                        return t;
                    }
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            return new Deserializer((Class<Enum>)ctxt.getContextualType().getRawClass());
        }
    }
}
