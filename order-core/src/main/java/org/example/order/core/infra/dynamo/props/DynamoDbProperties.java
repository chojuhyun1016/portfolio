package org.example.order.core.infra.dynamo.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DynamoDB 프로퍼티
 * ------------------------------------------------------------------------
 * 기본:
 * - enabled           : 전역 스위치 (true일 때만 모듈 동작)
 * - endpoint/region   : LocalStack/DDB Local 등 연결 정보
 * - accessKey/secretKey: 필요 시 정적 크레덴셜
 * - autoCreate        : 로컬 + true → 테이블/인덱스 생성 + 시드 적용
 * - migrationLocation : 마이그레이션 JSON 위치 (V{n}__*.json)
 * - seedLocation      : 시드 JSON 위치 (V{n}__*.json)
 * - createMissingGsi  : 기존 테이블 존재 시 누락 GSI만 보강
 * <p>
 * 스키마 리컨실(로컬/Dev 한정):
 * - enabled          : 드리프트 감지/조정 켜기
 * - dryRun           : 위험 변경은 기본 DRY-RUN (로그만)
 * - allowDestructive : 파괴적 재생성 허용(데이터 의미 없거나 적을 때)
 * - deleteExtraGsi   : 마이그레이션에 없는 GSI 삭제
 * - copyData         : 재생성 시 임시 테이블로 데이터 복사
 * - maxItemCount     : copyData 허용 상한(초과 시 예외)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "dynamodb")
public class DynamoDbProperties {

    private boolean enabled = false;

    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;

    private String tableName;

    private Boolean autoCreate = false;
    private String migrationLocation = "classpath:dynamodb/migration";
    private String seedLocation = "classpath:dynamodb/seed";
    private Boolean createMissingGsi = true;

    /**
     * 로컬/Dev 전용 스키마 리컨실 옵션
     */
    private SchemaReconcile schemaReconcile = new SchemaReconcile();

    @Getter
    @Setter
    public static class SchemaReconcile {
        private Boolean enabled = true;           // 드리프트 감지/조정 사용
        private Boolean dryRun = true;            // 위험 변경 DRY-RUN
        private Boolean allowDestructive = false; // 파괴적 재생성 허용
        private Boolean deleteExtraGsi = false;   // 여분 GSI 삭제
        private Boolean copyData = false;         // 재생성 시 데이터 복사
        private Long maxItemCount = 10_000L;      // 복사 상한
    }
}
