package com.example.lotto.recommend;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * 1~45에서 번호 6개를 뽑는 순수 로직. DB와 무관해서 단위 테스트하기 쉽게 분리했다.
 */
public final class NumberPicker {

    public static final int PICK = 6;
    public static final int MAX = 45;
    private static final int MAX_FILTER_TRIES = 1000;

    private final RandomGenerator random;

    public NumberPicker(RandomGenerator random) {
        this.random = random;
    }

    /** weights[n]은 번호 n의 가중치(인덱스 0은 쓰지 않음). 비복원 추출 후 오름차순 정렬. */
    public List<Integer> weighted(double[] weights) {
        double[] w = weights.clone();
        List<Integer> picked = new ArrayList<>(PICK);
        while (picked.size() < PICK) {
            double total = 0;
            for (int n = 1; n <= MAX; n++) {
                total += w[n];
            }
            double r = random.nextDouble(total);
            int chosen = MAX;
            for (int n = 1; n <= MAX; n++) {
                r -= w[n];
                if (r < 0) {
                    chosen = n;
                    break;
                }
            }
            // 부동소수 오차로 마지막까지 간 경우 아직 안 뽑힌 가장 큰 번호를 쓴다.
            while (w[chosen] == 0) {
                chosen--;
            }
            picked.add(chosen);
            w[chosen] = 0;
        }
        return picked.stream().sorted().toList();
    }

    public List<Integer> uniform() {
        return weighted(uniformWeights());
    }

    public List<Integer> filtered() {
        for (int i = 0; i < MAX_FILTER_TRIES; i++) {
            List<Integer> numbers = uniform();
            if (passesFilter(numbers)) {
                return numbers;
            }
        }
        throw new IllegalStateException("조건에 맞는 조합을 찾지 못했습니다.");
    }

    /** 홀수 2~4개, 합계 100~175, 연속번호는 최대 2개까지 이어짐. numbers는 오름차순. */
    public static boolean passesFilter(List<Integer> numbers) {
        long odd = numbers.stream().filter(n -> n % 2 == 1).count();
        int sum = numbers.stream().mapToInt(Integer::intValue).sum();
        int run = 1;
        int maxRun = 1;
        for (int i = 1; i < numbers.size(); i++) {
            run = numbers.get(i) == numbers.get(i - 1) + 1 ? run + 1 : 1;
            maxRun = Math.max(maxRun, run);
        }
        return odd >= 2 && odd <= 4 && sum >= 100 && sum <= 175 && maxRun <= 2;
    }

    public static double[] uniformWeights() {
        double[] w = new double[MAX + 1];
        for (int n = 1; n <= MAX; n++) {
            w[n] = 1;
        }
        return w;
    }
}
