package com.vlu.srsanalyzer.controller;

import com.vlu.srsanalyzer.entity.*;
import com.vlu.srsanalyzer.exception.ForbiddenException;
import com.vlu.srsanalyzer.repository.*;
import com.vlu.srsanalyzer.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.*;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins="http://localhost:5173")
@RequiredArgsConstructor
public class ExcelExportController {
    private final RequirementRepository requirements;
    private final AnalysisResultRepository results;
    private final UserStoryRepository stories;
    private final AcceptanceCriteriaRepository criteria;
    private final AiUsageRepository usage;
    private final UserRepository users;
    private final AuthService auth;

    private void checkOwner(Requirement r, User u) {
        if (u.getRole() != Role.ADMIN && (r.getUser() == null || !r.getUser().getId().equals(u.getId())))
            throw new ForbiddenException("Bạn không có quyền xuất dữ liệu của yêu cầu này.");
    }

    @GetMapping(value="/requirements/{id}/excel", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Transactional(readOnly=true)
    public ResponseEntity<byte[]> requirementExcel(@PathVariable long id, HttpServletRequest request) throws Exception {
        User u = auth.current(request);
        Requirement r = requirements.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu."));
        checkOwner(r,u);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = header(wb);
            Sheet info = wb.createSheet("SRS");
            String[][] rows = {
                {"Mục", "Nội dung"},
                {"Requirement", "REQ-"+id},
                {"Tiêu đề", Objects.toString(r.getTitle(),"")},
                {"Nội dung", Objects.toString(r.getRawDescription(),"")},
                {"Trạng thái", r.getStatus().name()}
            };
            for(int i=0;i<rows.length;i++){Row row=info.createRow(i);for(int j=0;j<2;j++){Cell c=row.createCell(j);c.setCellValue(rows[i][j]);if(i==0)c.setCellStyle(head);}}
            AnalysisResult a=results.findByRequirementId(id).orElse(null);
            int row=6;
            row=section(info, row, head, "Tóm tắt", a==null?"Chưa có kết quả phân tích.":a.getSummary());
            row=section(info, row, head, "Yêu cầu chức năng", a==null?"":a.getFunctionalRequirements());
            row=section(info, row, head, "Yêu cầu phi chức năng", a==null?"":a.getNonFunctionalRequirements());
            row=section(info, row, head, "Điểm chưa rõ", a==null?"":a.getAmbiguousNotes());
            for(int i=0;i<2;i++) info.autoSizeColumn(i);

            Sheet usSheet=wb.createSheet("UserStory & Acceptance Criteria");
            Row h=usSheet.createRow(0); String[] hs={"STT","UserStory","Priority","Acceptance Criteria"};
            for(int i=0;i<hs.length;i++){h.createCell(i).setCellValue(hs[i]);h.getCell(i).setCellStyle(head);}
            int rr=1, index=1;
            for (UserStory story : stories.findByRequirementId(id)) {
                long storyId = Objects.requireNonNull(
                        story.getId(),
                        "UserStory ID không hợp lệ."
                );
                List<AcceptanceCriteria> acceptanceCriteria =
                        criteria.findByUserStoryId(storyId);

                if (acceptanceCriteria.isEmpty()) {
                    Row rowData = usSheet.createRow(rr++);
                    rowData.createCell(0).setCellValue(index++);
                    rowData.createCell(1).setCellValue(
                            Objects.toString(story.getContent(), "")
                    );
                    rowData.createCell(2).setCellValue(
                            Objects.toString(story.getPriority(), "")
                    );
                } else {
                    for (AcceptanceCriteria criterion : acceptanceCriteria) {
                        Row rowData = usSheet.createRow(rr++);
                        rowData.createCell(0).setCellValue(index++);
                        rowData.createCell(1).setCellValue(
                                Objects.toString(story.getContent(), "")
                        );
                        rowData.createCell(2).setCellValue(
                                Objects.toString(story.getPriority(), "")
                        );
                        rowData.createCell(3).setCellValue(
                                Objects.toString(criterion.getContent(), "")
                        );
                    }
                }
            }
            for(int i=0;i<4;i++) usSheet.autoSizeColumn(i);
            byte[] bytes=wbToBytes(wb,out);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=REQ-"+id+"-SRS.xlsx").contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(bytes);
        }
    }

    @GetMapping(value="/admin/usage-excel", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Transactional(readOnly=true)
    public ResponseEntity<byte[]> adminUsageExcel(HttpServletRequest request) throws Exception {
        User admin=auth.current(request); if(admin.getRole()!=Role.ADMIN) throw new RuntimeException("Bạn không có quyền xuất báo cáo quản trị.");
        try(Workbook wb=new XSSFWorkbook();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            CellStyle head=header(wb);
            Sheet us=wb.createSheet("Users"); String[] uh={"User ID","Tài khoản","Họ tên","Email","Role","Prompt Preference","Số Requirement","Lượt AI","Tổng Token","Tạo lúc"}; Row h=us.createRow(0); for(int i=0;i<uh.length;i++){h.createCell(i).setCellValue(uh[i]);h.getCell(i).setCellStyle(head);} int row=1;
            for(User u:users.findAll()){Row x=us.createRow(row++);int c=0;x.createCell(c++).setCellValue(u.getId());x.createCell(c++).setCellValue(Objects.toString(u.getUsername(),""));x.createCell(c++).setCellValue(Objects.toString(u.getFullName(),""));x.createCell(c++).setCellValue(Objects.toString(u.getEmail(),""));x.createCell(c++).setCellValue(u.getRole().name());x.createCell(c++).setCellValue(Objects.toString(u.getAiPreference(),""));x.createCell(c++).setCellValue(requirements.findByUserId(u.getId()).size());x.createCell(c++).setCellValue(usage.requestsByUser(u.getId()));x.createCell(c++).setCellValue(usage.totalTokensByUser(u.getId()));x.createCell(c++).setCellValue(Objects.toString(u.getCreatedAt(),""));}
            for(int i=0;i<uh.length;i++)us.autoSizeColumn(i);
            Sheet rq=wb.createSheet("Requirements"); String[] rh={"REQ ID","Tiêu đề","Trạng thái","User ID","Username","Họ tên","Prompt Preference","Ngày tạo","Cập nhật"}; h=rq.createRow(0);for(int i=0;i<rh.length;i++){h.createCell(i).setCellValue(rh[i]);h.getCell(i).setCellStyle(head);} row=1;
            for(Requirement q:requirements.findAll()){User u=q.getUser();Row x=rq.createRow(row++);int c=0;x.createCell(c++).setCellValue("REQ-"+String.format("%04d",q.getId()));x.createCell(c++).setCellValue(Objects.toString(q.getTitle(),""));x.createCell(c++).setCellValue(q.getStatus().name());x.createCell(c++).setCellValue(u==null?0:u.getId());x.createCell(c++).setCellValue(u==null?"":u.getUsername());x.createCell(c++).setCellValue(u==null?"":Objects.toString(u.getFullName(),""));x.createCell(c++).setCellValue(u==null?"":Objects.toString(u.getAiPreference(),""));x.createCell(c++).setCellValue(Objects.toString(q.getCreatedAt(),""));x.createCell(c++).setCellValue(Objects.toString(q.getUpdatedAt(),""));}
            for(int i=0;i<rh.length;i++)rq.autoSizeColumn(i);
            Sheet ai=wb.createSheet("AI Usage"); String[] ah={"Usage ID","User","REQ ID","Requirement","Prompt","Prompt Token","Output Token","Total Token","Provider","Thời gian"}; h=ai.createRow(0);for(int i=0;i<ah.length;i++){h.createCell(i).setCellValue(ah[i]);h.getCell(i).setCellStyle(head);} row=1; for(AiUsage a:usage.findRecent()){Row x=ai.createRow(row++);int c=0;x.createCell(c++).setCellValue(a.getId());x.createCell(c++).setCellValue(a.getUser().getUsername());x.createCell(c++).setCellValue("REQ-"+String.format("%04d",a.getRequirement().getId()));x.createCell(c++).setCellValue(a.getRequirement().getTitle());x.createCell(c++).setCellValue(Objects.toString(a.getPrompt(),""));x.createCell(c++).setCellValue(Objects.requireNonNullElse(a.getPromptTokens(),0));x.createCell(c++).setCellValue(Objects.requireNonNullElse(a.getOutputTokens(),0));x.createCell(c++).setCellValue(Objects.requireNonNullElse(a.getTotalTokens(),0));x.createCell(c++).setCellValue(Objects.toString(a.getProvider(),""));x.createCell(c++).setCellValue(Objects.toString(a.getCreatedAt(),""));} for(int i=0;i<ah.length;i++)ai.autoSizeColumn(i);
            wb.write(out); return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=ADMIN-SRS-REPORT.xlsx").contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(out.toByteArray());
        }
    }
    private CellStyle header(Workbook wb){CellStyle s=wb.createCellStyle(); Font font=wb.createFont(); font.setBold(true); s.setFont(font); s.setWrapText(true); return s;}
    private int section(Sheet s,int row,CellStyle head,String title,String value){Row h=s.createRow(row++);h.createCell(0).setCellValue(title);h.getCell(0).setCellStyle(head);h.createCell(1).setCellValue(Objects.toString(value,""));return row+1;}
    private byte[] wbToBytes(Workbook wb,ByteArrayOutputStream out)throws Exception{wb.write(out);return out.toByteArray();}
}
