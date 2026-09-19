package com.vlu.srsanalyzer.dto; import lombok.*; import java.util.*;
@Data @AllArgsConstructor public class AdminDashboardResponse {private long totalUsers; private long totalRequirements; private long analyzedRequirements; private long totalTokens; private long totalAiRequests; private List<AdminUsageResponse> recentUsage;}
