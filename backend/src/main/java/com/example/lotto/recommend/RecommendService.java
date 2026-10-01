package com.example.lotto.recommend;

import com.example.lotto.draw.LottoDraw;
import com.example.lotto.draw.LottoDrawRepository;
import com.example.lotto.stats.StatsService;
import com.example.lotto.stats.StatsService.FrequencyStats;
import com.example.lotto.stats.StatsService.NumberStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Service
public class RecommendService {

    private final StatsService statsService;
    private final RecommendationRepository recommendationRepository;
    private final LottoDrawRepository drawRepository;
    private final NumberPicker picker = new NumberPicker(new SecureRandom());

    public RecommendService(StatsService statsService,
                            RecommendationRepository recommendationRepository,
                            LottoDrawRepository drawRepository) {
        this.statsService = statsService;
        this.recommendationRepository = recommendationRepository;
        this.drawRepository = drawRepository;
    }

    @Transactional
    public List<RecommendationView> recommend(Long userId, Strategy strategy, int count) {
        FrequencyStats stats = statsService.frequency(null);
        int targetDrawNo = stats.latestDrawNo() + 1;

        List<Recommendation> saved = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            List<Integer> numbers = switch (strategy) {
                case RANDOM -> picker.uniform();
                case FILTERED -> picker.filtered();
                // +1: 한 번도 안 나왔거나 방금 나온 번호도 뽑힐 여지를 남긴다.
                case FREQUENCY -> picker.weighted(weights(stats, s -> s.count() + 1.0));
                case OVERDUE -> picker.weighted(weights(stats, s -> s.gap() + 1.0));
            };
            saved.add(recommendationRepository.save(new Recommendation(userId, strategy, targetDrawNo, numbers)));
        }
        return saved.stream().map(r -> RecommendationView.of(r, null)).toList();
    }

    @Transactional(readOnly = true)
    public Page<RecommendationView> history(Long userId, int page, int size) {
        Page<Recommendation> recs = recommendationRepository.findByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        List<Integer> targets = recs.stream().map(Recommendation::getTargetDrawNo).distinct().toList();
        Map<Integer, LottoDraw> draws = drawRepository.findByDrawNoIn(targets).stream()
                .collect(Collectors.toMap(LottoDraw::getDrawNo, Function.identity()));
        return recs.map(r -> RecommendationView.of(r, draws.get(r.getTargetDrawNo())));
    }

    private static double[] weights(FrequencyStats stats, ToDoubleFunction<NumberStat> f) {
        double[] w = new double[NumberPicker.MAX + 1];
        for (NumberStat s : stats.numbers()) {
            w[s.number()] = f.applyAsDouble(s);
        }
        return w;
    }

    /**
     * drawn은 추첨이 끝났을 때만 채워진다. matched와 rank는 그때만 의미가 있다.
     */
    public record RecommendationView(Long id, LocalDateTime createdAt, Strategy strategy, int targetDrawNo,
                                     List<Integer> numbers, boolean drawn, List<Integer> matched,
                                     boolean bonusMatched, int rank) {

        static RecommendationView of(Recommendation r, LottoDraw draw) {
            List<Integer> numbers = r.numbers();
            if (draw == null) {
                return new RecommendationView(r.getId(), r.getCreatedAt(), r.getStrategy(), r.getTargetDrawNo(),
                        numbers, false, List.of(), false, 0);
            }
            List<Integer> winning = draw.numbers();
            return new RecommendationView(r.getId(), r.getCreatedAt(), r.getStrategy(), r.getTargetDrawNo(),
                    numbers, true,
                    numbers.stream().filter(winning::contains).toList(),
                    numbers.contains(draw.getBonus()),
                    PrizeRank.of(numbers, winning, draw.getBonus()));
        }
    }
}
