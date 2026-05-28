package com.lsx.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.core.common.Result.Result;
import com.lsx.system.config.ConfigMeta;
import com.lsx.system.config.ConfigRegistry;
import com.lsx.system.dto.ConfigEffectiveDTO;
import com.lsx.system.entity.SysConfig;
import com.lsx.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/config")
@Tag(name = "系统参数配置接口")
public class SysConfigController {

    @Resource
    private SysConfigService configService;

    @Value("${internal.token:}")
    private String internalToken;

    @GetMapping("/list")
    @Operation(summary = "根据参数键名查询参数值")
    public Result<String> getConfigValue(@RequestParam("configKey") String configKey) {
        String value = configService.getValue(configKey);
        return Result.success(value);
    }

    @GetMapping("/inner/list")
    @Operation(summary = "内部根据参数键名查询参数值")
    public Result<String> getConfigValueInner(
            @RequestParam("configKey") String configKey,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (internalToken != null && !internalToken.isEmpty() && !internalToken.equals(token)) {
            return Result.fail("无权访问");
        }
        String value = configService.getValue(configKey);
        return Result.success(value);
    }
    
    @GetMapping("/all")
    @Operation(summary = "获取所有配置")
    public Result<List<SysConfig>> listAll() {
        return Result.success(configService.list());
    }

    @GetMapping("/registry")
    @Operation(summary = "配置清单与生效值")
    public Result<List<ConfigEffectiveDTO>> registry() {
        List<ConfigMeta> metas = ConfigRegistry.all();
        List<SysConfig> dbConfigs = configService.list();
        Map<String, String> dbMap = new HashMap<>();
        for (SysConfig c : dbConfigs) {
            if (c.getConfigKey() != null) {
                dbMap.put(c.getConfigKey(), c.getConfigValue());
            }
        }

        Map<String, ConfigEffectiveDTO> outMap = new HashMap<>();
        for (ConfigMeta m : metas) {
            String db = dbMap.get(m.getKey());
            ConfigEffectiveDTO dto = new ConfigEffectiveDTO();
            dto.setKey(m.getKey());
            dto.setType(m.getType());
            dto.setDefaultValue(m.getDefaultValue());
            dto.setValue(db);
            dto.setEffectiveValue(db != null ? db : m.getDefaultValue());
            dto.setModule(m.getModule());
            dto.setDescription(m.getDescription());
            outMap.put(m.getKey(), dto);
        }

        for (SysConfig c : dbConfigs) {
            String key = c.getConfigKey();
            if (key == null) continue;
            if (outMap.containsKey(key)) continue;
            ConfigEffectiveDTO dto = new ConfigEffectiveDTO();
            dto.setKey(key);
            dto.setType("string");
            dto.setDefaultValue(null);
            dto.setValue(c.getConfigValue());
            dto.setEffectiveValue(c.getConfigValue());
            dto.setModule("db-only");
            dto.setDescription("未注册配置项");
            outMap.put(key, dto);
        }

        return Result.success(new java.util.ArrayList<>(outMap.values()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取配置详情")
    public Result<SysConfig> getById(@PathVariable("id") Long id) {
        return Result.success(configService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增配置")
    public Result<Boolean> add(@RequestBody SysConfig config) {
        return Result.success(configService.save(config));
    }

    @PutMapping
    @Operation(summary = "修改配置")
    public Result<Boolean> update(@RequestBody SysConfig config) {
        return Result.success(configService.updateById(config));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        return Result.success(configService.removeById(id));
    }
}
