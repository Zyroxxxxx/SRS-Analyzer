package com.vlu.srsanalyzer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlu.srsanalyzer.entity.*;
import com.vlu.srsanalyzer.repository.*;
import com.vlu.srsanalyzer.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ExportController {

    private final RequirementRepository requirements;
    private final AnalysisResultRepository results;
    private final UserStoryRepository stories;
    private final AcceptanceCriteriaRepository criteria;
    private final AiUsageRepository usage;
    private final AuthService auth;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ================== Xuat HTML (mo bang trinh duyet -> In -> Luu thanh PDF) ==================

    @GetMapping(value = "/requirements/{id}/srs", produces = "text/html;charset=UTF-8")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> srsHtml(@PathVariable Long id, HttpServletRequest req) {
        User u = auth.current(req);
        Requirement r = loadOwned(id, u);
        AnalysisResult a = results.findByRequirementId(id).orElse(null);
        String html = buildHtml(r, a);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=REQ-" + id + "-SRS.html")
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(html.getBytes(StandardCharsets.UTF_8));
    }

    // ================== Xuat Excel that (.xlsx) cho 1 yeu cau, gom ca Ma tran truy vet ==================

    @GetMapping("/requirements/{id}/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> srsExcel(@PathVariable Long id, HttpServletRequest req) throws IOException {
        User u = auth.current(req);
        Requirement r = loadOwned(id, u);
        AnalysisResult a = results.findByRequirementId(id).orElse(null);
        List<UserStory> storyList = stories.findByRequirementId(id);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(wb);

            writeOverviewSheet(wb, headerStyle, r, a);
            writeJsonListSheet(wb, headerStyle, "Yeu cau chuc nang (FR)", a == null ? null : a.getFunctionalRequirements());
            writeJsonListSheet(wb, headerStyle, "Yeu cau phi chuc nang (NFR)", a == null ? null : a.getNonFunctionalRequirements());
            writeUserStorySheet(wb, headerStyle, storyList);
            writeTraceabilitySheet(wb, headerStyle, r, storyList);
            writeJsonListSheet(wb, headerStyle, "Diem chua ro", a == null ? null : a.getAmbiguousNotes());

            return toResponse(wb, "REQ-" + id + "-SRS.xlsx");
        }
    }

    // ================== Bao cao Excel toan he thong cho Admin: prompt + token cua tung user ==================

    @GetMapping("/admin/usage-report")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> adminUsageReport(HttpServletRequest req) throws IOException {
        User u = auth.current(req);
        if (u.getRole() != Role.ADMIN) {
            throw new RuntimeException("Chi quan tri vien moi co quyen xuat bao cao nay");
        }

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(wb);
            Sheet sheet = wb.createSheet("Lich su su dung AI");
            String[] cols = {"ID", "Nguoi dung", "Ma yeu cau", "Tieu de yeu cau", "Prompt gui AI", "Token dau vao", "Token dau ra", "Tong token", "Nha cung cap", "Thoi gian"};
            writeHeaderRow(sheet, headerStyle, cols);

            int rowIdx = 1;
            for (AiUsage a : usage.findRecent()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getId());
                row.createCell(1).setCellValue(a.getUser().getUsername());
                row.createCell(2).setCellValue("REQ-" + String.format("%04d", a.getRequirement().getId()));
                row.createCell(3).setCellValue(a.getRequirement().getTitle());
                row.createCell(4).setCellValue(a.getPrompt());
                row.createCell(5).setCellValue(a.getPromptTokens());
                row.createCell(6).setCellValue(a.getOutputTokens());
                row.createCell(7).setCellValue(a.getTotalTokens());
                row.createCell(8).setCellValue(a.getProvider());
                row.createCell(9).setCellValue(a.getCreatedAt() == null ? "" : a.getCreatedAt().format(DATE_FMT));
            }
            autoSize(sheet, cols.length);
            return toResponse(wb, "bao-cao-su-dung-AI.xlsx");
        }
    }

    // ================== Helper: quyen truy cap ==================

    private Requirement loadOwned(Long id, User u) {
        Requirement r = requirements.findByIdWithOwner(id).orElseThrow(() -> new RuntimeException("Khong tim thay yeu cau."));
        if (u.getRole() != Role.ADMIN && (r.getUser() == null || !r.getUser().getId().equals(u.getId()))) {
            throw new RuntimeException("Khong co quyen truy cap yeu cau nay");
        }
        return r;
    }

    // ================== Helper: Excel ==================

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeHeaderRow(Sheet sheet, CellStyle style, String[] cols) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    private void writeOverviewSheet(Workbook wb, CellStyle headerStyle, Requirement r, AnalysisResult a) {
        Sheet sheet = wb.createSheet("Thong tin yeu cau");
        String[] labels = {"Ma yeu cau", "Tieu de", "Mo ta goc", "Trang thai", "Ngay tao", "Tom tat phan tich AI"};
        String[] values = {
                "REQ-" + String.format("%04d", r.getId()),
                r.getTitle(),
                r.getRawDescription(),
                r.getStatus().name(),
                r.getCreatedAt() == null ? "" : r.getCreatedAt().format(DATE_FMT),
                a == null ? "Chua co ket qua phan tich." : a.getSummary()
        };
        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(i);
            Cell k = row.createCell(0);
            k.setCellValue(labels[i]);
            k.setCellStyle(headerStyle);
            row.createCell(1).setCellValue(values[i]);
        }
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 16000);
    }

    // Ghi 1 sheet dang danh sach tu 1 chuoi JSON array (dung chung cho FR / NFR / diem chua ro)
    private void writeJsonListSheet(Workbook wb, CellStyle headerStyle, String sheetName, String json) {
        Sheet sheet = wb.createSheet(safeName(sheetName));
        writeHeaderRow(sheet, headerStyle, new String[]{"STT", "Noi dung"});
        List<String> items = parseJsonList(json);
        for (int i = 0; i < items.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(items.get(i));
        }
        sheet.setColumnWidth(1, 20000);
    }

    private void writeUserStorySheet(Workbook wb, CellStyle headerStyle, List<UserStory> storyList) {
        Sheet sheet = wb.createSheet("User Story");
        writeHeaderRow(sheet, headerStyle, new String[]{"STT", "User Story", "Do uu tien", "So Acceptance Criteria"});
        int rowIdx = 1;
        for (UserStory story : storyList) {
            int acCount = criteria.findByUserStoryId(story.getId()).size();
            Row row = sheet.createRow(rowIdx);
            row.createCell(0).setCellValue(rowIdx);
            row.createCell(1).setCellValue(story.getContent());
            row.createCell(2).setCellValue(story.getPriority());
            row.createCell(3).setCellValue(acCount);
            rowIdx++;
        }
        sheet.setColumnWidth(1, 22000);
    }

    // Ma tran truy vet (Traceability Matrix): moi dong noi 1 User Story voi Acceptance Criteria cua no,
    // giup Hoi dong bao ve thay ro moi lien ket tu yeu cau goc -> dac ta -> tieu chi nghiem thu.
    private void writeTraceabilitySheet(Workbook wb, CellStyle headerStyle, Requirement r, List<UserStory> storyList) {
        Sheet sheet = wb.createSheet("Ma tran truy vet");
        writeHeaderRow(sheet, headerStyle, new String[]{"Ma yeu cau", "Tieu de yeu cau", "User Story", "Acceptance Criteria", "Trang thai kiem thu"});
        int rowIdx = 1;
        String reqCode = "REQ-" + String.format("%04d", r.getId());
        if (storyList.isEmpty()) {
            Row row = sheet.createRow(rowIdx);
            row.createCell(0).setCellValue(reqCode);
            row.createCell(1).setCellValue(r.getTitle());
            row.createCell(2).setCellValue("(chua co User Story)");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue("Chua kiem thu");
        }
        for (UserStory story : storyList) {
            List<AcceptanceCriteria> acList = criteria.findByUserStoryId(story.getId());
            if (acList.isEmpty()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(reqCode);
                row.createCell(1).setCellValue(r.getTitle());
                row.createCell(2).setCellValue(story.getContent());
                row.createCell(3).setCellValue("(chua co Acceptance Criteria)");
                row.createCell(4).setCellValue("Chua kiem thu");
            }
            for (AcceptanceCriteria ac : acList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(reqCode);
                row.createCell(1).setCellValue(r.getTitle());
                row.createCell(2).setCellValue(story.getContent());
                row.createCell(3).setCellValue(ac.getContent());
                row.createCell(4).setCellValue("Chua kiem thu");
            }
        }
        sheet.setColumnWidth(1, 10000);
        sheet.setColumnWidth(2, 18000);
        sheet.setColumnWidth(3, 18000);
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode n = mapper.readTree(json);
            if (n.isArray()) {
                return java.util.stream.StreamSupport.stream(n.spliterator(), false)
                        .map(x -> x.isTextual() ? x.asText() : x.toString())
                        .toList();
            }
        } catch (Exception ignored) {}
        return List.of(json);
    }

    private String safeName(String name) {
        // Ten sheet Excel gioi han 31 ky tu va khong duoc chua mot so ky tu dac biet
        String cleaned = name.replaceAll("[\\\\/*?:\\[\\]]", "");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private ResponseEntity<byte[]> toResponse(XSSFWorkbook wb, String filename) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    // ================== Helper: HTML (giu nguyen tu ban truoc) ==================

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String listHtml(String json) {
        List<String> items = parseJsonList(json);
        if (items.isEmpty()) return "<p>Chua co du lieu.</p>";
        StringBuilder b = new StringBuilder("<ul>");
        for (String x : items) b.append("<li>").append(esc(x)).append("</li>");
        return b.append("</ul>").toString();
    }

    private String userStoriesHtml(Long requirementId) {
        List<UserStory> list = stories.findByRequirementId(requirementId);
        if (list.isEmpty()) return "<p>Chua co UserStory.</p>";
        StringBuilder b = new StringBuilder("<ul>");
        for (UserStory story : list) {
            b.append("<li><strong>").append(esc(story.getPriority())).append("</strong>: ")
                    .append(esc(story.getContent())).append("</li>");
        }
        return b.append("</ul>").toString();
    }

    private String acceptanceCriteriaHtml(Long requirementId) {
        List<UserStory> list = stories.findByRequirementId(requirementId);
        StringBuilder b = new StringBuilder("<ul>");
        int count = 0;
        for (UserStory story : list) {
            for (AcceptanceCriteria c : criteria.findByUserStoryId(story.getId())) {
                b.append("<li>").append(esc(c.getContent())).append("</li>");
                count++;
            }
        }
        if (count == 0) return "<p>Chua co Acceptance Criteria.</p>";
        return b.append("</ul>").toString();
    }

    private String buildHtml(Requirement r, AnalysisResult a) {
        String summary = a == null ? "Chua co ket qua phan tich." : esc(a.getSummary());
        return "<!doctype html><html><head><meta charset='utf-8'><title>SRS - " + esc(r.getTitle()) +
                "</title><style>body{font-family:Arial,sans-serif;max-width:900px;margin:48px auto;color:#17211d;line-height:1.6}h1{font-size:30px}h2{margin-top:34px;border-bottom:1px solid #ddd;padding-bottom:7px}p,li{font-size:14px}.meta{color:#68746f}</style></head><body>" +
                "<h1>Dac ta yeu cau phan mem (SRS)</h1><p class='meta'>REQ-" + r.getId() + " &middot; " + esc(r.getStatus().name()) + "</p>" +
                "<h2>1. Yeu cau</h2><h3>" + esc(r.getTitle()) + "</h3><p>" + esc(r.getRawDescription()) + "</p>" +
                (a == null ? "<h2>2. Phan tich</h2><p>Chua co ket qua phan tich.</p>" :
                "<h2>2. Tom tat</h2><p>" + summary + "</p>" +
                "<h2>3. Yeu cau chuc nang</h2>" + listHtml(a.getFunctionalRequirements()) +
                "<h2>4. Yeu cau phi chuc nang</h2>" + listHtml(a.getNonFunctionalRequirements()) +
                "<h2>5. UserStory</h2>" + userStoriesHtml(r.getId()) +
                "<h2>6. Acceptance Criteria</h2>" + acceptanceCriteriaHtml(r.getId()) +
                "<h2>7. Diem chua ro / cau hoi mo</h2>" + listHtml(a.getAmbiguousNotes())) +
                "<h2>8. Kiem tra truoc khi trien khai</h2><p>Xac nhan cac quy tac nghiep vu, UserStory va Acceptance Criteria voi ben lien quan truoc khi trien khai.</p>" +
                "</body></html>";
    }
}
