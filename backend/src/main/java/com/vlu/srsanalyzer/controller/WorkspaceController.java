package com.vlu.srsanalyzer.controller;

import com.vlu.srsanalyzer.dto.RequirementResponse;
import com.vlu.srsanalyzer.entity.*;
import com.vlu.srsanalyzer.repository.AnalysisResultRepository;
import com.vlu.srsanalyzer.security.AuthService;
import com.vlu.srsanalyzer.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.origin}")
public class WorkspaceController {

    public record VersionRequest(String source, String content) {}
    public record StoryRequest(String content, String priority) {}
    public record TextRequest(String content) {}
    public record RewriteRequest(String instruction) {}
    public record ConfirmRequest(boolean confirmed) {}
    public record EmailRequest(List<String> to, String subject, String message, boolean attachSrs, boolean attachSwimlane) {}

    private final WorkspaceService ws;
    private final SwimlaneService swimlane;
    private final EmailService email;
    private final RequirementService reqService;
    private final AnalysisResultRepository results;
    private final ExportController export;
    private final AuthService auth;

    // ---- Phien ban & A/B ----
    @GetMapping("/requirements/{id}/versions")
    public List<WorkspaceService.VersionDto> versions(@PathVariable Long id, HttpServletRequest r) {
        return ws.versions(id, auth.current(r));
    }

    @PostMapping("/requirements/{id}/generate-ab")
    public Map<String, String> generateAB(@PathVariable Long id, HttpServletRequest r) {
        return ws.generateAB(id, auth.current(r));
    }

    @PostMapping("/requirements/{id}/versions")
    public RequirementResponse applyVersion(@PathVariable Long id, @RequestBody VersionRequest q, HttpServletRequest r) {
        return RequirementResponse.fromEntity(ws.applyVersion(id, q.source(), q.content(), auth.current(r)));
    }

    @PostMapping("/requirements/{id}/versions/{versionId}/restore")
    public RequirementResponse restore(@PathVariable Long id, @PathVariable Long versionId, HttpServletRequest r) {
        return RequirementResponse.fromEntity(ws.restore(id, versionId, auth.current(r)));
    }

    // ---- User Story / Acceptance Criteria ----
    @GetMapping("/requirements/{id}/stories")
    public List<WorkspaceService.StoryDto> stories(@PathVariable Long id, HttpServletRequest r) {
        return ws.stories(id, auth.current(r));
    }

    @PostMapping("/requirements/{id}/stories")
    public WorkspaceService.StoryDto addStory(@PathVariable Long id, @RequestBody StoryRequest q, HttpServletRequest r) {
        return ws.addStory(id, q.content(), q.priority(), auth.current(r));
    }

    @PutMapping("/stories/{sid}")
    public WorkspaceService.StoryDto updateStory(@PathVariable Long sid, @RequestBody StoryRequest q, HttpServletRequest r) {
        return ws.updateStory(sid, q.content(), q.priority(), auth.current(r));
    }

    @DeleteMapping("/stories/{sid}")
    public ResponseEntity<Void> deleteStory(@PathVariable Long sid, HttpServletRequest r) {
        ws.deleteStory(sid, auth.current(r));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/stories/{sid}/rewrite")
    public Map<String, String> rewrite(@PathVariable Long sid, @RequestBody RewriteRequest q, HttpServletRequest r) {
        return Map.of("content", ws.rewriteStory(sid, q.instruction(), auth.current(r)));
    }

    @PostMapping("/stories/{sid}/confirm")
    public WorkspaceService.StoryDto confirm(@PathVariable Long sid, @RequestBody ConfirmRequest q, HttpServletRequest r) {
        return ws.confirm(sid, q.confirmed(), auth.current(r));
    }

    @PostMapping("/stories/{sid}/criteria")
    public WorkspaceService.StoryDto addCriteria(@PathVariable Long sid, @RequestBody TextRequest q, HttpServletRequest r) {
        return ws.addCriteria(sid, q.content(), auth.current(r));
    }

    @PutMapping("/criteria/{cid}")
    public WorkspaceService.StoryDto updateCriteria(@PathVariable Long cid, @RequestBody TextRequest q, HttpServletRequest r) {
        return ws.updateCriteria(cid, q.content(), auth.current(r));
    }

    @DeleteMapping("/criteria/{cid}")
    public WorkspaceService.StoryDto deleteCriteria(@PathVariable Long cid, HttpServletRequest r) {
        return ws.deleteCriteria(cid, auth.current(r));
    }

    // ---- Upload Word ----
    @PostMapping(value = "/requirements/import-docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RequirementResponse> importDocx(@RequestParam("file") MultipartFile file, HttpServletRequest r) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(RequirementResponse.fromEntity(ws.importDocx(file, auth.current(r))));
    }

    // ---- SwimLane PNG ----
    @GetMapping(value = "/requirements/{id}/swimlane.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> swimlanePng(@PathVariable Long id, HttpServletRequest r) throws IOException {
        User u = auth.current(r);
        Requirement req = reqService.entity(id, u);
        byte[] png = swimlane.render(req.getTitle(), ws.stories(id, u));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=REQ-" + id + "-swimlane.png")
                .contentType(MediaType.IMAGE_PNG).body(png);
    }

    // ---- Gui email ----
    @PostMapping("/requirements/{id}/email")
    public Map<String, String> sendEmail(@PathVariable Long id, @RequestBody EmailRequest q, HttpServletRequest r) throws IOException {
        User u = auth.current(r);
        Requirement req = reqService.entity(id, u);
        Map<String, Object[]> files = new LinkedHashMap<>();
        if (q.attachSrs()) {
            AnalysisResult a = results.findByRequirementId(id).orElse(null);
            files.put("REQ-" + id + "-SRS.html", new Object[]{export.buildHtml(req, a).getBytes(StandardCharsets.UTF_8), "text/html; charset=UTF-8"});
        }
        if (q.attachSwimlane())
            files.put("REQ-" + id + "-swimlane.png", new Object[]{swimlane.render(req.getTitle(), ws.stories(id, u)), "image/png"});
        String subject = q.subject() == null || q.subject().isBlank() ? "SRS: " + req.getTitle() : q.subject().trim();
        String body = (q.message() == null ? "" : q.message().trim() + "\n\n") + "Yeu cau: " + req.getTitle()
                + "\nGui boi: " + (u.getFullName() != null ? u.getFullName() : u.getUsername()) + " (SRS Analyzer)";
        email.send(q.to(), subject, body, files);
        return Map.of("message", "Da gui email thanh cong");
    }
}
