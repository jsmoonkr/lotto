package com.example.lotto.user;

/**
 * 메뉴별 권한 수준. ADMIN은 USE를 포함한다.
 * NONE은 권한이 없다는 뜻으로 API 입력에만 쓰고 DB에는 저장하지 않는다(행을 지운다).
 */
public enum PermissionLevel {
    NONE,
    /** 메뉴 사용 */
    USE,
    /** 메뉴 사용 + 다른 사용자에게 이 메뉴의 USE 권한 부여/회수 (+ 메뉴별 관리 기능) */
    ADMIN
}
