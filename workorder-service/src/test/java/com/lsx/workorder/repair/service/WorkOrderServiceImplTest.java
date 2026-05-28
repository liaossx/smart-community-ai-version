package com.lsx.workorder.repair.service;

import com.lsx.workorder.ai.entity.AiWorkOrderAnalysis;
import com.lsx.workorder.ai.service.AiWorkOrderAnalysisService;
import com.lsx.workorder.client.HouseServiceClient;
import com.lsx.workorder.client.UserServiceClient;
import com.lsx.core.common.Util.UserContext;
import com.lsx.workorder.dto.external.UserInfoDTO;
import com.lsx.workorder.repair.entity.Repair;
import com.lsx.workorder.repair.entity.WorkOrder;
import com.lsx.workorder.repair.mapper.WorkOrderMapper;
import com.lsx.workorder.repair.service.impl.WorkOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkOrderServiceImplTest {

    @org.junit.jupiter.api.AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void createFromRepairCopiesLatestAiPriorityWithoutAssigningWorker() {
        Repair repair = new Repair();
        repair.setId(100L);
        repair.setCommunityId(1L);

        AiWorkOrderAnalysis latest = new AiWorkOrderAnalysis();
        latest.setRepairId(100L);
        latest.setPriority(3);
        latest.setRecommendedTeam("Plumbing");

        RepairService repairService = mock(RepairService.class);
        AiWorkOrderAnalysisService aiWorkOrderAnalysisService = mock(AiWorkOrderAnalysisService.class);
        WorkOrderMapper mapper = mock(WorkOrderMapper.class);

        when(repairService.getById(100L)).thenReturn(repair);
        when(aiWorkOrderAnalysisService.getLatestAnalysis(100L)).thenReturn(latest);
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
        when(mapper.insert(any())).thenAnswer(invocation -> {
            WorkOrder inserted = invocation.getArgument(0);
            inserted.setId(1L);
            return 1;
        });

        WorkOrderServiceImpl service = serviceWith(repairService, aiWorkOrderAnalysisService, mapper);

        WorkOrder order = service.createFromRepair(100L);

        assertThat(order.getRepairId()).isEqualTo(100L);
        assertThat(order.getCommunityId()).isEqualTo(1L);
        assertThat(order.getPriority()).isEqualTo(3);
        assertThat(order.getWorkerId()).isNull();
        assertThat(order.getWorkerName()).isNull();
        assertThat(order.getWorkerPhone()).isNull();
        verify(aiWorkOrderAnalysisService, never()).analyzeRepair(100L);
    }

    @Test
    void createFromRepairCreatesAiSnapshotWhenLatestDoesNotExist() {
        Repair repair = new Repair();
        repair.setId(100L);
        repair.setCommunityId(1L);

        AiWorkOrderAnalysis created = new AiWorkOrderAnalysis();
        created.setRepairId(100L);
        created.setPriority(4);
        created.setRecommendedTeam("Electrical");

        RepairService repairService = mock(RepairService.class);
        AiWorkOrderAnalysisService aiWorkOrderAnalysisService = mock(AiWorkOrderAnalysisService.class);
        WorkOrderMapper mapper = mock(WorkOrderMapper.class);

        when(repairService.getById(100L)).thenReturn(repair);
        when(aiWorkOrderAnalysisService.getLatestAnalysis(100L))
                .thenThrow(new RuntimeException("AI analysis snapshot not found"));
        when(aiWorkOrderAnalysisService.analyzeRepair(100L)).thenReturn(created);
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
        when(mapper.insert(any())).thenAnswer(invocation -> {
            WorkOrder inserted = invocation.getArgument(0);
            inserted.setId(1L);
            return 1;
        });

        WorkOrderServiceImpl service = serviceWith(repairService, aiWorkOrderAnalysisService, mapper);

        WorkOrder order = service.createFromRepair(100L);

        assertThat(order.getPriority()).isEqualTo(4);
        assertThat(order.getWorkerId()).isNull();
        assertThat(order.getWorkerName()).isNull();
        assertThat(order.getWorkerPhone()).isNull();
        verify(aiWorkOrderAnalysisService).analyzeRepair(100L);
    }

    @Test
    void createFromRepairUsesDefaultPriorityWhenAiAnalysisFails() {
        Repair repair = new Repair();
        repair.setId(100L);
        repair.setCommunityId(1L);

        RepairService repairService = mock(RepairService.class);
        AiWorkOrderAnalysisService aiWorkOrderAnalysisService = mock(AiWorkOrderAnalysisService.class);
        WorkOrderMapper mapper = mock(WorkOrderMapper.class);

        when(repairService.getById(100L)).thenReturn(repair);
        when(aiWorkOrderAnalysisService.getLatestAnalysis(100L))
                .thenThrow(new RuntimeException("AI analysis snapshot not found"));
        when(aiWorkOrderAnalysisService.analyzeRepair(100L))
                .thenThrow(new RuntimeException("AI provider unavailable"));
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
        when(mapper.insert(any())).thenAnswer(invocation -> {
            WorkOrder inserted = invocation.getArgument(0);
            inserted.setId(1L);
            return 1;
        });

        WorkOrderServiceImpl service = serviceWith(repairService, aiWorkOrderAnalysisService, mapper);

        WorkOrder order = service.createFromRepair(100L);

        assertThat(order.getPriority()).isEqualTo(1);
        assertThat(order.getWorkerId()).isNull();
        assertThat(order.getWorkerName()).isNull();
        assertThat(order.getWorkerPhone()).isNull();
        verify(aiWorkOrderAnalysisService).analyzeRepair(100L);
        verify(mapper).insert(any());
    }

    @Test
    void createFromRepairReturnsExistingWorkOrderWithoutAiWork() {
        Repair repair = new Repair();
        repair.setId(100L);
        repair.setCommunityId(1L);

        WorkOrder existing = new WorkOrder();
        existing.setId(9L);
        existing.setRepairId(100L);
        existing.setPriority(2);
        existing.setWorkerId(88L);
        existing.setWorkerName("Existing Worker");

        RepairService repairService = mock(RepairService.class);
        AiWorkOrderAnalysisService aiWorkOrderAnalysisService = mock(AiWorkOrderAnalysisService.class);
        WorkOrderMapper mapper = mock(WorkOrderMapper.class);

        when(repairService.getById(100L)).thenReturn(repair);
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(existing);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        WorkOrderServiceImpl service = serviceWith(repairService, aiWorkOrderAnalysisService, mapper);

        WorkOrder order = service.createFromRepair(100L);

        assertThat(order).isSameAs(existing);
        assertThat(order.getPriority()).isEqualTo(2);
        assertThat(order.getWorkerId()).isEqualTo(88L);
        verify(aiWorkOrderAnalysisService, never()).getLatestAnalysis(100L);
        verify(aiWorkOrderAnalysisService, never()).analyzeRepair(100L);
        verify(mapper, never()).insert(any());
    }

    @Test
    void assignToWorkerRejectsWorkerFromDifferentCommunity() {
        WorkOrder order = new WorkOrder();
        order.setId(9L);
        order.setCommunityId(1L);
        order.setStatus("PENDING");

        UserInfoDTO worker = new UserInfoDTO();
        worker.setId(88L);
        worker.setUserId(88L);
        worker.setRole("worker");
        worker.setCommunityId(2L);

        UserContext.setRole("admin");
        UserContext.setCommunityId(1L);

        RepairService repairService = mock(RepairService.class);
        AiWorkOrderAnalysisService aiWorkOrderAnalysisService = mock(AiWorkOrderAnalysisService.class);
        WorkOrderMapper mapper = mock(WorkOrderMapper.class);
        UserServiceClient userServiceClient = mock(UserServiceClient.class);

        when(mapper.selectById(9L)).thenReturn(order);
        when(userServiceClient.getUserById(88L)).thenReturn(worker);

        WorkOrderServiceImpl service = serviceWith(repairService, aiWorkOrderAnalysisService, mapper, userServiceClient);

        assertThatThrownBy(() -> service.assignToWorker(9L, 88L, "Worker B", "13800000000", 2))
                .hasMessageContaining("维修人员与工单不属于同一社区");

        verify(mapper, never()).updateById(any());
    }

    @Test
    void assignToWorkerAcceptsWorkerFromSameCommunity() {
        WorkOrder order = new WorkOrder();
        order.setId(9L);
        order.setCommunityId(1L);
        order.setStatus("PENDING");

        UserInfoDTO worker = new UserInfoDTO();
        worker.setId(88L);
        worker.setUserId(88L);
        worker.setRole("worker");
        worker.setCommunityId(1L);

        UserContext.setRole("admin");
        UserContext.setCommunityId(1L);

        RepairService repairService = mock(RepairService.class);
        AiWorkOrderAnalysisService aiWorkOrderAnalysisService = mock(AiWorkOrderAnalysisService.class);
        WorkOrderMapper mapper = mock(WorkOrderMapper.class);
        UserServiceClient userServiceClient = mock(UserServiceClient.class);

        when(mapper.selectById(9L)).thenReturn(order);
        when(mapper.updateById(any())).thenReturn(1);
        when(userServiceClient.getUserById(88L)).thenReturn(worker);

        WorkOrderServiceImpl service = serviceWith(repairService, aiWorkOrderAnalysisService, mapper, userServiceClient);

        boolean assigned = service.assignToWorker(9L, 88L, "Worker A", "13800000000", 2);

        assertThat(assigned).isTrue();
        assertThat(order.getWorkerId()).isEqualTo(88L);
        assertThat(order.getStatus()).isEqualTo("ASSIGNED");
    }

    private WorkOrderServiceImpl serviceWith(RepairService repairService,
                                             AiWorkOrderAnalysisService aiWorkOrderAnalysisService,
                                             WorkOrderMapper mapper) {
        return serviceWith(repairService, aiWorkOrderAnalysisService, mapper, mock(UserServiceClient.class));
    }

    private WorkOrderServiceImpl serviceWith(RepairService repairService,
                                             AiWorkOrderAnalysisService aiWorkOrderAnalysisService,
                                             WorkOrderMapper mapper,
                                             UserServiceClient userServiceClient) {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        ReflectionTestUtils.setField(service, "repairService", repairService);
        ReflectionTestUtils.setField(service, "aiWorkOrderAnalysisService", aiWorkOrderAnalysisService);
        ReflectionTestUtils.setField(service, "houseServiceClient", mock(HouseServiceClient.class));
        ReflectionTestUtils.setField(service, "userServiceClient", userServiceClient);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        return service;
    }
}
