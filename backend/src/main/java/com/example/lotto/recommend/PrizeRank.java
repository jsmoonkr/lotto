package com.example.lotto.recommend;

import java.util.List;

public final class PrizeRank {

    private PrizeRank() {
    }

    /** 1~5등이면 등수, 낙첨이면 0. */
    public static int of(List<Integer> picked, List<Integer> winning, int bonus) {
        long matched = picked.stream().filter(winning::contains).count();
        return switch ((int) matched) {
            case 6 -> 1;
            case 5 -> picked.contains(bonus) ? 2 : 3;
            case 4 -> 4;
            case 3 -> 5;
            default -> 0;
        };
    }
}
