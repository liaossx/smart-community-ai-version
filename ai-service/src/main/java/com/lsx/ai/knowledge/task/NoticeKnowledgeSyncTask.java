package com.lsx.ai.knowledge.task;

import com.lsx.ai.knowledge.dto.KnowledgeSyncResponse;
import com.lsx.ai.knowledge.service.NoticeKnowledgeSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "smart-community.ai.knowledge.sync.notices", name = "enabled", havingValue = "true")
public class NoticeKnowledgeSyncTask {
    private static final Logger log = LoggerFactory.getLogger(NoticeKnowledgeSyncTask.class);

    private final NoticeKnowledgeSyncService noticeKnowledgeSyncService;

    public NoticeKnowledgeSyncTask(NoticeKnowledgeSyncService noticeKnowledgeSyncService) {
        this.noticeKnowledgeSyncService = noticeKnowledgeSyncService;
    }

    @Scheduled(
            initialDelayString = "${smart-community.ai.knowledge.sync.notices.initial-delay-ms:30000}",
            fixedDelayString = "${smart-community.ai.knowledge.sync.notices.fixed-delay-ms:300000}"
    )
    public void syncPublishedNotices() {
        KnowledgeSyncResponse response = noticeKnowledgeSyncService.syncPublishedNotices();
        if (response.getFailedCount() != null && response.getFailedCount() > 0) {
            log.warn("Scheduled notice knowledge sync finished with failures. batchNo={}, scanned={}, synced={}, disabled={}, failed={}, messages={}",
                    response.getSyncBatchNo(),
                    response.getScannedCount(),
                    response.getSyncedCount(),
                    response.getDisabledCount(),
                    response.getFailedCount(),
                    response.getMessages());
            return;
        }
        log.info("Scheduled notice knowledge sync finished. batchNo={}, scanned={}, synced={}, disabled={}",
                response.getSyncBatchNo(),
                response.getScannedCount(),
                response.getSyncedCount(),
                response.getDisabledCount());
    }
}
