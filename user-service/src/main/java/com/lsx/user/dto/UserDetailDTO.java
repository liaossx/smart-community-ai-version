package com.lsx.user.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserDetailDTO extends UserDTO {
    private List<BoundHouse> houses;
    private List<BoundVehicle> vehicles;

    @Data
    public static class BoundHouse {
        private Long houseId;
        private String communityName;
        private String buildingNo;
        private String houseNo;
    }

    @Data
    public static class BoundVehicle {
        private Long vehicleId;
        private String plateNo;
        private Integer status;
    }
}

