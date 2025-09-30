package org.example.order.core.infra.dynamo.migration.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 테이블 정의 모델
 * ------------------------------------------------------------------------
 * - DynamoDB는 "키 스키마"만 강제. 비키 속성은 스키마 없음(문서화/타입추론용으로 attributes 유지)
 * - 비교/변경 대상은 키 스키마(HASH/RANGE), GSI/LSI 정의만 취급한다.
 * - GSI: 이름, 키(해시/레인지), Projection(ALL|KEYS_ONLY|INCLUDE), nonKeyAttributes
 * - LSI: 테이블과 동일 HASH + 별도 RANGE, Projection
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
        private String name; // 속성명(문서화/타입 매핑용)
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
        private Integer readCapacity = 5;          // 테스트 기본값
        private Integer writeCapacity = 5;         // 테스트 기본값
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
