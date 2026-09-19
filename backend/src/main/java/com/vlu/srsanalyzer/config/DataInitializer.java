package com.vlu.srsanalyzer.config;

import com.vlu.srsanalyzer.entity.Role;
import com.vlu.srsanalyzer.entity.User;
import com.vlu.srsanalyzer.repository.UserRepository;
import com.vlu.srsanalyzer.security.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Tao san vai tai khoan demo khi CSDL con trong, de tien chay thu he thong ma khong can dang ky truoc
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository users;
    private final AuthService auth;

    @Bean
    CommandLineRunner seed() {
        return args -> {
            // Bao dam luon co dung mot Admin goc khi khoi dong.
            // Tai khoan demo "admin" la Admin goc; neu CSDL cu khong con tai khoan nay,
            // chon Admin dau tien lam Admin goc de khong mat co che bao ve.
            if (!users.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@company.local");
                admin.setPasswordHash(auth.encode("admin123"));
                admin.setFullName("System Administrator");
                admin.setRole(Role.ADMIN);
                admin.setRootAdmin(true);
                admin.setAiPreference("Formal, concise, enterprise-oriented SRS analysis");
                admin.setActive(true);
                admin.setTokenQuota(null);
                users.save(admin);
            }

            if (users.countByRootAdminTrue() == 0) {
                User root = users.findByUsername("admin")
                        .filter(u -> u.getRole() == Role.ADMIN)
                        .orElseGet(() -> users.findFirstByRoleOrderByIdAsc(Role.ADMIN).orElse(null));
                if (root != null) {
                    root.setRootAdmin(true);
                    users.save(root);
                }
            }

            if (!users.existsByUsername("user")) {
                User user = new User();
                user.setUsername("user");
                user.setEmail("user@company.local");
                user.setPasswordHash(auth.encode("user123"));
                user.setFullName("Project Analyst");
                user.setRole(Role.USER);
                user.setAiPreference("Clear, practical, testable requirements");
                user.setActive(true);
                user.setTokenQuota(200000); // Demo han muc token de minh hoa tinh nang quota
                users.save(user);
            }

            // Them 1 user demo thu hai de trang quan tri co du lieu so sanh giua nhieu nguoi dung
            if (!users.existsByUsername("analyst2")) {
                User user2 = new User();
                user2.setUsername("analyst2");
                user2.setEmail("analyst2@company.local");
                user2.setPasswordHash(auth.encode("analyst123"));
                user2.setFullName("Nguyen Van Phan Tich");
                user2.setRole(Role.USER);
                user2.setAiPreference("Ngan gon, uu tien bao mat va kha nang kiem thu");
                user2.setActive(true);
                user2.setTokenQuota(50000);
                users.save(user2);
            }
        };
    }
}
