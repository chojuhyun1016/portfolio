package org.example.order.core.infra.dynamo.migration.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 시드 파일 모델
 * - table: 대상 테이블명
 * - items: putItem 할 문서 리스트 (Map<String,Object>)
 */
@Getter
@Setter
public class SeedFile {
    private String table;
    private List<Map<String, Object>> items;
}
