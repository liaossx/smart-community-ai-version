package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.parking.dto.RechargeDTO;
import com.lsx.parking.entity.ParkingAccount;
import com.lsx.parking.service.ParkingAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/parking/account")
@Tag(name = "停车账户接口")
@RequiredArgsConstructor
public class ParkingAccountController {

    private final ParkingAccountService accountService;
    @Operation(summary = "充值停车账户")
    @PostMapping("/recharge")
    public Result<Void> recharge(@RequestBody RechargeDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        accountService.recharge(userId, dto.getAmount());
        return Result.success();
    }
    @Operation(summary = "查询停车账户余额")
    @GetMapping("/balance")
    public Result<BigDecimal> balance() {
        Long userId = UserContext.getCurrentUserId();
        ParkingAccount account = accountService.getOrCreateAccount(userId);
        return Result.success(account.getBalance());
    }
}
