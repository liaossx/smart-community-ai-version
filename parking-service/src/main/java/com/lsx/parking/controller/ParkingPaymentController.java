package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.PaymentSignUtil;
import com.lsx.parking.dto.ParkingPaySuccessDTO;
import com.lsx.parking.service.ParkingPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/parking/pay")
@Tag(name = "停车-支付")
public class ParkingPaymentController {

    @Autowired
    private ParkingPaymentService parkingPaymentService;

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

    @PostMapping("/callback")
    @Operation(summary = "支付回调", description = "支付渠道异步通知回调")
    public Result<String> payCallback(@RequestParam("orderNo") String orderNo,
                                      @RequestParam("tradeNo") String tradeNo,
                                      @RequestParam("status") String status,
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
        String nonceKey = "pay:nonce:parking:" + nonce;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(nonceKey, "1", Duration.ofSeconds(paymentAllowedSkewSeconds * 2));
        if (ok == null || !ok) {
            return Result.fail("重复回调");
        }
        Map<String, String> params = new HashMap<>();
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
            parkingPaymentService.payCallback(orderNo, tradeNo, status, amount, payChannel);
            return Result.success("回调处理成功");
        } catch (Exception e) {
            return Result.fail("回调处理失败：" + e.getMessage());
        }
    }

    @PostMapping("/success")
    @Operation(summary = "支付成功回调（模拟）")
    public Result<Void> paySuccess(@RequestBody ParkingPaySuccessDTO dto,
                                   @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!mockPaymentEnabled) {
            return Result.fail("接口已关闭");
        }
        if (internalToken != null && !internalToken.isEmpty() && !internalToken.equals(token)) {
            return Result.fail("无权访问");
        }
        try {
            parkingPaymentService.paySuccess(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("支付回调异常", e);
            return Result.fail("支付处理失败");
        }
    }
}
