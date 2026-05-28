package com.lsx.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.system.entity.SysConfig;

public interface SysConfigService extends IService<SysConfig> {
    
    String getValue(String key);
    
    String getValue(String key, String defaultValue);
}
