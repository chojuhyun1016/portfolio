package org.example.order.common.code.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum ContinentCode implements CodeEnum {

    GB("글로벌", List.of(RegionCode.NN)),
    KR("한국", List.of(RegionCode.KR)),
    AS("아시아", List.of(RegionCode.HK, RegionCode.TW, RegionCode.SG, RegionCode.MY)),
    NA("북 아메리카", List.of(RegionCode.US, RegionCode.CA, RegionCode.MX));

    private final String text;
    private final List<RegionCode> listRegionCode;

    public static ContinentCode of(String regionString) {
        if (Objects.isNull(regionString))
            return null;

        return Arrays.stream(ContinentCode.values())
                .filter(continent -> continent.getListRegionCode().contains(RegionCode.of(regionString)))
                .findFirst()
                .orElse(null);
    }

    public static ContinentCode of(RegionCode regionCode) {
        if (Objects.isNull(regionCode))
            return null;

        return Arrays.stream(ContinentCode.values())
                .filter(continent -> continent.getListRegionCode().contains(regionCode))
                .findFirst()
                .orElse(null);
    }
}
