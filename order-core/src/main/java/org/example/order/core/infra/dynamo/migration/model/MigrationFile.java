package org.example.order.core.infra.dynamo.migration.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 마이그레이션 루트 모델
 * - tables: 테이블 정의 리스트
 */
@Getter
@Setter
public class MigrationFile {
    private List<TableDef> tables = new ArrayList<>();
}
