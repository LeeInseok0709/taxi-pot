package com.taxipot.global.util;

import java.math.BigDecimal;

/** 두 좌표 거리(m, 하버사인). 도착 확인 50m·목록 반경 검색에 사용 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000d;

    private GeoUtils() {
    }

    public static int distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (int) Math.round(EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    public static int distanceMeters(BigDecimal lat1, BigDecimal lng1, double lat2, double lng2) {
        return distanceMeters(lat1.doubleValue(), lng1.doubleValue(), lat2, lng2);
    }
}
