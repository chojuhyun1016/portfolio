package org.example.order.common.infra.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.core.exception.code.CommonExceptionCode;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ObjectMapper 관련 JSON 직렬화/역직렬화 유틸리티 클래스.
 * - JSON String ↔ Object 변환
 * - JSON 필드 추출
 * - List, Map 변환 등 지원
 */
@Slf4j
@UtilityClass
public class ObjectMapperUtils {

    private static final ObjectMapper objectMapper = ObjectMapperFactory.defaultObjectMapper();

    /**
     * 특정 JSON 필드 값 추출 (String → Class 변환)
     */
    public static <T> T getFieldValueFromString(String json, String fieldName, Class<T> clz) {
        try {
            JsonNode fieldNode = objectMapper.readTree(json).get(fieldName);
            return objectMapper.treeToValue(fieldNode, clz);
        } catch (Exception e) {
            String msg = String.format("Failed to extract field [%s] from JSON", fieldName);
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR, msg);
        }
    }

    /**
     * 특정 JSON 필드 값 추출 (Object → Class 변환)
     */
    public static <T> T getFieldValueFromObject(Object data, String fieldName, Class<T> clz) {
        return getFieldValueFromString(writeValueAsString(data), fieldName, clz);
    }

    /**
     * 객체를 JSON String으로 변환
     */
    public static String writeValueAsString(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            String msg = "Failed to serialize object to JSON string";
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR, msg);
        }
    }

    /**
     * JSON String을 Class로 변환
     */
    public static <T> T readValue(String data, Class<T> clz) {
        try {
            return objectMapper.readValue(data, clz);
        } catch (Exception e) {
            String msg = String.format("Failed to deserialize JSON to [%s]", clz.getSimpleName());
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR, msg);
        }
    }

    /**
     * JSON String을 제네릭 TypeReference로 변환
     */
    public static <T> T readValue(String data, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(data, typeRef);
        } catch (Exception e) {
            String msg = "Failed to deserialize JSON to TypeReference";
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR, msg);
        }
    }

    /**
     * Map으로 변환 (Object → Map)
     */
    public static Map<String, Object> valueToMap(Object data) {
        return convertValue(data, new TypeReference<>() {});
    }

    /**
     * JSON 배열(문자열)을 List로 변환
     */
    public static <T> List<T> convertToList(String data, Class<T> clz) {
        try {
            return objectMapper.readValue(data,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clz));
        } catch (Exception e) {
            String msg = String.format("Failed to convert JSON to List<%s>", clz.getSimpleName());
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR, msg);
        }
    }

    /**
     * 객체를 다른 타입으로 변환 (ex: Map → DTO)
     */
    public static <T> T valueToObject(Object data, Class<T> clz) {
        try {
            return objectMapper.convertValue(data, clz);
        } catch (Exception e) {
            String msg = String.format("Failed to convert object to [%s]", clz.getSimpleName());
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR, msg);
        }
    }

    /**
     * 객체를 TypeReference 기반으로 변환 (Map → List 등)
     */
    public static <T> T convertValue(Object data, TypeReference<T> typeRef) {
        try {
            return objectMapper.convertValue(data, typeRef);
        } catch (Exception e) {
            String msg = "Failed to convert value using TypeReference";
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR, msg);
        }
    }

    /**
     * 객체를 JSON Tree로 변환 후 Class로 변환
     */
    public static <T> T convertTreeToValue(Object data, Class<T> clz) {
        try {
            JsonNode jsonNode = objectMapper.valueToTree(data);

            return objectMapper.treeToValue(jsonNode, clz);
        } catch (Exception e) {
            String msg = String.format("Failed to convert tree to [%s]", clz.getSimpleName());
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR, msg);
        }
    }

    /**
     * JSON 배열(Object[])을 List로 변환
     */
    public static <T> List<T> convertTreeToValues(Object[] data, Class<T> clz) {
        List<T> result = new ArrayList<>();
        try {
            for (Object obj : data) {
                result.add(convertTreeToValue(obj, clz));
            }

            return result;
        } catch (Exception e) {
            String msg = String.format("Failed to convert Object[] to List<%s>", clz.getSimpleName());
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR, msg);
        }
    }

    /**
     * JSON 직렬화 후 파일 출력
     */
    public static void writeValue(FileOutputStream outputStream, Object object) {
        try {
            objectMapper.writeValue(outputStream, object);
        } catch (Exception e) {
            String msg = "Failed to write JSON to output stream";
            log.error(msg, e);
            throw new CommonException(CommonExceptionCode.DATA_WRITE_ERROR, msg);
        }
    }
}
