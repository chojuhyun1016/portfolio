package org.example.order.core.support.mapping.config;

import org.example.order.common.helper.datetime.DateTimeUtils;
import org.example.order.core.support.mapping.TimeMapper;
import org.example.order.core.support.mapping.TimeProvider;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

/**
 * 애플리케이션 전역 MapStruct 공통 설정
 * - componentModel: spring
 * - unmappedTargetPolicy: 누락 필드는 컴파일 에러로 강제
 * - injectionStrategy: 생성자 주입 선호
 * - imports: expression에서 사용하는 공통 유틸/타입
 */
@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        imports = {DateTimeUtils.class, LocalDateTime.class},
        uses = {TimeMapper.class, TimeProvider.class}
)
public interface AppMappingConfig {
}
