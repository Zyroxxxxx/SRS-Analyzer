package com.vlu.srsanalyzer.dto;

import com.vlu.srsanalyzer.entity.Requirement;
import com.vlu.srsanalyzer.entity.RequirementStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequirementResponse {

    private Long id;
    private String title;
    private String rawDescription;
    private RequirementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ownerUsername; // De hien thi "yeu cau cua ai" tren giao dien, nhat la khi Admin xem toan bo

    public static RequirementResponse fromEntity(Requirement r) {
        return new RequirementResponse(
                r.getId(),
                r.getTitle(),
                r.getRawDescription(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getUser() != null ? r.getUser().getUsername() : null
        );
    }
}
