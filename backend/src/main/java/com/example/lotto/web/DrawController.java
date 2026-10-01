package com.example.lotto.web;

import com.example.lotto.collect.DrawSyncService;
import com.example.lotto.draw.LottoDraw;
import com.example.lotto.draw.LottoDrawRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/draws")
@Validated
public class DrawController {

    private final LottoDrawRepository repository;
    private final DrawSyncService syncService;

    public DrawController(LottoDrawRepository repository, DrawSyncService syncService) {
        this.repository = repository;
        this.syncService = syncService;
    }

    @GetMapping
    public PageResponse<DrawView> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(repository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "drawNo")))
                .map(DrawView::of));
    }

    @GetMapping("/latest")
    public DrawView latest() {
        return repository.findTopByOrderByDrawNoDesc().map(DrawView::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "아직 수집된 회차가 없습니다."));
    }

    @GetMapping("/{drawNo}")
    public DrawView get(@PathVariable int drawNo) {
        return repository.findById(drawNo).map(DrawView::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, drawNo + "회차가 없습니다."));
    }

    @PostMapping("/sync")
    public Map<String, Integer> sync() {
        return Map.of("added", syncService.syncAll());
    }

    public record Prize(int rank, long winners, long amount) {
    }

    public record DrawView(int drawNo, LocalDate drawDate, List<Integer> numbers, int bonus,
                           List<Prize> prizes, long totalSales) {

        static DrawView of(LottoDraw d) {
            return new DrawView(d.getDrawNo(), d.getDrawDate(), d.numbers(), d.getBonus(),
                    List.of(new Prize(1, d.getFirstWinners(), d.getFirstPrize()),
                            new Prize(2, d.getSecondWinners(), d.getSecondPrize()),
                            new Prize(3, d.getThirdWinners(), d.getThirdPrize()),
                            new Prize(4, d.getFourthWinners(), d.getFourthPrize()),
                            new Prize(5, d.getFifthWinners(), d.getFifthPrize())),
                    d.getTotalSales());
        }
    }
}
