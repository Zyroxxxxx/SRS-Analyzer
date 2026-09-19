package com.vlu.srsanalyzer.entity;

public enum RequirementStatus {
    PENDING,    // Vua nhap, chua duoc AI phan tich
    ANALYZING,  // Dang goi AI xu ly
    ANALYZED,   // Da co ket qua phan tich
    FAILED      // Goi AI that bai
}
