package com.example.lotto.user;

public enum UserStatus {
    ACTIVE,
    /** 관리자가 정지한 계정. 로그인과 모든 API 호출이 막힌다. */
    DISABLED
}
