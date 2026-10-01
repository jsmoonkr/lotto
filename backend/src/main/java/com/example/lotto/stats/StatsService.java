package com.example.lotto.stats;

import com.example.lotto.draw.LottoDraw;
import com.example.lotto.draw.LottoDrawRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatsService {

    public static final int MAX_NUMBER = 45;

    private final LottoDrawRepository repository;

    public StatsService(LottoDrawRepository repository) {
        this.repository = repository;
    }

    /**
     * 번호별 출현 통계. recent가 있으면 최근 recent개 회차만 센다.
     * 미출현 기간(gap)은 범위와 상관없이 전체 기록 기준이다.
     */
    public FrequencyStats frequency(Integer recent) {
        int latest = repository.findTopByOrderByDrawNoDesc().map(LottoDraw::getDrawNo).orElse(0);
        int from = recent == null ? 1 : Math.max(1, latest - recent + 1);
        List<LottoDraw> draws = repository.findAll();

        long[] counts = new long[MAX_NUMBER + 1];
        int[] lastSeen = new int[MAX_NUMBER + 1];
        int drawCount = 0;
        for (LottoDraw draw : draws) {
            boolean inRange = draw.getDrawNo() >= from;
            if (inRange) {
                drawCount++;
            }
            for (int n : draw.numbers()) {
                if (inRange) {
                    counts[n]++;
                }
                lastSeen[n] = Math.max(lastSeen[n], draw.getDrawNo());
            }
        }

        List<NumberStat> numbers = new ArrayList<>(MAX_NUMBER);
        for (int n = 1; n <= MAX_NUMBER; n++) {
            int gap = lastSeen[n] == 0 ? latest : latest - lastSeen[n];
            numbers.add(new NumberStat(n, counts[n], lastSeen[n] == 0 ? null : lastSeen[n], gap));
        }
        return new FrequencyStats(latest, drawCount, numbers);
    }

    public record FrequencyStats(int latestDrawNo, int drawCount, List<NumberStat> numbers) {
    }

    /** gap: 마지막으로 나온 뒤 지난 회차 수 (최신 회차에 나왔으면 0) */
    public record NumberStat(int number, long count, Integer lastDrawNo, int gap) {
    }
}
