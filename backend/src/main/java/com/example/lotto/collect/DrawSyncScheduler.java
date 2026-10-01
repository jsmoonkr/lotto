package com.example.lotto.collect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "lotto.sync.enabled", havingValue = "true", matchIfMissing = true)
public class DrawSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DrawSyncScheduler.class);

    private final DrawSyncService syncService;

    public DrawSyncScheduler(DrawSyncService syncService) {
        this.syncService = syncService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        run();
    }

    /** 추첨은 토요일 20:35경. 결과가 늦게 올라올 때를 대비해 일요일 아침에 한 번 더 확인한다. */
    @Scheduled(cron = "0 0 22 * * SAT", zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 9 * * SUN", zone = "Asia/Seoul")
    public void weekly() {
        run();
    }

    private void run() {
        try {
            syncService.syncAll();
        } catch (RuntimeException e) {
            log.warn("당첨번호 수집 실패: {}", e.getMessage(), e);
        }
    }
}
