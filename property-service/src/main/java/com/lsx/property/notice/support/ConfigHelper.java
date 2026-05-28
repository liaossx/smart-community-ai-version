package com.lsx.property.notice.support;

import com.lsx.core.common.Result.Result;
import com.lsx.property.client.SystemConfigClient;
import feign.FeignException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ConfigHelper {
    @Resource
    private SystemConfigClient systemConfigClient;

    public String get(String key) {
        try {
            Result<String> r = systemConfigClient.getValue(key);
            if (r == null) return null;
            return r.getData();
        } catch (FeignException e) {
            return null;
        }
    }

    public int getInt(String key, int def) {
        String v = get(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    public boolean getBool(String key, boolean def) {
        String v = get(key);
        if (v == null) return def;
        String s = v.trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s)) return true;
        if ("false".equals(s) || "0".equals(s) || "no".equals(s)) return false;
        return def;
    }
}
