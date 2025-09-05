package org.example.order.domain.order.model;

/**
 * JDBC 배치 옵션
 * - batchChunkSize: 배치 청크 크기(기본 구현값 대체)
 * - sqlHint: 드라이버/DB 힌트 문자열 (선택, 구현에서 미사용 가능)
 */
public final class OrderBatchOptions {
    private final Integer batchChunkSize;
    private final String sqlHint;

    private OrderBatchOptions(Builder b) {
        this.batchChunkSize = b.batchChunkSize;
        this.sqlHint = b.sqlHint;
    }

    public Integer getBatchChunkSize() {
        return batchChunkSize;
    }

    public String getSqlHint() {
        return sqlHint;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Integer batchChunkSize;
        private String sqlHint;

        public Builder batchChunkSize(Integer v) {
            this.batchChunkSize = v;
            return this;
        }

        public Builder sqlHint(String v) {
            this.sqlHint = v;
            return this;
        }

        public OrderBatchOptions build() {
            return new OrderBatchOptions(this);
        }
    }
}
