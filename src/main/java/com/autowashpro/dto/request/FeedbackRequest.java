package com.autowashpro.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FeedbackRequest {

    @NotNull(message = "BookingID không được trống")
    private Integer bookingId;

    @NotNull(message = "Rating không được trống")
    @Min(value = 1, message = "Rating tối thiểu là 1")
    @Max(value = 5, message = "Rating tối đa là 5")
    private Integer rating;

    private String comment;
}