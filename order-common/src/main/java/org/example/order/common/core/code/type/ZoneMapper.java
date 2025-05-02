package org.example.order.common.core.code.type;

public class ZoneMapper {

    private ZoneMapper() {}

    public static ZoneCode zoneOf(RegionCode regionCode) {
        if (regionCode == null) {
            return ZoneCode.UTC;
        }

        return switch (regionCode) {
            case KR -> ZoneCode.KR;
            case US -> ZoneCode.US;
            case CA -> ZoneCode.CA;
            case TW -> ZoneCode.TW;
            case HK -> ZoneCode.HK;
            case MY -> ZoneCode.MY;
            case SG -> ZoneCode.SG;
            case MX -> ZoneCode.MX;
            default -> ZoneCode.US;
        };
    }
}
