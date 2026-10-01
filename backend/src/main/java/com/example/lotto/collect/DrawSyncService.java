package com.example.lotto.collect;

import com.example.lotto.draw.LottoDraw;
import com.example.lotto.draw.LottoDrawRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DrawSyncService {

    private static final Logger log = LoggerFactory.getLogger(DrawSyncService.class);

    private final DhLotteryClient client;
    private final LottoDrawRepository repository;
    private final long requestDelayMs;
    private final ReentrantLock lock = new ReentrantLock();

    public DrawSyncService(DhLotteryClient client,
                           LottoDrawRepository repository,
                           @Value("${lotto.sync.request-delay-ms:300}") long requestDelayMs) {
        this.client = client;
        this.repository = repository;
        this.requestDelayMs = requestDelayMs;
    }

    /**
     * DB에 없는 회차를 최신 회차까지 받아 저장한다.
     *
     * @return 새로 저장한 회차 수 (이미 다른 수집이 진행 중이면 0)
     */
    public int syncAll() {
        if (!lock.tryLock()) {
            log.info("당첨번호 수집이 이미 진행 중입니다.");
            return 0;
        }
        try {
            int next = repository.findTopByOrderByDrawNoDesc().map(d -> d.getDrawNo() + 1).orElse(1);
            int saved = 0;
            while (true) {
                List<LottoDraw> fresh = fetchFrom(next);
                if (fresh.isEmpty()) {
                    break;
                }
                repository.saveAll(fresh);
                saved += fresh.size();
                next = fresh.getLast().getDrawNo() + 1;
                sleep();
            }
            log.info("당첨번호 수집 완료: {}개 회차 추가, 다음 회차 {}", saved, next);
            return saved;
        } finally {
            lock.unlock();
        }
    }

    /** next 회차부터 이어지는 회차들. 응답 구간이 정확하지 않아 start+4로 먼저 묻고, 비면 start로 다시 묻는다. */
    private List<LottoDraw> fetchFrom(int next) {
        List<LottoDraw> draws = toNewDraws(client.fetchAround(next + 4), next);
        if (draws.isEmpty()) {
            sleep();
            draws = toNewDraws(client.fetchAround(next), next);
        }
        return draws;
    }

    private static List<LottoDraw> toNewDraws(List<DhLotteryResponse.Item> items, int next) {
        return items.stream()
                .filter(i -> i.ltEpsd() >= next)
                .sorted(Comparator.comparingInt(DhLotteryResponse.Item::ltEpsd))
                .map(DhLotteryResponse.Item::toEntity)
                .toList();
    }

    private void sleep() {
        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("수집이 중단되었습니다.", e);
        }
    }
}
