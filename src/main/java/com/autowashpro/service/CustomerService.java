package com.autowashpro.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.autowashpro.dto.request.UpdateProfileRequest;
import com.autowashpro.dto.response.CustomerDTO;
import com.autowashpro.entity.User;
import com.autowashpro.repository.CustomerRepository;
import com.autowashpro.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerDTO getMyProfile(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return customerRepository.findByUser_UserId(user.getUserId())
                .map(CustomerDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public Map<String, Object> updateProfile(String email, UpdateProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        if (!req.getPhone().matches("^[0-9]{9,11}$")) {
            throw new RuntimeException("Số điện thoại phải có từ 9 đến 11 chữ số");
        }

        if (!user.getPhone().equals(req.getPhone()) &&
                userRepository.existsByPhone(req.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng bởi tài khoản khác!");
        }

        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        userRepository.save(user);

        return Map.of("message", "Cập nhật thông tin thành công!");
    }

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(CustomerDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public CustomerDTO getCustomerById(Integer id) {
        return customerRepository.findById(id)
                .map(CustomerDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }
}
