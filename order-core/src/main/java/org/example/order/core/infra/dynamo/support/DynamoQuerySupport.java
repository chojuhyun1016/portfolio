package org.example.order.core.infra.dynamo.support;

import lombok.experimental.UtilityClass;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DynamoDB Enhanced Client 기반 공용 스캔 유틸리티.
 * v1 DynamoDBMapper 사용 코드를 v2 스타일로 대체.
 * <p>
 * 모든 메서드는 무상태(static)이며, 호출 시 EnhancedClient/테이블명을 인자로 받습니다.
 */
@UtilityClass
public class DynamoQuerySupport {

    private static <T> DynamoDbTable<T> table(DynamoDbEnhancedClient client, Class<T> clazz, String tableName) {
        return client.table(tableName, TableSchema.fromBean(clazz));
    }

    private static List<?> scan(DynamoDbTable<?> table, ScanEnhancedRequest request) {
        PageIterable<?> pages = table.scan(request);
        return pages.items().stream().collect(Collectors.toList());
    }

    private static Expression eqExpr(String attr, AttributeValue v) {
        return Expression.builder()
                .expression("#a = :v")
                .expressionNames(Map.of("#a", attr))
                .expressionValues(Map.of(":v", v))
                .build();
    }

    /**
     * 지정된 속성과 값으로 스캔 (숫자 타입)
     */
    public static <T> List<T> scanByNumber(DynamoDbEnhancedClient client, Class<T> clazz, String tableName,
                                           String attributeName, Number value) {
        var expr = eqExpr(attributeName, AttributeValue.builder().n(String.valueOf(value)).build());
        var req = ScanEnhancedRequest.builder().filterExpression(expr).build();

        @SuppressWarnings("unchecked")
        List<T> items = (List<T>) scan(table(client, clazz, tableName), req);
        return items;
    }

    /**
     * 문자열 속성으로 스캔
     */
    public static <T> List<T> scanByString(DynamoDbEnhancedClient client, Class<T> clazz, String tableName,
                                           String attributeName, String value) {
        var expr = eqExpr(attributeName, AttributeValue.builder().s(value).build());
        var req = ScanEnhancedRequest.builder().filterExpression(expr).build();

        @SuppressWarnings("unchecked")
        List<T> items = (List<T>) scan(table(client, clazz, tableName), req);
        return items;
    }

    /**
     * 숫자 범위(BETWEEN)로 스캔
     */
    public static <T> List<T> scanByNumberRange(DynamoDbEnhancedClient client, Class<T> clazz, String tableName,
                                                String attributeName, long from, long to) {
        var expr = Expression.builder()
                .expression("#a BETWEEN :from AND :to")
                .expressionNames(Map.of("#a", attributeName))
                .expressionValues(Map.of(
                        ":from", AttributeValue.builder().n(String.valueOf(from)).build(),
                        ":to", AttributeValue.builder().n(String.valueOf(to)).build()
                ))
                .build();

        var req = ScanEnhancedRequest.builder().filterExpression(expr).build();

        @SuppressWarnings("unchecked")
        List<T> items = (List<T>) scan(table(client, clazz, tableName), req);

        return items;
    }

    /**
     * 특정 속성이 존재하는 항목만 조회 (attribute_exists)
     */
    public static <T> List<T> scanWithExists(DynamoDbEnhancedClient client, Class<T> clazz, String tableName,
                                             String attributeName) {
        var expr = Expression.builder()
                .expression("attribute_exists(#a)")
                .expressionNames(Map.of("#a", attributeName))
                .build();

        var req = ScanEnhancedRequest.builder().filterExpression(expr).build();

        @SuppressWarnings("unchecked")
        List<T> items = (List<T>) scan(table(client, clazz, tableName), req);

        return items;
    }

    /**
     * 속성이 없는 항목만 조회 (attribute_not_exists)
     */
    public static <T> List<T> scanWithNotExists(DynamoDbEnhancedClient client, Class<T> clazz, String tableName,
                                                String attributeName) {
        var expr = Expression.builder()
                .expression("attribute_not_exists(#a)")
                .expressionNames(Map.of("#a", attributeName))
                .build();

        var req = ScanEnhancedRequest.builder().filterExpression(expr).build();

        @SuppressWarnings("unchecked")
        List<T> items = (List<T>) scan(table(client, clazz, tableName), req);

        return items;
    }

    /**
     * 속성이 포함된 값(contains)으로 스캔
     */
    public static <T> List<T> scanWithContains(DynamoDbEnhancedClient client, Class<T> clazz, String tableName,
                                               String attributeName, String keyword) {
        var expr = Expression.builder()
                .expression("contains(#a, :v)")
                .expressionNames(Map.of("#a", attributeName))
                .expressionValues(Map.of(":v", AttributeValue.builder().s(keyword).build()))
                .build();

        var req = ScanEnhancedRequest.builder().filterExpression(expr).build();

        @SuppressWarnings("unchecked")
        List<T> items = (List<T>) scan(table(client, clazz, tableName), req);

        return items;
    }

    /**
     * 여러 문자열 equals 조건을 AND 로 조합한 스캔 (단순 버전)
     * ex) {"status"="OK","type"="A"} -> "#k0 = :v0 AND #k1 = :v1"
     */
    public static <T> List<T> scanWithAndConditions(DynamoDbEnhancedClient client, Class<T> clazz, String tableName,
                                                    Map<String, String> stringConditions) {
        if (stringConditions == null || stringConditions.isEmpty()) {
            // 조건 없으면 전체 스캔
            @SuppressWarnings("unchecked")
            List<T> items = (List<T>) scan(table(client, clazz, tableName),
                    ScanEnhancedRequest.builder().build());

            return items;
        }

        Map<String, String> names = new LinkedHashMap<>();
        Map<String, AttributeValue> values = new LinkedHashMap<>();
        StringBuilder expr = new StringBuilder();

        int i = 0;

        for (var e : stringConditions.entrySet()) {
            String nk = "#k" + i;
            String vk = ":v" + i;

            if (i > 0) {
                expr.append(" AND ");
            }

            expr.append(nk).append(" = ").append(vk);
            names.put(nk, e.getKey());
            values.put(vk, AttributeValue.builder().s(e.getValue()).build());
            i++;
        }

        var expression = Expression.builder()
                .expression(expr.toString())
                .expressionNames(names)
                .expressionValues(values)
                .build();

        var req = ScanEnhancedRequest.builder().filterExpression(expression).build();
        @SuppressWarnings("unchecked")
        List<T> items = (List<T>) scan(table(client, clazz, tableName), req);

        return items;
    }
}
