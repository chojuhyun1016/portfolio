package org.example.order.common.infra.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import lombok.NoArgsConstructor;
import org.example.order.common.core.code.dto.CodeEnumDto;
import org.example.order.common.core.code.type.CodeEnum;
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
    public static class Deserializer extends JsonDeserializer<Enum<?>> implements ContextualDeserializer {

        private Class<? extends Enum<?>> target;

        public Deserializer(Class<? extends Enum<?>> target) {
            this.target = target;
        }

        @Override
        public Enum<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            if (jsonParser.currentToken() == JsonToken.VALUE_STRING) {
                return Enum.valueOf(target.asSubclass(Enum.class), jsonParser.getValueAsString());
            } else {
                final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
                final JsonNode valueNode = node.get("code");

                if (valueNode == null) return null;

                for (Enum<?> constant : target.getEnumConstants()) {
                    if (constant.toString().equals(valueNode.textValue())) {
                        return constant;
                    }
                }
            }
            return null;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            JavaType contextualType = ctxt.getContextualType();
            Class<?> rawClass = contextualType.getRawClass();
            if (Enum.class.isAssignableFrom(rawClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) rawClass;
                return new Deserializer(enumClass);
            } else {
                throw new IllegalStateException("Expected enum type but got: " + rawClass);
            }
        }
    }
}
