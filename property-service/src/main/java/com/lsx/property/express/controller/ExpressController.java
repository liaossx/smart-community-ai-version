package com.lsx.property.express.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.property.express.dto.ExpressAuthorizeDTO;
import com.lsx.property.express.dto.ExpressCreateDTO;
import com.lsx.property.express.dto.ExpressPickDTO;
import com.lsx.property.express.service.ExpressService;
import com.lsx.property.express.vo.ExpressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/express")
@Tag(name = "快递管理接口", description = "快递登记、查询、取件、授权")
@Slf4j
public class ExpressController {

    @Autowired
    private ExpressService expressService;

    @PostMapping
    @Operation(summary = "物业登记快递", description = "登记快递并生成取件码/存放位置")
    public Result<Long> registerExpress(@RequestBody ExpressCreateDTO dto) {
        try {
            Long id = expressService.registerExpress(dto);
            return Result.success(id);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("登记快递失败", e);
            return Result.fail("登记失败，请稍后重试");
        }
    }

    @GetMapping("/my")
    @Operation(summary = "业主查询自己的快递", description = "按记录时间倒序")
    public Result<Page<ExpressVO>> listMyExpress(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Page<ExpressVO> page = expressService.listMyExpress(userId, pageNum, pageSize);
            return Result.success(page);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询快递失败", e);
            return Result.fail("查询失败，请稍后重试");
        }
    }

    @PutMapping("/{id}/pick")
    @Operation(summary = "业主确认取件", description = "校验取件码，更新快递状态")
    public Result<Boolean> pickExpress(@PathVariable("id") Long expressId,
                                       @RequestBody ExpressPickDTO dto) {
        try {
            Boolean success = expressService.pickExpress(expressId, dto);
            return success ? Result.success(true) : Result.fail("取件失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("取件失败", e);
            return Result.fail("取件失败，请稍后重试");
        }
    }

    @PostMapping("/{id}/authorize")
    @Operation(summary = "业主授权代取", description = "记录授权人信息及有效期")
    public Result<Boolean> authorizeExpress(@PathVariable("id") Long expressId,
                                            @RequestBody ExpressAuthorizeDTO dto) {
        try {
            Boolean success = expressService.authorizeExpress(expressId, dto);
            return success ? Result.success(true) : Result.fail("授权失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("授权失败", e);
            return Result.fail("授权失败，请稍后重试");
        }
    }
}
