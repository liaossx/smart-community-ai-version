package com.lsx.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.system.entity.SysConfig;
import com.lsx.system.mapper.SysConfigMapper;
import com.lsx.system.service.SysConfigService;
import org.springframework.stereotype.Service;

@Service
public class SysConfigImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public String getValue(String key) {
        return getValue(key, null);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        SysConfig config = this.getOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config != null) {
            return config.getConfigValue();
        }
        return defaultValue;
    }
}
