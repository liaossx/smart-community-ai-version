package com.lsx.user.dto.external;

import lombok.Data;
import java.io.Serializable;

@Data
public class HouseDTO implements Serializable {
    private Long id;
    private String communityName;
    private String buildingNo;
    private String houseNo;
}
