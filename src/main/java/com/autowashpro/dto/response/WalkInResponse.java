package com.autowashpro.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalkInResponse {
    private Integer walkInId;
    private String fullName;
    private String phone;
    private String licensePlate;
    private String vehicleType;
    private String laneName;
    private String employeeName;
    private String status;
    private BigDecimal serviceTotal;
    private String paymentMethod;
    private String paymentStatus;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}