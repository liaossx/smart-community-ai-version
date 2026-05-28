package com.lsx.property.express.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.property.express.dto.ExpressAuthorizeDTO;
import com.lsx.property.express.dto.ExpressCreateDTO;
import com.lsx.property.express.dto.ExpressPickDTO;
import com.lsx.property.express.entity.Express;
import com.lsx.property.express.mapper.ExpressMapper;
import com.lsx.property.express.service.ExpressService;
import com.lsx.property.express.vo.ExpressVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ExpressServiceImpl extends ServiceImpl<ExpressMapper, Express> implements ExpressService {

    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_PICKED = "PICKED";

    @Override
    public Long registerExpress(ExpressCreateDTO dto) {
        Assert.notNull(dto.getUserId(), "业主ID不能为空");
        Assert.isTrue(StringUtils.hasText(dto.getRecipientName()), "收件人姓名不能为空");
        Assert.isTrue(StringUtils.hasText(dto.getRecipientPhone()), "收件人电话不能为空");
        Assert.isTrue(StringUtils.hasText(dto.getCompany()), "快递公司不能为空");
        Assert.isTrue(StringUtils.hasText(dto.getTrackingNo()), "运单号不能为空");

        Express express = new Express();
        BeanUtil.copyProperties(dto, express);
        express.setStatus(STATUS_WAITING);
        express.setAuthorized(Boolean.FALSE);
        express.setLocationCode(generateLocationCode());
        express.setPickupCode(RandomUtil.randomNumbers(6));
        LocalDateTime now = LocalDateTime.now();
        express.setCreateTime(now);
        express.setUpdateTime(now);
        baseMapper.insert(express);
        return express.getId();
    }

    @Override
    public Page<ExpressVO> listMyExpress(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Page<Express> page = new Page<>(pageNum, pageSize);
        Page<Express> expressPage = baseMapper.selectPage(page, Wrappers.<Express>lambdaQuery()
                .eq(Express::getUserId, userId)
                .orderByDesc(Express::getCreateTime));
        // 强制转换为Page<ExpressVO>
        return (Page<ExpressVO>) expressPage.convert(this::convertToVO);
    }

    @Override
    public Boolean pickExpress(Long expressId, ExpressPickDTO dto) {
        Assert.notNull(expressId, "快递ID不能为空");
        Assert.notNull(dto.getUserId(), "用户ID不能为空");
        Express express = baseMapper.selectById(expressId);
        if (express == null) {
            throw new RuntimeException("快递不存在");
        }
        boolean isOwner = express.getUserId().equals(dto.getUserId());
        boolean byAuthorized = Boolean.TRUE.equals(dto.getByAuthorized());
        if (!byAuthorized) {
            if (!isOwner) {
                throw new RuntimeException("无权操作该快递");
            }
        } else {
            // 授权取件需要满足一系列校验
            if (!isOwner) {
                throw new RuntimeException("授权信息与业主不匹配");
            }
            if (!Boolean.TRUE.equals(express.getAuthorized())) {
                throw new RuntimeException("当前快递未设置授权，无法代取");
            }
            if (express.getAuthorizeExpireTime() == null || express.getAuthorizeExpireTime().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("授权已过期，请重新授权");
            }
            if (!StringUtils.hasText(dto.getOperatorPhone()) || !dto.getOperatorPhone().equals(express.getAuthorizedPhone())) {
                throw new RuntimeException("授权人手机号不匹配，拒绝取件");
            }
            if (StringUtils.hasText(express.getAuthorizedName()) && StringUtils.hasText(dto.getOperatorName())
                    && !express.getAuthorizedName().equals(dto.getOperatorName())) {
                throw new RuntimeException("授权人姓名不匹配，拒绝取件");
            }
        }
        if (!STATUS_WAITING.equals(express.getStatus())) {
            throw new RuntimeException("快递已被取走或已失效");
        }
        if (!express.getPickupCode().equals(dto.getPickupCode())) {
            throw new RuntimeException("取件码不正确");
        }
        express.setStatus(STATUS_PICKED);
        express.setPickupTime(LocalDateTime.now());
        if (StringUtils.hasText(dto.getRemark())) {
            express.setRemark(dto.getRemark());
        } else if (byAuthorized) {
            express.setRemark("授权人代取：" + dto.getOperatorName());
        } else {
            express.setRemark(null);
        }
        express.setUpdateTime(LocalDateTime.now());
        express.setAuthorized(Boolean.FALSE);
        express.setAuthorizedName(null);
        express.setAuthorizedPhone(null);
        express.setAuthorizeExpireTime(null);
        return baseMapper.updateById(express) > 0;
    }

    @Override
    public Boolean authorizeExpress(Long expressId, ExpressAuthorizeDTO dto) {
        Assert.notNull(expressId, "快递ID不能为空");
        Assert.notNull(dto.getUserId(), "业主ID不能为空");
        Express express = baseMapper.selectById(expressId);
        if (express == null) {
            throw new RuntimeException("快递不存在");
        }
        if (!express.getUserId().equals(dto.getUserId())) {
            throw new RuntimeException("无权授权该快递");
        }
        if (!STATUS_WAITING.equals(express.getStatus())) {
            throw new RuntimeException("当前状态不可授权");
        }
        express.setAuthorized(Boolean.TRUE);
        express.setAuthorizedName(dto.getAuthorizedName());
        express.setAuthorizedPhone(dto.getAuthorizedPhone());
        LocalDateTime expire = dto.getExpireTime() != null ? dto.getExpireTime() : LocalDateTime.now().plusHours(24);
        express.setAuthorizeExpireTime(expire);
        express.setUpdateTime(LocalDateTime.now());
        return baseMapper.updateById(express) > 0;
    }

    private ExpressVO convertToVO(Express express) {
        ExpressVO vo = new ExpressVO();
        BeanUtil.copyProperties(express, vo);
        return vo;
    }

    private String generateLocationCode() {
        return "L" + RandomUtil.randomStringUpper(3);
    }
}
