package com.lsx.system.dto;

import lombok.Data;

@Data
public class ConfigEffectiveDTO {
    private String key;
    private String type;
    private String defaultValue;
    private String value;
    private String effectiveValue;
    private String module;
    private String description;
}
