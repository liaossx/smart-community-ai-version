package com.lsx.user.dto.external;

import lombok.Data;
import java.io.Serializable;

@Data
public class VehicleDTO implements Serializable {
    private Long id;
    private String plateNo;
    private Integer status;
}
