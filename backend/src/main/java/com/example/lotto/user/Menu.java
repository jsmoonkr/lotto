package com.example.lotto.user;

/** 권한을 따로 주는 메뉴 단위. */
public enum Menu {
    RECOMMEND("추천"),
    HISTORY("추천 기록"),
    DRAWS("당첨번호"),
    STATS("통계");

    private final String label;

    Menu(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public String authority(PermissionLevel level) {
        return "MENU_" + name() + "_" + level.name();
    }
}
