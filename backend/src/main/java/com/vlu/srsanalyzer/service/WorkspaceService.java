package com.vlu.srsanalyzer.service;

import com.vlu.srsanalyzer.entity.*;
import com.vlu.srsanalyzer.exception.ResourceNotFoundException;
import com.vlu.srsanalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

// Nghiep vu cho giao dien chat: phien ban A/B, quan ly User Story/Acceptance Criteria, nhap file Word
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    public static final int MAX_WORDS = 100_000;

    public record VersionDto(Long id, int versionNo, String source, String content, LocalDateTime createdAt) {}
    public record CriteriaDto(Long id, String content) {}
    public record StoryDto(Long id, String content, String priority, boolean confirmed, List<CriteriaDto> criteria) {}

    private final RequirementService reqService;
    private final RequirementRepository requirements;
    private final RequirementVersionRepository versions;
    private final UserStoryRepository stories;
    private final AcceptanceCriteriaRepository criteria;
    private final AiAnalysisService ai;

    // ===================== Phien ban & Generation A/B =====================

    @Transactional
    public List<VersionDto> versions(Long reqId, User u) {
        Requirement r = reqService.entity(reqId, u);
        ensureBase(r);
        return versions.findByRequirementIdOrderByVersionNoDesc(reqId).stream()
                .map(v -> new VersionDto(v.getId(), v.getVersionNo(), v.getSource(), v.getContent(), v.getCreatedAt()))
                .toList();
    }

    @Transactional
    public Map<String, String> generateAB(Long reqId, User u) {
        Requirement r = reqService.entity(reqId, u);
        ensureBase(r);
        String text = r.getRawDescription();
        String a = ai.complete(abPrompt("A", text), u, r, "[Demo - chua cau hinh LLM_API_KEY]\nBan A (chinh thuc):\n1. The system shall " + firstLine(text));
        String b = ai.complete(abPrompt("B", text), u, r, "[Demo - chua cau hinh LLM_API_KEY]\nBan B (gon, agile):\nMuc tieu: " + firstLine(text));
        return Map.of("A", a, "B", b);
    }

    private String abPrompt(String variant, String text) {
        String style = variant.equals("A")
                ? "VARIANT A - formal and complete: an enterprise SRS-style specification with numbered 'The system shall ...' statements, explicit actors, business rules, constraints and error handling."
                : "VARIANT B - concise and agile: one short goal paragraph, a bullet list of key capabilities written from the user's point of view, then 'Out of scope' and 'Open questions' bullets.";
        return "You are a senior Business Analyst. Rewrite the requirement below as a clearer specification.\n"
                + style + "\nRules: preserve the original meaning, do not invent features, answer in the same language as the input, "
                + "output plain text only (no Markdown code fences, no preamble).\n\nREQUIREMENT:\n" + text;
    }

    @Transactional
    public Requirement applyVersion(Long reqId, String source, String content, User u) {
        Requirement r = reqService.entity(reqId, u);
        if (content == null || content.isBlank()) throw new RuntimeException("Noi dung phien ban khong duoc de trong");
        ensureBase(r);
        String src = source != null && Set.of("A", "B", "MANUAL").contains(source) ? source : "MANUAL";
        return saveVersion(r, src, content.trim());
    }

    @Transactional
    public Requirement restore(Long reqId, Long versionId, User u) {
        Requirement r = reqService.entity(reqId, u);
        RequirementVersion v = versions.findById(versionId)
                .filter(x -> x.getRequirement().getId().equals(reqId))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phien ban #" + versionId));
        return saveVersion(r, "RESTORE", v.getContent());
    }

    private Requirement saveVersion(Requirement r, String source, String content) {
        RequirementVersion v = new RequirementVersion();
        v.setRequirement(r);
        v.setVersionNo(versions.maxVersionNo(r.getId()) + 1);
        v.setSource(source);
        v.setContent(content);
        versions.save(v);
        r.setRawDescription(content);
        r.setStatus(RequirementStatus.PENDING); // noi dung doi -> can phan tich lai
        return requirements.save(r);
    }

    private void ensureBase(Requirement r) {
        if (versions.maxVersionNo(r.getId()) == 0) {
            RequirementVersion v = new RequirementVersion();
            v.setRequirement(r);
            v.setVersionNo(1);
            v.setSource("ORIGINAL");
            v.setContent(r.getRawDescription());
            versions.save(v);
        }
    }

    private String firstLine(String s) {
        String t = s.strip();
        int i = t.indexOf('\n');
        t = i > 0 ? t.substring(0, i) : t;
        return t.length() > 300 ? t.substring(0, 300) + "..." : t;
    }

    // ===================== User Story & Acceptance Criteria =====================

    @Transactional(readOnly = true)
    public List<StoryDto> stories(Long reqId, User u) {
        reqService.entity(reqId, u);
        return stories.findByRequirementId(reqId).stream()
                .sorted(Comparator.comparing(UserStory::getId))
                .map(this::toDto).toList();
    }

    @Transactional
    public StoryDto addStory(Long reqId, String content, String priority, User u) {
        Requirement r = reqService.entity(reqId, u);
        if (content == null || content.isBlank()) throw new RuntimeException("Noi dung User Story khong duoc de trong");
        return toDto(stories.save(new UserStory(null, r, content.trim(), normPriority(priority), false, null)));
    }

    @Transactional
    public StoryDto updateStory(Long storyId, String content, String priority, User u) {
        UserStory s = story(storyId, u);
        if (content != null) {
            if (content.isBlank()) throw new RuntimeException("Noi dung User Story khong duoc de trong");
            s.setContent(content.trim());
        }
        if (priority != null) s.setPriority(normPriority(priority));
        s.setConfirmed(false); // sua xong phai xac nhan lai
        return toDto(stories.save(s));
    }

    @Transactional
    public void deleteStory(Long storyId, User u) {
        UserStory s = story(storyId, u);
        criteria.deleteAllByUserStoryId(s.getId());
        stories.delete(s);
    }

    // Goi y viet lai (chua luu) - giao dien cho nguoi dung bam "Ap dung" thi moi goi updateStory
    @Transactional
    public String rewriteStory(Long storyId, String instruction, User u) {
        UserStory s = story(storyId, u);
        Requirement r = s.getRequirement();
        String hint = instruction == null || instruction.isBlank() ? "Make it clearer, specific and testable." : instruction.trim();
        String prompt = "Rewrite this User Story using the exact format \"As a [role], I want to [action], so that [benefit].\" "
                + "Keep the original meaning and the same language as the input. Instruction: " + hint
                + "\nReturn ONLY the rewritten story as one sentence, no quotes, no explanation.\n\nUSER STORY:\n" + s.getContent()
                + "\n\nREQUIREMENT CONTEXT:\n" + firstLine(r.getRawDescription());
        return ai.complete(prompt, u, r, s.getContent() + " [Demo - chua cau hinh LLM_API_KEY]");
    }

    @Transactional
    public StoryDto addCriteria(Long storyId, String content, User u) {
        UserStory s = story(storyId, u);
        if (content == null || content.isBlank()) throw new RuntimeException("Noi dung Acceptance Criteria khong duoc de trong");
        criteria.save(new AcceptanceCriteria(null, s, content.trim(), null));
        return unconfirm(s);
    }

    @Transactional
    public StoryDto updateCriteria(Long criteriaId, String content, User u) {
        AcceptanceCriteria c = criteriaOf(criteriaId, u);
        if (content == null || content.isBlank()) throw new RuntimeException("Noi dung Acceptance Criteria khong duoc de trong");
        c.setContent(content.trim());
        criteria.save(c);
        return unconfirm(c.getUserStory());
    }

    @Transactional
    public StoryDto deleteCriteria(Long criteriaId, User u) {
        AcceptanceCriteria c = criteriaOf(criteriaId, u);
        UserStory s = c.getUserStory();
        criteria.delete(c);
        return unconfirm(s);
    }

    @Transactional
    public StoryDto confirm(Long storyId, boolean confirmed, User u) {
        UserStory s = story(storyId, u);
        if (confirmed && criteria.findByUserStoryId(s.getId()).isEmpty())
            throw new RuntimeException("Can it nhat 1 Acceptance Criteria truoc khi xac nhan User Story");
        s.setConfirmed(confirmed);
        return toDto(stories.save(s));
    }

    private StoryDto unconfirm(UserStory s) {
        s.setConfirmed(false);
        return toDto(stories.save(s));
    }

    private UserStory story(Long id, User u) {
        UserStory s = stories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Khong tim thay User Story #" + id));
        reqService.entity(s.getRequirement().getId(), u); // kiem tra quyen so huu
        return s;
    }

    private AcceptanceCriteria criteriaOf(Long id, User u) {
        AcceptanceCriteria c = criteria.findById(id).orElseThrow(() -> new ResourceNotFoundException("Khong tim thay Acceptance Criteria #" + id));
        story(c.getUserStory().getId(), u);
        return c;
    }

    private StoryDto toDto(UserStory s) {
        List<CriteriaDto> cs = criteria.findByUserStoryId(s.getId()).stream()
                .sorted(Comparator.comparing(AcceptanceCriteria::getId))
                .map(c -> new CriteriaDto(c.getId(), c.getContent())).toList();
        return new StoryDto(s.getId(), s.getContent(), s.getPriority(), Boolean.TRUE.equals(s.getConfirmed()), cs);
    }

    private String normPriority(String p) {
        return p != null && Set.of("HIGH", "MEDIUM", "LOW").contains(p.toUpperCase()) ? p.toUpperCase() : "MEDIUM";
    }

    // ===================== Upload Word (.docx), toi da 100.000 tu =====================

    @Transactional
    public Requirement importDocx(MultipartFile file, User u) throws IOException {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!name.toLowerCase().endsWith(".docx"))
            throw new RuntimeException("Chi ho tro file Word .docx (hay luu file .doc cu thanh .docx)");

        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            for (IBodyElement e : doc.getBodyElements()) {
                if (e instanceof XWPFParagraph p) {
                    String t = p.getText();
                    if (t != null && !t.isBlank()) sb.append(t.strip()).append('\n');
                } else if (e instanceof XWPFTable tbl) {
                    for (XWPFTableRow row : tbl.getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (XWPFTableCell c : row.getTableCells()) cells.add(c.getText().strip().replace('\n', ' '));
                        sb.append(String.join(" | ", cells)).append('\n');
                    }
                }
            }
        } catch (IOException | RuntimeException ex) {
            throw new RuntimeException("Khong doc duoc file Word. File co the bi hong hoac khong phai .docx hop le.");
        }

        String text = sb.toString().strip();
        if (text.isEmpty()) throw new RuntimeException("File Word khong co noi dung van ban");
        int words = text.split("\\s+").length;
        if (words > MAX_WORDS)
            throw new RuntimeException("File co " + words + " tu, vuot gioi han " + MAX_WORDS + " tu. Vui long tach nho tai lieu.");

        String title = name.substring(0, name.length() - 5).strip();
        if (title.isEmpty()) title = "Tai lieu Word";
        if (title.length() > 255) title = title.substring(0, 255);

        Requirement r = new Requirement();
        r.setTitle(title);
        r.setRawDescription(text);
        r.setUser(u);
        r.setStatus(RequirementStatus.PENDING);
        r = requirements.save(r);

        RequirementVersion v = new RequirementVersion();
        v.setRequirement(r);
        v.setVersionNo(1);
        v.setSource("UPLOAD");
        v.setContent(text);
        versions.save(v);
        return r;
    }
}
