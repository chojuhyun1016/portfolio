package org.example.order.common.code.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum RegionCode implements CodeEnum {

    NN ("NN"),
    KR ("KR"),
    US ("US"),
    CA ("CA"),
    HK ("HK"),
    TW ("TW"),
    MY ("MY"),
    SG ("SG"),
    MX ("MX"),
    ;

    private final String text;

    public String formatName(String firstName, String lastName) {
        if (ContinentCode.NA.equals(ContinentCode.of(this))) {
            return Objects.toString(lastName, "") + " " + Objects.toString(firstName, "");
        } else {
            return Objects.toString(firstName, "") + " " + Objects.toString(lastName, "");
        }
    }

    public static RegionCode of(String regionString) {
        if (Objects.isNull(regionString))
            return null;

        return Arrays.stream(RegionCode.values())
                .filter(status -> status.toString().equals(regionString))
                .findFirst()
                .orElse(null);
    }

    public static List<RegionCode> availableRegionCodes() {
        return Arrays.asList(RegionCode.KR, RegionCode.US, RegionCode.TW, RegionCode.HK, RegionCode.CA, RegionCode.SG, RegionCode.MY, RegionCode.MX);
    }
}
