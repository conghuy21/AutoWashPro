package com.autowashpro.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedbackResponse {
    private Integer feedbackId;
    private Integer bookingId;
    private Integer customerId;
    private String customerName;
    private String licensePlate;
    private Integer rating;
    private String comment;
    private Boolean isVisible;
    private LocalDateTime createdAt;
}