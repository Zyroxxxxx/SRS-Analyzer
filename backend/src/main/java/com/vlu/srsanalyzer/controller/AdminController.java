package com.vlu.srsanalyzer.controller;

import com.vlu.srsanalyzer.dto.*;
import com.vlu.srsanalyzer.entity.*;
import com.vlu.srsanalyzer.repository.AiUsageRepository;
import com.vlu.srsanalyzer.repository.AuditLogRepository;
import com.vlu.srsanalyzer.repository.RequirementRepository;
import com.vlu.srsanalyzer.repository.UserRepository;
import com.vlu.srsanalyzer.security.AuthService;
import com.vlu.srsanalyzer.service.RequirementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService auth;
    private final UserRepository users;
    private final RequirementRepository reqs;
    private final AiUsageRepository usage;
    private final AuditLogRepository auditLogs;
    private final RequirementService requirementService;

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard(HttpServletRequest request) {
        requireAdmin(request);

        List<AdminUsageResponse> recent = usage.findRecent().stream()
                .limit(50)
                .map(this::toUsageResponse)
                .toList();

        long analyzed = reqs.countByStatus(RequirementStatus.ANALYZED);

        return new AdminDashboardResponse(
                users.count(),
                reqs.count(),
                analyzed,
                usage.totalTokens(),
                usage.totalRequests(),
                recent
        );
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(HttpServletRequest request) {
        requireAdmin(request);
        return users.findAll().stream().map(u -> new AdminUserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                u.getRole().name(),
                u.isRootAdminOrDefault(),
                u.isActiveOrDefault(),
                u.getTokenQuota(),
                reqs.countByUserId(u.getId()),
                usage.requestsByUser(u.getId()),
                usage.totalTokensByUser(u.getId()),
                u.getCreatedAt()
        )).toList();
    }

    @GetMapping("/users/{id}")
    @Transactional(readOnly = true)
    public AdminUserDetailResponse userDetail(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        User u = users.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user #" + id));

        List<RequirementResponse> requirementList = reqs.findByUserIdWithOwner(id).stream()
                .map(RequirementResponse::fromEntity)
                .toList();

        List<AdminUsageResponse> usageList = usage.findByUserIdOrderByCreatedAtDesc(id).stream()
                .map(this::toUsageResponse)
                .toList();

        return new AdminUserDetailResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole().name(),
                u.isRootAdminOrDefault(), u.isActiveOrDefault(), u.getAiPreference(), u.getTokenQuota(),
                usage.requestsByUser(id), usage.totalTokensByUser(id), u.getCreatedAt(),
                requirementList, usageList
        );
    }

    @PostMapping("/users")
    public AdminUserResponse createUser(@Valid @RequestBody AdminCreateUserRequest body, HttpServletRequest request) {
        requireAdmin(request);
        User created = auth.adminCreate(
                body.getUsername(),
                body.getEmail(),
                body.getPassword(),
                body.getFullName(),
                body.getRole() == null ? Role.USER : Role.valueOf(body.getRole().toUpperCase())
        );
        return toUserResponse(created);
    }

    @PutMapping("/users/{id}")
    @Transactional
    public AdminUserResponse updateUser(@PathVariable Long id, @RequestBody AdminUserUpdateRequest body, HttpServletRequest request) {
        User admin = requireAdmin(request);
        User u = users.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user #" + id));

        // Admin được phân quyền không được thay đổi hay vô hiệu hóa Admin gốc.
        // Admin gốc vẫn có thể quản lý tên/hạn mức của chính mình nhưng không thể tự hạ quyền/khóa.
        if (u.isRootAdminOrDefault() && !admin.isRootAdminOrDefault()) {
            throw new RuntimeException("Chỉ Admin gốc mới có thể quản lý tài khoản Admin gốc");
        }

        if (body.getFullName() != null) {
            String fullName = body.getFullName().trim();
            if (!fullName.isBlank()) u.setFullName(fullName);
        }
        if (body.getRole() != null) {
            Role newRole;
            try {
                newRole = Role.valueOf(body.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Vai trò không hợp lệ");
            }
            if (u.getId().equals(admin.getId()) && newRole != Role.ADMIN) {
                throw new RuntimeException("Không thể tự hạ quyền chính tài khoản đang đăng nhập");
            }
            if (u.getRole() == Role.ADMIN && newRole != Role.ADMIN && users.countByRole(Role.ADMIN) <= 1) {
                throw new RuntimeException("Hệ thống phải duy trì ít nhất một tài khoản quản trị viên");
            }
            u.setRole(newRole);
        }
        if (body.getActive() != null) {
            if (u.getId().equals(admin.getId()) && !body.getActive()) {
                throw new RuntimeException("Không thể tự khóa tài khoản đang đăng nhập");
            }
            u.setActive(body.getActive());
        }
        if (body.getTokenQuota() != null) {
            if (body.getTokenQuota() < 0) {
                throw new RuntimeException("Hạn mức token không được là số âm");
            }
            u.setTokenQuota(body.getTokenQuota() == 0 ? null : body.getTokenQuota());
        }

        return toUserResponse(users.save(u));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public void deleteUser(@PathVariable Long id, HttpServletRequest request) {
        User admin = requireAdmin(request);
        User target = users.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user #" + id));

        if (target.getId().equals(admin.getId())) {
            throw new RuntimeException("Không thể xóa tài khoản quản trị đang đăng nhập");
        }
        if (target.isRootAdminOrDefault()) {
            throw new RuntimeException("Không thể xóa Admin gốc của hệ thống");
        }

        // RequirementService handles all Requirement children in FK-safe order.
        List<Long> requirementIds = reqs.findByUserId(id).stream()
                .map(Requirement::getId)
                .toList();
        for (Long requirementId : requirementIds) {
            requirementService.delete(requirementId, admin);
        }

        // Remove remaining direct User children after all owned Requirements are gone.
        auth.deleteTokensForUser(id);
        usage.deleteByUserId(id);
        auditLogs.deleteByUserId(id);
        users.delete(target);
    }

    @GetMapping("/requirements")
    @Transactional(readOnly = true)
    public List<AdminRequirementResponse> listRequirements(HttpServletRequest request) {
        requireAdmin(request);
        return reqs.findAllWithOwner().stream().map(r -> {
            User owner = r.getUser();
            return new AdminRequirementResponse(
                    r.getId(), r.getTitle(), r.getStatus().name(),
                    owner == null ? "Không xác định" : owner.getUsername(),
                    owner == null ? "" : java.util.Objects.toString(owner.getFullName(), ""),
                    owner == null ? "" : java.util.Objects.toString(owner.getAiPreference(), ""),
                    r.getCreatedAt(), r.getUpdatedAt());
        }).toList();
    }

    @GetMapping("/audit")
    @Transactional(readOnly = true)
    public List<AdminAuditResponse> listAudit(HttpServletRequest request) {
        requireAdmin(request);
        return auditLogs.findRecentWithOwners().stream().limit(100).map(a -> new AdminAuditResponse(
                a.getId(),
                a.getUser() == null ? "Không xác định" : a.getUser().getUsername(),
                a.getRequirement() == null ? null : a.getRequirement().getId(),
                a.getRequirement() == null ? "" : a.getRequirement().getTitle(),
                a.getAction(),
                java.util.Objects.toString(a.getDetails(), ""),
                a.getCreatedAt()
        )).toList();
    }

    @DeleteMapping("/audit/{id}")
    @Transactional
    public void deleteAudit(@PathVariable Long id, HttpServletRequest request) {
        User admin = requireAdmin(request);
        if (!admin.isRootAdminOrDefault()) {
            throw new RuntimeException("Chỉ Admin gốc mới có quyền xóa nhật ký quản trị");
        }
        if (!auditLogs.existsById(id)) {
            throw new RuntimeException("Không tìm thấy nhật ký #" + id);
        }
        auditLogs.deleteById(id);
    }

    @DeleteMapping("/audit")
    @Transactional
    public void deleteAllAudit(HttpServletRequest request) {
        User admin = requireAdmin(request);
        if (!admin.isRootAdminOrDefault()) {
            throw new RuntimeException("Chỉ Admin gốc mới có quyền xóa nhật ký quản trị");
        }
        auditLogs.deleteAllInBatch();
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole().name(),
                user.isRootAdminOrDefault(), user.isActiveOrDefault(), user.getTokenQuota(),
                reqs.countByUserId(user.getId()), usage.requestsByUser(user.getId()), usage.totalTokensByUser(user.getId()), user.getCreatedAt()
        );
    }

    private AdminUsageResponse toUsageResponse(AiUsage a) {
        return new AdminUsageResponse(
                a.getId(),
                a.getUser().getUsername(),
                a.getRequirement().getId(),
                a.getRequirement().getTitle(),
                a.getPrompt(),
                a.getPromptTokens() == null ? 0 : a.getPromptTokens(),
                a.getOutputTokens() == null ? 0 : a.getOutputTokens(),
                a.getTotalTokens() == null ? 0 : a.getTotalTokens(),
                a.getProvider(),
                a.getCreatedAt()
        );
    }

    private User requireAdmin(HttpServletRequest request) {
        User u = auth.current(request);
        if (u.getRole() != Role.ADMIN) {
            throw new RuntimeException("Chỉ quản trị viên mới có quyền truy cập chức năng này");
        }
        return u;
    }
}
