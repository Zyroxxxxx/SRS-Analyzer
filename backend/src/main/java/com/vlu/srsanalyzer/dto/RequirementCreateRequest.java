package com.vlu.srsanalyzer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequirementCreateRequest {

    @NotBlank(message = "Tieu de khong duoc de trong")
    @Size(max = 255, message = "Tieu de toi da 255 ky tu")
    private String title;

    @NotBlank(message = "Mo ta yeu cau khong duoc de trong")
    private String rawDescription;
}
