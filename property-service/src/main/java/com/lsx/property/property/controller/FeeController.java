package com.lsx.property.property.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.PaymentSignUtil;
import com.lsx.property.property.dto.CurrentFeeDTO;
import com.lsx.property.property.dto.FeeDTO;
import com.lsx.property.property.dto.FeeHistoryDTO;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.property.property.dto.GenerateFeeDTO;
import com.lsx.property.property.dto.PayFeeDTO;
import com.lsx.property.property.service.FeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fee")
@Slf4j
@Tag(name = "物业费用接口", description = "物业费用的查询、生成、缴纳及历史记录")
public class FeeController {

    @Autowired
    private FeeService feeService;

    @Value("${mock.payment.enabled:false}")
    private boolean mockPaymentEnabled;

    @Value("${internal.token:}")
    private String internalToken;

    @Value("${payment.secret:}")
    private String paymentSecret;

    @Value("${payment.allowedSkewSeconds:300}")
    private long paymentAllowedSkewSeconds;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/current")
    @Operation(summary = "查询当前未缴费用", description = "查询指定用户当前所有未缴纳的物业费用")
    public Result<List<CurrentFeeDTO>> getCurrentUnpaid(
            @Parameter(description = "用户ID", required = true) @RequestParam("userId") Long userId) {
        try {
            log.info("查询用户[{}]未缴费用", userId);
            List<CurrentFeeDTO> result = feeService.getCurrentUnpaid(userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询未缴费用失败", e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/unpaid")
    @Operation(summary = "查询当前未缴费用(别名)", description = "同 /current 接口")
    public Result<List<CurrentFeeDTO>> getUnpaidAlias(@RequestParam("userId") Long userId) {
        try {
            List<CurrentFeeDTO> result = feeService.getCurrentUnpaid(userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/history")
    @Operation(summary = "查询缴费历史", description = "分页查询用户的历史缴费记录")
    public Result<Page<FeeHistoryDTO>> getPaymentHistory(
            @RequestParam Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        try {
            if (userId == null) {
                return Result.fail("用户ID不能为空");
            }

            LocalDateTime start = null;
            LocalDateTime end = null;

            if (startTime != null && startTime.length() == 10) {
                start = LocalDate.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            } else if (startTime != null) {
                start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            if (endTime != null && endTime.length() == 10) {
                end = LocalDate.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atTime(23, 59, 59);
            } else if (endTime != null) {
                end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            Page<FeeHistoryDTO> result = feeService.getPaymentHistory(userId, start, end, pageNum, pageSize);
            return Result.success(result);

        } catch (Exception e) {
            log.error("查询缴费历史失败 userId={}", userId, e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }


    @PostMapping({"/generate", "/admin/generate"})
    @Operation(summary = "生成账单(管理员)", description = "管理员手动触发生成指定周期的物业费账单")
    @Log(title = "生成账单", businessType = BusinessType.INSERT)
    public Result<Boolean> generateBills(
            @Parameter(description = "生成账单参数", required = true) @RequestBody GenerateFeeDTO dto,
            @Parameter(description = "管理员ID", required = true) @RequestParam("adminId") Long adminId) {
        try {
            log.info("管理员[{}]开始生成账单: {}", adminId, dto);
            Boolean success = feeService.generateBills(dto, adminId);
            return success ? Result.success(true) : Result.fail("生成账单失败");
        } catch (Exception e) {
            log.error("生成账单异常", e);
            return Result.fail("生成失败：" + e.getMessage());
        }
    }

    @PutMapping("/pay")
    @Operation(summary = "缴纳费用", description = "用户缴纳指定的物业费用")
    public Result<String> payFee(
            @Parameter(description = "支付参数", required = true) @RequestBody PayFeeDTO dto,
            @Parameter(description = "用户ID", required = true) @RequestParam("userId") Long userId) {
        try {
            log.info("用户[{}]尝试支付账单[{}]", userId, dto.getFeeId());
            String result = feeService.payFee(dto, userId);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.warn("支付失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("支付异常", e);
            return Result.fail("支付失败：" + e.getMessage());
        }
    }

    @PostMapping("/pay")
    @Operation(summary = "缴纳费用(POST)", description = "同 PUT /pay 接口")
    public Result<String> payFeePost(@RequestBody PayFeeDTO dto, @RequestParam("userId") Long userId) {
        try {
            String result = feeService.payFee(dto, userId);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("支付失败：" + e.getMessage());
        }
    }

    @GetMapping({"/list", "/admin/list"})
    @Operation(summary = "管理员查询账单列表", description = "管理员查询所有账单")
    public Result<Page<FeeDTO>> adminList(@RequestParam(value = "status", required = false) String status,
                                          @RequestParam(value = "ownerName", required = false) String ownerName,
                                          @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<FeeDTO> page = feeService.adminList(status, ownerName, pageNum, pageSize);
        return Result.success(page);
    }

    @PostMapping("/pay/callback")
    @Operation(summary = "支付回调", description = "支付渠道异步通知回调")
    public Result<String> payCallback(
            @Parameter(description = "订单号", required = true) @RequestParam("orderNo") String orderNo,
            @Parameter(description = "交易流水号", required = true) @RequestParam("tradeNo") String tradeNo,
            @Parameter(description = "支付状态", required = true) @RequestParam("status") String status,
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            @RequestParam(value = "payChannel", required = false) String payChannel,
            @RequestHeader(value = "X-Pay-Timestamp", required = false) String ts,
            @RequestHeader(value = "X-Pay-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Pay-Sign", required = false) String sign) {
        if (paymentSecret == null || paymentSecret.isEmpty()) {
            return Result.fail("支付配置缺失");
        }
        long now = System.currentTimeMillis() / 1000;
        long timestamp;
        try {
            timestamp = Long.parseLong(ts);
        } catch (Exception e) {
            return Result.fail("时间戳无效");
        }
        if (Math.abs(now - timestamp) > paymentAllowedSkewSeconds) {
            return Result.fail("回调已过期");
        }
        if (nonce == null || nonce.isEmpty()) {
            return Result.fail("nonce缺失");
        }
        String nonceKey = "pay:nonce:fee:" + nonce;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(nonceKey, "1", Duration.ofSeconds(paymentAllowedSkewSeconds * 2));
        if (ok == null || !ok) {
            return Result.fail("重复回调");
        }
        Map<String, String> params = new java.util.HashMap<>();
        params.put("orderNo", orderNo);
        params.put("tradeNo", tradeNo);
        params.put("status", status);
        params.put("amount", amount != null ? amount.toPlainString() : "");
        params.put("payChannel", payChannel != null ? payChannel : "");
        params.put("timestamp", String.valueOf(timestamp));
        params.put("nonce", nonce);
        if (!PaymentSignUtil.verify(paymentSecret, params, sign)) {
            stringRedisTemplate.delete(nonceKey);
            return Result.fail("验签失败");
        }
        try {
            log.info("收到支付回调: orderNo={}, status={}", orderNo, status);
            feeService.payCallback(orderNo, tradeNo, status, amount, payChannel);
            return Result.success("回调处理成功");
        } catch (Exception e) {
            log.error("支付回调处理失败", e);
            return Result.fail("回调处理失败：" + e.getMessage());
        }
    }

    @PostMapping("/pay/callback/mock")
    @Operation(summary = "支付回调(模拟)", description = "仅用于测试环境的模拟回调")
    public Result<String> payCallbackMock(
            @RequestParam("orderNo") String orderNo,
            @RequestParam("tradeNo") String tradeNo,
            @RequestParam("status") String status,
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            @RequestParam(value = "payChannel", required = false) String payChannel,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!mockPaymentEnabled) {
            return Result.fail("接口已关闭");
        }
        if (internalToken != null && !internalToken.isEmpty() && !internalToken.equals(token)) {
            return Result.fail("无权访问");
        }
        try {
            feeService.payCallback(orderNo, tradeNo, status, amount, payChannel);
            return Result.success("回调处理成功");
        } catch (Exception e) {
            return Result.fail("回调处理失败：" + e.getMessage());
        }
    }

    @PostMapping({"/remind/batch", "/admin/remind/batch"})
    @Operation(summary = "批量催缴", description = "管理员批量发送催缴通知")
    @Log(title = "批量催缴", businessType = BusinessType.UPDATE)
    public Result<String> remindBatch(@RequestBody Map<String, List<Long>> params) {
        List<Long> ids = params.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择需要催缴的账单");
        }
        boolean success = feeService.remind(ids);
        return success ? Result.success("催缴通知发送成功") : Result.fail("催缴通知发送失败");
    }

    @PostMapping({"/remind/{id}", "/admin/remind/{id}"})
    @Operation(summary = "单个催缴", description = "管理员对单个账单发送催缴通知")
    public Result<String> remindSingle(@PathVariable("id") Long id) {
        boolean success = feeService.remind(Collections.singletonList(id));
        return success ? Result.success("催缴通知发送成功") : Result.fail("催缴通知发送失败");
    }
}
