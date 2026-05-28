package com.lsx.ai.knowledge.controller;

import com.lsx.ai.knowledge.dto.KnowledgeEmbeddingRebuildResponse;
import com.lsx.ai.knowledge.service.KnowledgeEmbeddingRebuildService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge/embeddings")
public class KnowledgeEmbeddingController {
    private final KnowledgeEmbeddingRebuildService rebuildService;

    public KnowledgeEmbeddingController(KnowledgeEmbeddingRebuildService rebuildService) {
        this.rebuildService = rebuildService;
    }

    @PostMapping("/rebuild")
    public KnowledgeEmbeddingRebuildResponse rebuild() {
        return rebuildService.rebuildAll();
    }
}
