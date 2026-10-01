package com.example.lotto.user;

import java.util.Locale;

/** 개인정보 입력값 정규화와 화면 표시용 마스킹. */
public final class PersonalData {

    private PersonalData() {
    }

    public static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** 숫자만 남긴다. 비어 있으면 null. */
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    public static String formatPhone(String digits) {
        if (digits == null) {
            return null;
        }
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return digits;
    }

    /** hong@example.com → ho**@example.com */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String visible = local.substring(0, Math.min(2, local.length()));
        return visible + "*".repeat(Math.max(2, local.length() - visible.length())) + email.substring(at);
    }

    /** 01012345678 → 010-****-5678 */
    public static String maskPhone(String digits) {
        if (digits == null) {
            return null;
        }
        if (digits.length() < 8) {
            return "***";
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }
}
