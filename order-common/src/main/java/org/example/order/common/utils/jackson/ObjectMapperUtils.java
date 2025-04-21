package org.example.order.common.utils.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.exception.code.CommonExceptionCode;
import org.example.order.common.exception.CommonException;
import org.example.order.common.jackson.config.CommonObjectMapperFactory;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@UtilityClass
public class ObjectMapperUtils {
    private static final ObjectMapper objectMapper = CommonObjectMapperFactory.defaultObjectMapper();

    public <T> T getFieldValueFromString(String json, String fieldName, Class<T> clz) {
        try {
            return valueToObject(objectMapper.readTree(json).get(fieldName), clz);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        }
    }

    public <T> T getFieldValueFromObject(Object data, String fieldName, Class<T> clz) {
        try {
            return getFieldValueFromString(writeValueAsString(data), fieldName, clz);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        }
    }

    public String writeValueAsString(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        }
    }

    public static Map<String, Object> valueToMap(Object data) {
        try {
            return objectMapper.readValue(data.toString(), new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        }
    }

    public static <T> T readValue(String data, Class<T> clz) {
        try {
            return objectMapper.readValue(data, clz);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        }
    }

    public static void writeValue(FileOutputStream outputStream, Object object) {
        try {
            objectMapper.writeValue(outputStream, object);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_WRITE_ERROR);
        }
    }

    public static <T> T valueToObject(Object data, Class<T> clz) {
        try {
            return objectMapper.convertValue(valueToMap(data), clz);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        }
    }

    public static <T> T convertTreeToValue(Object data, Class<T> clz) {
        try {
            JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(data));
            return objectMapper.treeToValue(jsonNode, clz);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        }
    }

    public static <T> List<T> convertTreeToValues(Object[] data, Class<T> clz) {
        try {
            List<T> result = new ArrayList<>();
            for (Object obj : data) {
                JsonNode jsonNode = objectMapper.readTree(writeValueAsString(obj));
                result.add(objectMapper.treeToValue(jsonNode, clz));
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        }
    }

    public static <T> List<T> convertToList(String data, Class<T> clz) {
        try {
            return objectMapper.readValue(data, objectMapper.getTypeFactory().constructCollectionType(List.class, clz));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        }
    }
}
