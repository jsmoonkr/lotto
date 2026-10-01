package com.example.lotto.web;

import com.example.lotto.stats.StatsService;
import com.example.lotto.stats.StatsService.FrequencyStats;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Validated
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/frequency")
    public FrequencyStats frequency(@RequestParam(required = false) @Min(1) Integer recent) {
        return statsService.frequency(recent);
    }
}
