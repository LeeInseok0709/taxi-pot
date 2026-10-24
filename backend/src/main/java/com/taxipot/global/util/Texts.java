package com.taxipot.global.util;

import java.util.Locale;

public final class Texts {

    /** 탈퇴 회원 표시용 예약 접두사 (DB CHECK chk_users_nickname_reserved) */
    public static final String RESERVED_NICKNAME_PREFIX = "탈퇴회원_";

    private Texts() {
    }

    /** '010-1234-5678' → '01012345678' (휴대폰·계좌번호 저장 전 정규화) */
    public static String digitsOnly(String value) {
        return value == null ? null : value.replaceAll("[^0-9]", "");
    }

    public static boolean isReservedNickname(String nickname) {
        return nickname != null && nickname.startsWith(RESERVED_NICKNAME_PREFIX);
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** 이메일 '@' 뒤 도메인 (소문자) */
    public static String emailDomain(String email) {
        String e = normalizeEmail(email);
        int at = e == null ? -1 : e.lastIndexOf('@');
        return at < 0 ? "" : e.substring(at + 1);
    }

    /** 응답 마스킹: 123456789012 → 1234****9012 */
    public static String maskAccount(String digits) {
        if (digits == null || digits.length() <= 8) {
            return digits;
        }
        return digits.substring(0, 4) + "*".repeat(digits.length() - 8) + digits.substring(digits.length() - 4);
    }
}
