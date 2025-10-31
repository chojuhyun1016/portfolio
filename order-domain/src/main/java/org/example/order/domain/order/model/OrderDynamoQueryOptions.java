package org.example.order.domain.order.model;

/**
 * DynamoDB 조회 옵션 (운영 가드 표준화)
 * - limit: 결과 상한 (null 이면 구현 기본값)
 * - consistentRead: getItem 강한 일관성 여부 (GSI 쿼리에는 적용되지 않음)
 * - allowScanFallback: GSI 실패/미구성 시 scan 허용 여부
 * - startKey: 페이지네이션 시작키 (확장 포인트; 현재 구현에서는 null 무시)
 */
public final class OrderDynamoQueryOptions {

    private final Integer limit;
    private final Boolean consistentRead;
    private final Boolean allowScanFallback;
    private final String startKey;

    private OrderDynamoQueryOptions(Builder b) {
        this.limit = b.limit;
        this.consistentRead = b.consistentRead;
        this.allowScanFallback = b.allowScanFallback;
        this.startKey = b.startKey;
    }

    public Integer getLimit() {
        return limit;
    }

    public Boolean getConsistentRead() {
        return consistentRead;
    }

    public Boolean getAllowScanFallback() {
        return allowScanFallback;
    }

    public String getStartKey() {
        return startKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Integer limit;
        private Boolean consistentRead;
        private Boolean allowScanFallback;
        private String startKey;

        public Builder limit(Integer limit) {
            this.limit = limit;

            return this;
        }

        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;

            return this;
        }

        public Builder allowScanFallback(Boolean allowScanFallback) {
            this.allowScanFallback = allowScanFallback;

            return this;
        }

        public Builder startKey(String startKey) {
            this.startKey = startKey;

            return this;
        }

        public OrderDynamoQueryOptions build() {
            return new OrderDynamoQueryOptions(this);
        }
    }
}
