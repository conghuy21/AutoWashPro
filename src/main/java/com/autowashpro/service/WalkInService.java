package com.autowashpro.service;

import com.autowashpro.dto.request.WalkInRequest;
import com.autowashpro.dto.response.WalkInResponse;
import com.autowashpro.entity.*;
import com.autowashpro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalkInService {

    private final WalkInRepository walkInRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceLaneRepository serviceLaneRepository;
    private final UserRepository userRepository;

    /* ===== STAFF TẠO KHÁCH VÃNG LAI ===== */
    @Transactional
    public WalkInResponse createWalkIn(String email, WalkInRequest req) {
        Employee employee = getEmployeeByEmail(email);

        WalkInCustomer walkIn = new WalkInCustomer();
        walkIn.setFullName(req.getFullName());
        walkIn.setPhone(req.getPhone());
        walkIn.setLicensePlate(req.getLicensePlate());
        walkIn.setVehicleType(req.getVehicleType());
        walkIn.setServiceTotal(req.getServiceTotal());
        walkIn.setPaymentMethod(req.getPaymentMethod());
        walkIn.setNote(req.getNote());
        walkIn.setEmployee(employee);
        walkIn.setStatus("WAITING");
        walkIn.setPaymentStatus("PENDING");

        // Gán làn nếu có
        if (req.getLaneId() != null) {
            ServiceLane lane = serviceLaneRepository.findById(req.getLaneId())
                    .orElseThrow(() -> new RuntimeException("Lane not found"));
            walkIn.setLane(lane);
            lane.setIsAvailable(false);
            serviceLaneRepository.save(lane);
        }

        return toResponse(walkInRepository.save(walkIn));
    }

    /* ===== XEM TẤT CẢ ===== */
    public List<WalkInResponse> getAll() {
        return walkInRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /* ===== XEM HÔM NAY ===== */
    public List<WalkInResponse> getToday() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);
        return walkInRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(start, end)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /* ===== CẬP NHẬT TRẠNG THÁI ===== */
    @Transactional
    public WalkInResponse updateStatus(Integer walkInId, String status, String paymentStatus) {
        WalkInCustomer walkIn = walkInRepository.findById(walkInId)
                .orElseThrow(() -> new RuntimeException("WalkIn not found"));

        walkIn.setStatus(status);

        if (paymentStatus != null) {
            walkIn.setPaymentStatus(paymentStatus);
        }

        if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
            walkIn.setCompletedAt(LocalDateTime.now());
            // Giải phóng làn
            if (walkIn.getLane() != null) {
                ServiceLane lane = walkIn.getLane();
                lane.setIsAvailable(true);
                serviceLaneRepository.save(lane);
            }
        }

        return toResponse(walkInRepository.save(walkIn));
    }

    /* ===== XEM LÀNG TRỐNG ===== */
    public List<ServiceLane> getAvailableLanes() {
        return serviceLaneRepository.findByIsAvailableTrue();
    }

    // ===== HELPERS =====
    private Employee getEmployeeByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return employeeRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    private WalkInResponse toResponse(WalkInCustomer w) {
        return WalkInResponse.builder()
                .walkInId(w.getWalkInId())
                .fullName(w.getFullName())
                .phone(w.getPhone())
                .licensePlate(w.getLicensePlate())
                .vehicleType(w.getVehicleType())
                .laneName(w.getLane() != null ? w.getLane().getName() : null)
                .employeeName(w.getEmployee() != null
                        ? w.getEmployee().getUser().getFullName() : null)
                .status(w.getStatus())
                .serviceTotal(w.getServiceTotal())
                .paymentMethod(w.getPaymentMethod())
                .paymentStatus(w.getPaymentStatus())
                .note(w.getNote())
                .createdAt(w.getCreatedAt())
                .completedAt(w.getCompletedAt())
                .build();
    }
}