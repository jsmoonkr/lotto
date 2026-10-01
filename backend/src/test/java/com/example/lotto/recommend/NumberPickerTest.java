package com.example.lotto.recommend;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class NumberPickerTest {

    private final NumberPicker picker = new NumberPicker(new Random());

    @RepeatedTest(200)
    void uniformPicksSixDistinctSortedNumbersInRange() {
        assertValid(picker.uniform());
    }

    @RepeatedTest(200)
    void filteredSatisfiesConditions() {
        List<Integer> numbers = picker.filtered();
        assertValid(numbers);
        assertThat(NumberPicker.passesFilter(numbers)).isTrue();
    }

    @Test
    void weightedNeverPicksZeroWeightNumbers() {
        double[] w = new double[NumberPicker.MAX + 1];
        for (int n = 1; n <= 6; n++) {
            w[n] = 1;
        }
        for (int i = 0; i < 100; i++) {
            assertThat(picker.weighted(w)).containsExactly(1, 2, 3, 4, 5, 6);
        }
    }

    @Test
    void filterRules() {
        assertThat(NumberPicker.passesFilter(List.of(5, 12, 23, 28, 34, 41))).isTrue();   // 홀3, 합143
        assertThat(NumberPicker.passesFilter(List.of(1, 3, 5, 7, 9, 45))).isFalse();      // 홀6
        assertThat(NumberPicker.passesFilter(List.of(1, 2, 4, 6, 8, 10))).isFalse();      // 합31
        assertThat(NumberPicker.passesFilter(List.of(20, 21, 22, 30, 33, 40))).isFalse(); // 연속 3개
    }

    private static void assertValid(List<Integer> numbers) {
        assertThat(numbers).hasSize(6).isSorted().allMatch(n -> n >= 1 && n <= 45);
        assertThat(new HashSet<>(numbers)).hasSize(6);
    }
}
