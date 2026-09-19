package com.vlu.srsanalyzer.security;

import com.vlu.srsanalyzer.entity.AuthToken;
import com.vlu.srsanalyzer.entity.Role;
import com.vlu.srsanalyzer.entity.User;
import com.vlu.srsanalyzer.repository.AuthTokenRepository;
import com.vlu.srsanalyzer.exception.UnauthorizedException;
import com.vlu.srsanalyzer.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LoginResult login(String username, String password) {
        User u = users.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Sai tài khoản hoặc mật khẩu"));

        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new RuntimeException("Sai tài khoản hoặc mật khẩu");
        }
        if (!u.isActiveOrDefault()) {
            throw new RuntimeException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        tokens.save(new AuthToken(null, raw, u, LocalDateTime.now().plusHours(12), LocalDateTime.now()));
        return new LoginResult(raw, u);
    }

    // Dang ky tai khoan moi - mac dinh luon la Role.USER, khong cho tu phong ADMIN qua API cong khai
    public User register(String username, String email, String rawPassword, String fullName) {
        return createUser(username, email, rawPassword, fullName, Role.USER);
    }

    // Danh rieng cho Admin: duoc chon quyen ngay khi tao, khac voi dang ky cong khai
    public User adminCreate(String username, String email, String rawPassword, String fullName, Role role) {
        return createUser(username, email, rawPassword, fullName, role == null ? Role.USER : role);
    }

    private User createUser(String username, String email, String rawPassword, String fullName, Role role) {
        username = username == null ? "" : username.trim();
        email = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        fullName = fullName == null ? "" : fullName.trim();
        if (username.isBlank() || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new RuntimeException("Tài khoản, email và mật khẩu không được để trống");
        }
        if (users.existsByUsername(username)) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        if (users.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setFullName(fullName);
        u.setRole(role);
        u.setActive(true);
        return users.save(u);
    }

    public User current(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }
        AuthToken t = tokens.findByTokenWithUser(h.substring(7))
                .orElseThrow(() -> new RuntimeException("Phiên đăng nhập không hợp lệ"));
        if (t.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Phiên đăng nhập đã hết hạn");
        }
        User u = t.getUser();
        if (!u.isActiveOrDefault()) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }
        return u;
    }

    @Transactional
    public void deleteTokensForUser(Long userId) {
        tokens.deleteByUserId(userId);
    }

    @Transactional
    public void logout(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            tokens.deleteByToken(h.substring(7));
        }
    }

    public String encode(String raw) {
        return encoder.encode(raw);
    }

    public record LoginResult(String token, User user) {}
}
