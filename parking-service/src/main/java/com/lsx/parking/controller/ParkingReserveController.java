package com.lsx.parking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.parking.dto.ParkingReserveAdminCancelDTO;
import com.lsx.parking.dto.ParkingReserveCancelDTO;
import com.lsx.parking.dto.ParkingReserveCreateDTO;
import com.lsx.parking.dto.ParkingReserveQueryDTO;
import com.lsx.parking.service.ParkingReserveService;
import com.lsx.parking.vo.ParkingReserveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parking/reserve")
@Tag(name = "停车-车位预订")
@Slf4j
public class ParkingReserveController {

    @Autowired
    private ParkingReserveService parkingReserveService;

    @PostMapping
    @Operation(summary = "车位预订（用户发起）")
    public Result<Map<String, Object>> createReserve(@RequestBody ParkingReserveCreateDTO dto) {
        try {
            Long reserveId = parkingReserveService.createReserve(dto);
            Map<String, Object> data = new HashMap<>();
            data.put("reserveId", reserveId);
            data.put("expireTime", dto.getReserveEndTime());
            data.put("qrCode", "https://example.com/qr/" + reserveId);
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建车位预订异常", e);
            return Result.fail("预订失败，请稍后再试");
        }
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消预订（用户主动取消）")
    public Result<Map<String, Object>> cancelReserve(@RequestBody ParkingReserveCancelDTO dto) {
        try {
            Boolean success = parkingReserveService.cancelReserve(dto);
            Map<String, Object> data = new HashMap<>();
            data.put("success", success);
            return success ? Result.success(data) : Result.fail("取消失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("取消车位预订异常", e);
            return Result.fail("取消失败，请稍后再试");
        }
    }

    @GetMapping("/my/list")
    @Operation(summary = "我的预订列表（用户查看）")
    public Result<Map<String, Object>> listMyReserves(@RequestParam("userId") Long userId,
                                                      @RequestParam(value = "status", required = false) String status,
                                                      @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                      @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            ParkingReserveQueryDTO dto = new ParkingReserveQueryDTO();
            dto.setStatus(status);
            dto.setPageNum(pageNum);
            dto.setPageSize(pageSize);
            IPage<ParkingReserveVO> page = parkingReserveService.listMyReserves(userId, dto);

            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询我的预订列表异常", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }

    @GetMapping("/admin/list")
    @Operation(summary = "管理员查看预订列表")
    public Result<Map<String, Object>> adminListReserves(@RequestParam(value = "spaceNo", required = false) String spaceNo,
                                                         @RequestParam(value = "userId", required = false) Long userId,
                                                         @RequestParam(value = "status", required = false) String status,
                                                         @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            ParkingReserveQueryDTO dto = new ParkingReserveQueryDTO();
            dto.setSpaceNo(spaceNo);
            dto.setUserId(userId);
            dto.setStatus(status);
            dto.setPageNum(pageNum);
            dto.setPageSize(pageSize);

            IPage<ParkingReserveVO> page = parkingReserveService.adminListReserves(dto);
            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员查询预订列表异常", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }

    @PostMapping("/admin/cancel")
    @Operation(summary = "管理员强制取消预订")
    public Result<Map<String, Object>> adminCancelReserve(@RequestBody ParkingReserveAdminCancelDTO dto) {
        try {
            Boolean success = parkingReserveService.adminCancelReserve(dto);
            Map<String, Object> data = new HashMap<>();
            data.put("success", success);
            return success ? Result.success(data) : Result.fail("取消失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员强制取消预订异常", e);
            return Result.fail("取消失败，请稍后再试");
        }
    }
}
