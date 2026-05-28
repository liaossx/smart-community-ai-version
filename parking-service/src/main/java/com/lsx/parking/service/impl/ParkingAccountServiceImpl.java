package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.parking.entity.ParkingAccount;
import com.lsx.parking.entity.ParkingAccountLog;
import com.lsx.parking.mapper.ParkingAccountLogMapper;
import com.lsx.parking.mapper.ParkingAccountMapper;
import com.lsx.parking.service.ParkingAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ParkingAccountServiceImpl implements ParkingAccountService {

    private final ParkingAccountMapper accountMapper;
    private final ParkingAccountLogMapper logMapper;

    @Override
    @Transactional
    public ParkingAccount getOrCreateAccount(Long userId) {
        ParkingAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<ParkingAccount>()
                        .eq(ParkingAccount::getUserId, userId)
        );

        if (account != null) {
            return account;
        }

        // 没有账户就创建
        account = new ParkingAccount();
        account.setUserId(userId);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus("NORMAL");
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());

        accountMapper.insert(account);
        return account;
    }

    @Override
    @Transactional
    public void recharge(Long userId, BigDecimal amount) {
        ParkingAccount account = getOrCreateAccount(userId);

        account.setBalance(account.getBalance().add(amount));
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);

        ParkingAccountLog log = new ParkingAccountLog();
        log.setAccountId(account.getId());
        log.setAmount(amount);
        log.setType("RECHARGE");
        log.setRemark("账户充值");
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    @Override
    @Transactional
    public void consume(Long userId, BigDecimal amount, Long orderId) {
        ParkingAccount account = getOrCreateAccount(userId);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }

        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);

        ParkingAccountLog log = new ParkingAccountLog();
        log.setAccountId(account.getId());
        log.setOrderId(orderId);
        log.setAmount(amount.negate());
        log.setType("CONSUME");
        log.setRemark("停车消费");
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }
}
