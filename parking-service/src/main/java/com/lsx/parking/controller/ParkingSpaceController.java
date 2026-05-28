package com.lsx.parking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.parking.dto.ParkingAuthorizeDTO;
import com.lsx.parking.dto.ParkingSpaceBindDTO;
import com.lsx.parking.dto.ParkingSpaceQueryDTO;
import com.lsx.parking.service.ParkingSpaceService;
import com.lsx.parking.vo.ParkingAuthorizeVO;
import com.lsx.parking.vo.ParkingSpaceRemainVO;
import com.lsx.parking.vo.ParkingSpaceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lsx.parking.dto.SpaceRenewDTO;

import com.lsx.parking.dto.SpaceOpenDTO;

@RestController
@RequestMapping("/api/parking")
@Tag(name = "车位管理员接口")
@Slf4j
public class ParkingSpaceController {

    @Autowired
    private ParkingSpaceService parkingSpaceService;

    @PostMapping("/space/open")
    @Operation(summary = "车辆/车位首次开通缴费", description = "用于待缴费状态下的首次支付开通")
    public Result<Boolean> openSpace(@RequestBody SpaceOpenDTO dto) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null && dto.getUserId() == null) {
                return Result.fail("未登录");
            }
            if (userId != null) {
                dto.setUserId(userId);
            }
            
            parkingSpaceService.openSpace(dto);
            return Result.success(true);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("开通车位异常", e);
            return Result.fail("开通失败");
        }
    }

    @PostMapping("/space/renew")
    @Operation(summary = "车位续费", description = "支付金额并延长有效期")
    public Result<Boolean> renewSpace(@RequestBody SpaceRenewDTO dto) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null && dto.getUserId() == null) {
                return Result.fail("未登录");
            }
            // 优先使用 Token 中的 userId
            if (userId != null) {
                dto.setUserId(userId);
            }
            
            parkingSpaceService.renewSpace(dto);
            return Result.success(true);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("续费异常", e);
            return Result.fail("续费失败");
        }
    }

    @GetMapping("/space/remaining")
    @Operation(summary = "查询可用车位数量", description = "支持按小区ID查询，未传则统计全部")
    public Result<ParkingSpaceRemainVO> getRemaining(@RequestParam(value = "communityId", required = false) Long communityId) {
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            communityId = currentCommunityId;
        }
        ParkingSpaceRemainVO vo = parkingSpaceService.getRemaining(communityId);
        return Result.success(vo);
    }

    @PostMapping("/space/bind")
    @Operation(summary = "业主绑定固定车位", description = "绑定后车位状态变为占用")
    public Result<Boolean> bindSpace(@RequestBody ParkingSpaceBindDTO dto) {
        try {
            Boolean success = parkingSpaceService.bindSpace(dto);
            return success ? Result.success(true) : Result.fail("绑定失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("绑定车位异常", e);
            return Result.fail("绑定失败，请稍后再试");
        }
    }

    @GetMapping("/space/my")
    @Operation(summary = "查询我绑定的车位")
    public Result<List<ParkingSpaceVO>> listMySpaces() {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.fail("未登录");
            }
            List<ParkingSpaceVO> spaces = parkingSpaceService.listMySpaces(userId);
            return Result.success(spaces);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询车位失败", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }

    @PostMapping("/space/{id}/authorize")
    @Operation(summary = "固定车位授权访客使用")
    public Result<Boolean> authorize(@PathVariable("id") Long spaceId, @RequestBody ParkingAuthorizeDTO dto) {
        try {
            Boolean success = parkingSpaceService.authorizeSpace(spaceId, dto);
            return success ? Result.success(true) : Result.fail("授权失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("车位授权异常", e);
            return Result.fail("授权失败，请稍后再试");
        }
    }

    @GetMapping("/authorize/my")
    @Operation(summary = "业主查询自己的授权记录")
    public Result<IPage<ParkingAuthorizeVO>> listMyAuthorize(@RequestParam("userId") Long userId,
                                                            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            IPage<ParkingAuthorizeVO> page = parkingSpaceService.listMyAuthorizes(userId, pageNum, pageSize);
            return Result.success(page);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询授权记录异常", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }

    @GetMapping("/space/admin/list")
    @Operation(summary = "管理员获取车位列表", description = "支持按编号查询，返回是否占用及业主信息")
    public Result<Map<String, Object>> getAdminSpaceList(ParkingSpaceQueryDTO dto) {
        try {
            IPage<ParkingSpaceVO> page = parkingSpaceService.adminListSpaces(dto);
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("管理员查询车位列表异常", e);
            return Result.fail("查询失败");
        }
    }

    @GetMapping("/space/available")
    @Operation(summary = "查询可用固定车位列表", description = "用于业主绑定固定车位时选择，仅返回状态为AVAILABLE且类型为FIXED的车位")
    public Result<IPage<ParkingSpaceVO>> listAvailableFixedSpaces(
            @RequestParam(required = false) Long communityId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        // 如果未传 communityId，尝试从当前用户获取
        if (communityId == null) {
            communityId = UserContext.getCommunityId();
        }
        
        IPage<ParkingSpaceVO> page = parkingSpaceService.listAvailableFixedSpaces(communityId, pageNum, pageSize);
        return Result.success(page);
    }
}
