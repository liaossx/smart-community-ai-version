package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsActionTaskCreateRequest;
import com.lsx.ai.operations.dto.OperationsActionTaskCreateResponse;
import com.lsx.ai.operations.dto.OperationsActionTaskItem;
import com.lsx.ai.operations.dto.OperationsActionTaskPageResponse;
import com.lsx.ai.operations.dto.OperationsActionTaskUpdateRequest;

public interface OperationsActionTaskService {
    OperationsActionTaskCreateResponse createFromInsights(OperationsActionTaskCreateRequest request);

    OperationsActionTaskPageResponse list(Long communityId, String status, String taskBatchNo,
                                          Integer pageNum, Integer pageSize);

    OperationsActionTaskItem getRequired(Long id);

    OperationsActionTaskItem updateStatus(Long id, OperationsActionTaskUpdateRequest request);
}
