package com.example.lotto.web;

import com.example.lotto.recommend.RecommendService;
import com.example.lotto.recommend.RecommendService.RecommendationView;
import com.example.lotto.recommend.Strategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@Validated
public class RecommendationController {

    private final RecommendService recommendService;

    public RecommendationController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @PostMapping
    public List<RecommendationView> recommend(@Valid @RequestBody RecommendRequest request) {
        return recommendService.recommend(request.strategy(), request.count());
    }

    @GetMapping
    public PageResponse<RecommendationView> history(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(recommendService.history(page, size));
    }

    public record RecommendRequest(@NotNull Strategy strategy, @Min(1) @Max(5) int count) {
    }
}
