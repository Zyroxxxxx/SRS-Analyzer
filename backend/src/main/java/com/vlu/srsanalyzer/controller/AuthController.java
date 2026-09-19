package com.vlu.srsanalyzer.controller;

import com.vlu.srsanalyzer.dto.LoginRequest;
import com.vlu.srsanalyzer.dto.LoginResponse;
import com.vlu.srsanalyzer.dto.PreferenceRequest;
import com.vlu.srsanalyzer.dto.RegisterRequest;
import com.vlu.srsanalyzer.entity.User;
import com.vlu.srsanalyzer.repository.UserRepository;
import com.vlu.srsanalyzer.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest r) {
        var result = auth.login(r.getUsername(), r.getPassword());
        User u = result.user();
        return ResponseEntity.ok(toResponse(result.token(), u));
    }

    // Dang ky tai khoan moi cho user - khong can dang nhap truoc
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest r) {
        User u = auth.register(r.getUsername(), r.getEmail(), r.getPassword(), r.getFullName());
        // Dang ky xong tu dong dang nhap luon cho tien, tra ve token moi
        var loginResult = auth.login(r.getUsername(), r.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(loginResult.token(), u));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest r) {
        auth.logout(r);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(HttpServletRequest r) {
        User u = auth.current(r);
        return ResponseEntity.ok(toResponse("", u));
    }

    @PutMapping("/preference")
    public ResponseEntity<LoginResponse> preference(@RequestBody PreferenceRequest p, HttpServletRequest r) {
        User u = auth.current(r);
        u.setAiPreference(p.getAiPreference());
        if (p.getAnalysisLanguage() != null && !p.getAnalysisLanguage().isBlank()) {
            String language = p.getAnalysisLanguage().trim().toUpperCase();
            if (!language.equals("EN") && !language.equals("VI")) {
                throw new IllegalArgumentException("Ngôn ngữ phân tích không hợp lệ");
            }
            u.setAnalysisLanguage(language);
        }
        userRepository.save(u); // Luu tuong minh, khong dua vao dirty-checking ngam
        return ResponseEntity.ok(toResponse("", u));
    }


    private LoginResponse toResponse(String token, User u) {
        return new LoginResponse(token, u.getId(), u.getUsername(), u.getFullName(), u.getRole().name(), u.getAiPreference(), u.getAnalysisLanguageOrDefault(), u.isRootAdminOrDefault());
    }
}
