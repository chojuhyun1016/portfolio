package org.example.order.common.core.code.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum CurrencyType implements CodeEnum {

    USD ("USD", List.of(RegionCode.US)),
    TWD ("TWD", List.of(RegionCode.TW)),
    CAD ("CAD", List.of(RegionCode.CA)),
    HKD ("HKD", List.of(RegionCode.HK)),
    KRW ("KRW", List.of(RegionCode.KR)),
    ;

    private final String text;
    private final List<RegionCode> regions;

    public static CurrencyType of(String regionCodeString) {
        if (Objects.isNull(regionCodeString))
            return null;

        return Arrays.stream(CurrencyType.values())
                .filter(p -> p.getRegions().contains(RegionCode.of(regionCodeString)))
                .findFirst()
                .orElse(null);
    }

    public static CurrencyType of(RegionCode regionCode) {
        if (Objects.isNull(regionCode))
            return null;

        return Arrays.stream(CurrencyType.values())
                .filter(p -> p.getRegions().contains(regionCode))
                .findFirst()
                .orElse(null);
    }
}
