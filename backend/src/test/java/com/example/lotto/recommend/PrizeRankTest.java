package com.example.lotto.recommend;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrizeRankTest {

    private static final List<Integer> WINNING = List.of(10, 23, 29, 33, 37, 40);
    private static final int BONUS = 16;

    @Test
    void ranks() {
        assertThat(PrizeRank.of(List.of(10, 23, 29, 33, 37, 40), WINNING, BONUS)).isEqualTo(1);
        assertThat(PrizeRank.of(List.of(10, 16, 23, 29, 33, 37), WINNING, BONUS)).isEqualTo(2);
        assertThat(PrizeRank.of(List.of(1, 10, 23, 29, 33, 37), WINNING, BONUS)).isEqualTo(3);
        assertThat(PrizeRank.of(List.of(1, 2, 10, 23, 29, 33), WINNING, BONUS)).isEqualTo(4);
        assertThat(PrizeRank.of(List.of(1, 2, 3, 10, 23, 29), WINNING, BONUS)).isEqualTo(5);
        assertThat(PrizeRank.of(List.of(1, 2, 3, 4, 16, 29), WINNING, BONUS)).isEqualTo(0);
    }
}
