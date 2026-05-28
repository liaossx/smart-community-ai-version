package com.lsx.property.visitor.dto;

import com.lsx.property.visitor.entity.SysVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VisitorDTO extends SysVisitor {
    private String ownerName;
    private String buildingNo;
    private String houseNo;
}
