package com.autowashpro.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ServiceLanes")
public class ServiceLane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LaneID")
    private Integer laneId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "lane_type", nullable = false, length = 10)
    private String laneType;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}