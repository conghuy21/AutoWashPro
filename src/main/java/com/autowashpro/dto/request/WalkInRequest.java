package com.autowashpro.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WalkInRequest {

    private String fullName;
    private String phone;
    private String licensePlate;

    @NotBlank(message = "Loại xe không được trống")
    private String vehicleType; // MOTORBIKE, CAR

    private BigDecimal serviceTotal;
    private String paymentMethod;
    private String note;
    private Integer laneId;
}