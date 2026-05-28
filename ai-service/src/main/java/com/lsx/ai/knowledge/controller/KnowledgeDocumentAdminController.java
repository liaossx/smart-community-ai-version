package com.lsx.ai.knowledge.controller;

import com.lsx.ai.knowledge.dto.KnowledgeDocumentItem;
import com.lsx.ai.knowledge.dto.KnowledgeDocumentMutationResponse;
import com.lsx.ai.knowledge.dto.KnowledgeDocumentPageResponse;
import com.lsx.ai.knowledge.dto.KnowledgeDocumentSaveRequest;
import com.lsx.ai.knowledge.service.KnowledgeDocumentAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge/documents")
public class KnowledgeDocumentAdminController {
    private final KnowledgeDocumentAdminService adminService;

    public KnowledgeDocumentAdminController(KnowledgeDocumentAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public KnowledgeDocumentMutationResponse create(@Valid @RequestBody KnowledgeDocumentSaveRequest request) {
        return adminService.create(request);
    }

    @PutMapping("/{id}")
    public KnowledgeDocumentMutationResponse update(@PathVariable("id") Long id,
                                                    @Valid @RequestBody KnowledgeDocumentSaveRequest request) {
        return adminService.update(id, request);
    }

    @GetMapping("/{id}")
    public KnowledgeDocumentItem get(@PathVariable("id") Long id) {
        return adminService.getRequired(id);
    }

    @GetMapping
    public KnowledgeDocumentPageResponse list(@RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "sourceType", required = false) String sourceType,
                                              @RequestParam(value = "status", required = false) String status,
                                              @RequestParam(value = "communityId", required = false) Long communityId,
                                              @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                              @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return adminService.list(keyword, sourceType, status, communityId, pageNum, pageSize);
    }

    @PostMapping("/{id}/disable")
    public KnowledgeDocumentMutationResponse disable(@PathVariable("id") Long id) {
        return adminService.disable(id);
    }
}
