package com.lsx.system.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigMeta {
    private String key;
    private String type;
    private String defaultValue;
    private String module;
    private String description;
}
