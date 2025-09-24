package org.example.order.core.infra.dynamo.migration.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 테이블 정의 모델
 * ------------------------------------------------------------------------
 * - DynamoDB는 "키 스키마"만 강제. 비키 속성은 스키마 없음(문서화/타입추론용으로 attributes 유지)
 * - GSI/LSI를 JSON으로 선언하면 생성 시 반영
 * * GSI: 독립 RCU/WCU, Projection(ALL|KEYS_ONLY|INCLUDE), nonKeyAttributes(선택)
 * * LSI: 테이블과 동일한 HASH 키 + 별도 RANGE 키, Projection만 지정
 */
@Getter
@Setter
public class TableDef {
    private String name;
    private String hashKey;
    private String rangeKey;                       // 선택 (LSI 쓰려면 필수)
    private int readCapacity = 5;
    private int writeCapacity = 5;

    private List<AttributeDef> attributes = new ArrayList<>();

    private List<GsiDef> globalSecondaryIndexes = new ArrayList<>();
    private List<LsiDef> localSecondaryIndexes = new ArrayList<>();

    @Getter
    @Setter
    public static class AttributeDef {
        private String name; // 속성명
        private String type; // "S" | "N" | "B"
    }

    @Getter
    @Setter
    public static class GsiDef {
        private String name;
        private String hashKey;
        private String rangeKey;                   // 선택
        private String projection = "ALL";         // ALL|KEYS_ONLY|INCLUDE
        private List<String> nonKeyAttributes;     // projection=INCLUDE일 때만 의미
        private Integer readCapacity = 5;          // null 허용 → 기본 5
        private Integer writeCapacity = 5;         // null 허용 → 기본 5
    }

    @Getter
    @Setter
    public static class LsiDef {
        private String name;
        private String rangeKey;                   // 필수 (LSI는 테이블 HASH 키 + 이 RANGE 키)
        private String projection = "ALL";         // ALL|KEYS_ONLY|INCLUDE
        private List<String> nonKeyAttributes;     // projection=INCLUDE일 때만 의미
    }
}