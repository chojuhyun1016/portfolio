package org.example.order.core.application.order.mapper;

import org.example.order.cache.feature.order.model.OrderCacheRecord;
import org.example.order.core.application.order.dto.view.OrderView;
import org.example.order.core.support.mapping.config.AppMappingConfig;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * OrderCacheViewMapper
 * - Cache 전용 DTO(OrderCacheRecord) -> OrderView 매핑
 * - 캐시에는 최소 필드만 있으므로, 없는 필드는 null 고정
 */
@Mapper(
        config = AppMappingConfig.class,
        builder = @Builder(disableBuilder = true)
)
public interface OrderCacheViewMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "userNumber", source = "userNumber")
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "orderNumber", source = "orderNumber")
    @Mapping(target = "orderPrice", source = "orderPrice")
    @Mapping(target = "deleteYn", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdUserId", ignore = true)
    @Mapping(target = "createdUserType", ignore = true)
    @Mapping(target = "createdDatetime", ignore = true)
    @Mapping(target = "modifiedUserId", ignore = true)
    @Mapping(target = "modifiedUserType", ignore = true)
    @Mapping(target = "modifiedDatetime", ignore = true)
    @Mapping(target = "publishedTimestamp", ignore = true)
    @Mapping(target = "failure", constant = "false")
    OrderView toView(OrderCacheRecord cache);
}
