package org.example.order.core.infra.common.nosql;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import lombok.experimental.UtilityClass;

import java.util.*;

/**
 * 공통적으로 자주 사용되는 DynamoDB 쿼리 유틸 메서드 모음.
 * 상태가 없으며 static으로만 구성되어야 함.
 */
@UtilityClass
public class DynamoQuerySupport {

    /**
     * 지정된 속성과 값으로 스캔하는 메서드 (숫자 타입)
     */
    public static <T> List<T> scanByNumber(DynamoDBMapper mapper, Class<T> clazz, String attributeName, Number value) {
        Map<String, AttributeValue> eav = Map.of(
                ":val", new AttributeValue().withN(String.valueOf(value))
        );

        DynamoDBScanExpression scanExp = new DynamoDBScanExpression()
                .withFilterExpression(attributeName + " = :val")
                .withExpressionAttributeValues(eav);

        return mapper.scan(clazz, scanExp);
    }

    /**
     * 문자열 속성으로 스캔하는 메서드
     */
    public static <T> List<T> scanByString(DynamoDBMapper mapper, Class<T> clazz, String attributeName, String value) {
        Map<String, AttributeValue> eav = Map.of(
                ":val", new AttributeValue().withS(value)
        );

        DynamoDBScanExpression scanExp = new DynamoDBScanExpression()
                .withFilterExpression(attributeName + " = :val")
                .withExpressionAttributeValues(eav);

        return mapper.scan(clazz, scanExp);
    }

    /**
     * 숫자 범위로 스캔하는 메서드
     */
    public static <T> List<T> scanByNumberRange(DynamoDBMapper mapper, Class<T> clazz, String attributeName, long from, long to) {
        Map<String, AttributeValue> eav = Map.of(
                ":from", new AttributeValue().withN(String.valueOf(from)),
                ":to", new AttributeValue().withN(String.valueOf(to))
        );

        DynamoDBScanExpression scanExp = new DynamoDBScanExpression()
                .withFilterExpression(attributeName + " BETWEEN :from AND :to")
                .withExpressionAttributeValues(eav);

        return mapper.scan(clazz, scanExp);
    }

    /**
     * 특정 속성이 존재하는 항목만 조회 (null 체크)
     */
    public static <T> List<T> scanWithExists(DynamoDBMapper mapper, Class<T> clazz, String attributeName) {
        DynamoDBScanExpression scanExp = new DynamoDBScanExpression()
                .withFilterExpression("attribute_exists(" + attributeName + ")");

        return mapper.scan(clazz, scanExp);
    }

    /**
     * 속성이 없는 항목만 조회
     */
    public static <T> List<T> scanWithNotExists(DynamoDBMapper mapper, Class<T> clazz, String attributeName) {
        DynamoDBScanExpression scanExp = new DynamoDBScanExpression()
                .withFilterExpression("attribute_not_exists(" + attributeName + ")");

        return mapper.scan(clazz, scanExp);
    }

    /**
     * 속성이 포함된 값(contains)으로 스캔
     */
    public static <T> List<T> scanWithContains(DynamoDBMapper mapper, Class<T> clazz, String attributeName, String keyword) {
        Map<String, AttributeValue> eav = Map.of(
                ":val", new AttributeValue().withS(keyword)
        );

        DynamoDBScanExpression scanExp = new DynamoDBScanExpression()
                .withFilterExpression("contains(" + attributeName + ", :val)")
                .withExpressionAttributeValues(eav);

        return mapper.scan(clazz, scanExp);
    }

    /**
     * 여러 조건을 AND로 조합한 필터 스캔 (단순 버전)
     */
    public static <T> List<T> scanWithAndConditions(DynamoDBMapper mapper, Class<T> clazz, Map<String, String> stringConditions) {
        StringBuilder expression = new StringBuilder();
        Map<String, AttributeValue> eav = new HashMap<>();

        int i = 0;
        for (var entry : stringConditions.entrySet()) {
            String placeholder = ":val" + i;
            if (i > 0) expression.append(" AND ");
            expression.append(entry.getKey()).append(" = ").append(placeholder);
            eav.put(placeholder, new AttributeValue().withS(entry.getValue()));
            i++;
        }

        DynamoDBScanExpression scanExp = new DynamoDBScanExpression()
                .withFilterExpression(expression.toString())
                .withExpressionAttributeValues(eav);

        return mapper.scan(clazz, scanExp);
    }
}
