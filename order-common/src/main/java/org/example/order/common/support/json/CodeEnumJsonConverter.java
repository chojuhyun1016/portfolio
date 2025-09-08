package org.example.order.common.support.json;

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
    public static class Deserializer<E extends Enum<E>>
            extends JsonDeserializer<E> implements ContextualDeserializer {

        private Class<E> target;

        public Deserializer(Class<E> target) {
            this.target = target;
        }

        @Override
        public E deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                return Enum.valueOf(target, p.getValueAsString());
            } else {
                final JsonNode node = p.getCodec().readTree(p);
                final JsonNode valueNode = node.get("code");

                if (valueNode == null) {
                    return null;
                }

                for (E constant : target.getEnumConstants()) {
                    if (constant.toString().equals(valueNode.textValue())) {
                        return constant;
                    }
                }
            }

            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            JavaType t = ctxt.getContextualType();
            Class<?> raw = (t != null ? t.getRawClass() : null);

            if (raw != null && Enum.class.isAssignableFrom(raw)) {
                Class<? extends Enum> narrowed = raw.asSubclass(Enum.class);
                Class<E> enumClass = (Class<E>) narrowed;

                return new Deserializer<>(enumClass);
            }

            throw new IllegalStateException("Expected enum type but got: " + raw);
        }
    }
}
