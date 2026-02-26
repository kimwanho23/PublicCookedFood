package kwh.PublicCookedFood.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TempImageCleanupScheduler {

    private final ImageService imageService;

    @Value("${app.image.temp-cleanup.enabled:true}")
    private boolean enabled;

    @Value("${app.image.temp-cleanup.ttl-hours:24}")
    private long ttlHours;

    @Value("${app.image.temp-cleanup.batch-size:100}")
    private int batchSize;

    @Value("${app.image.temp-cleanup.max-batches-per-run:20}")
    private int maxBatchesPerRun;

    @Scheduled(cron = "${app.image.temp-cleanup.cron:0 0 * * * *}")
    public void cleanupStaleTempImages() {
        if (!enabled) {
            return;
        }
        if (ttlHours <= 0 || batchSize <= 0 || maxBatchesPerRun <= 0) {
            log.warn("TEMP 이미지 정리 스케줄 설정값이 유효하지 않습니다. ttlHours={}, batchSize={}, maxBatches={}",
                    ttlHours, batchSize, maxBatchesPerRun);
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusHours(ttlHours);
        int totalDeleted = 0;
        int batchCount = 0;

        while (batchCount < maxBatchesPerRun) {
            int deleted = imageService.deleteStaleTempImages(cutoff, batchSize);
            totalDeleted += deleted;
            batchCount++;

            if (deleted < batchSize) {
                break;
            }
        }

        if (totalDeleted > 0) {
            log.info("TEMP 이미지 정리 완료. deleted={}, cutoff={}, batches={}",
                    totalDeleted, cutoff, batchCount);
        }

        if (batchCount == maxBatchesPerRun) {
            log.warn("TEMP 이미지 정리가 최대 배치 수에 도달했습니다. maxBatches={}, cutoff={}",
                    maxBatchesPerRun, cutoff);
        }
    }
}
