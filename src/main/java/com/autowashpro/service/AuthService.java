package com.autowashpro.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autowashpro.dto.request.LoginRequest;
import com.autowashpro.dto.request.RegisterRequest;
import com.autowashpro.dto.response.UserDTO;
import com.autowashpro.entity.PasswordReset;
import com.autowashpro.entity.User;
import com.autowashpro.repository.PasswordResetRepository;
import com.autowashpro.repository.UserRepository;
import com.autowashpro.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetRepository passwordResetRepository;
    private final EmailService emailService;

    public Map<String, Object> register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng!");
        }

        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.Status.ACTIVE);
        userRepository.save(user);

        return Map.of(
                "message", "Đăng ký thành công!",
                "user", UserDTO.fromEntity(user));
    }

    public Map<String, Object> login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng!"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng!");
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new RuntimeException("Tài khoản đã bị khóa!");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return Map.of(
                "token", token,
                "user", UserDTO.fromEntity(user));
    }

    public UserDTO getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDTO.fromEntity(user);
    }

    /* ===== QUÊN MẬT KHẨU ===== */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        // Xóa token cũ nếu có
        passwordResetRepository.deleteByUser_UserId(user.getUserId());

        // Tạo token mới
        String token = java.util.UUID.randomUUID().toString();

        PasswordReset reset = new PasswordReset();
        reset.setUser(user);
        reset.setToken(token);
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        reset.setUsed(false);
        passwordResetRepository.save(reset);

        // Gửi email
        emailService.sendResetPasswordEmail(email, token);
    }

    /* ===== ĐẶT LẠI MẬT KHẨU ===== */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordReset reset = passwordResetRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ hoặc đã hết hạn!"));

        if (reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn! Vui lòng yêu cầu lại.");
        }

        User user = reset.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        reset.setUsed(true);
        passwordResetRepository.save(reset);
    }

    /* ===== ĐỔI MẬT KHẨU ===== */
    @Transactional
    public Map<String, Object> changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng!");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại!");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return Map.of("message", "Đổi mật khẩu thành công!");
    }

}
