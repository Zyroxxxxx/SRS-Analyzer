package com.vlu.srsanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vlu.srsanalyzer.dto.AiAnalysisResponse;
import com.vlu.srsanalyzer.entity.*;
import com.vlu.srsanalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final AnalysisResultRepository results;
    private final AiUsageRepository usage;
    private final UserStoryRepository stories;
    private final AcceptanceCriteriaRepository criteria;
    private final RequirementRepository requirements;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.api.model:gemini-3.6-flash}")
    private String model;

    @Transactional
    public AiAnalysisResponse analyze(Requirement req, User user) {
        enforceQuota(user);

        String prompt = buildPrompt(req, user);
        try {
            AiAnalysisResponse parsed;
            if (apiKey == null || apiKey.isBlank()) {
                parsed = mockResult(req, user, prompt);
            } else {
                JsonNode root = callGemini(prompt);
                String text = root.at("/candidates/0/content/parts/0/text").asText("");
                JsonNode meta = root.path("usageMetadata");
                int in = meta.path("promptTokenCount").asInt(0);
                int out = meta.path("candidatesTokenCount").asInt(0);
                int total = meta.path("totalTokenCount").asInt(in + out);
                parsed = parse(text, req.getId(), in, out, total, "Gemini");
            }
            persist(req, user, prompt, parsed);
            return parsed;
        } catch (Exception e) {
            req.setStatus(RequirementStatus.FAILED);
            requirements.save(req); // Luu tuong minh de trang thai loi khong bi mat sau khi tai lai trang
            throw new RuntimeException("Phan tich AI that bai: " + e.getMessage());
        }
    }

    // Chan truoc khi goi AI neu user da dung het han muc token duoc cap
    private void enforceQuota(User user) {
        if (user.getTokenQuota() == null) return; // null = khong gioi han
        long used = usage.totalTokensByUser(user.getId());
        if (used >= user.getTokenQuota()) {
            throw new RuntimeException(
                    "Da vuot han muc token duoc cap (" + used + "/" + user.getTokenQuota() +
                    "). Vui long lien he quan tri vien de duoc cap them.");
        }
    }

    private JsonNode callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
        ObjectNode body = mapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode c = contents.addObject();
        ArrayNode parts = c.putArray("parts");
        parts.addObject().put("text", prompt);
        ObjectNode cfg = body.putObject("generationConfig");
        cfg.put("temperature", 0.2);
        cfg.put("responseMimeType", "application/json");
        return RestClient.create().post().uri(url)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    // Prompt duoc thiet ke de: (1) noi dung tra ve bang tieng Viet de nguoi dung de doc,
    // (2) nhung GIU NGUYEN cac thuat ngu chuyen nganh bang tieng Anh (khong bi dich lon xon),
    // (3) User Story theo dung mau chuan nganh "As a ... I want to ... so that ...".
    private String buildPrompt(Requirement r, User u) {
        String language = u.getAnalysisLanguageOrDefault();
        String languageInstruction = language.equals("VI")
                ? "Generate ALL natural-language analysis content in Vietnamese."
                : "Generate ALL natural-language analysis content in English.";

        return """
                You are a professional Business Analyst specializing in software requirements engineering and SRS analysis.

                OUTPUT LANGUAGE (MANDATORY):
                - %s
                - Do not mix Vietnamese and English in the generated prose.
                - Keep standard software engineering terms in their established English form when appropriate, such as User Story, Acceptance Criteria, Functional Requirement, Non-functional Requirement, Given-When-Then, API, UI, UX, Frontend, Backend, Database, Token.
                - User Stories must always use the standard structure: "As a [role], I want to [action], so that [benefit]."
                - Acceptance Criteria must always use the Given-When-Then structure and keep the keywords Given, When, Then in English.
                - The language choice applies to summary, Functional Requirements, Non-functional Requirements, User Stories, Acceptance Criteria, and ambiguity notes.

                Return ONLY one valid JSON object (no Markdown, no explanation) with exactly these keys:
                summary (string), functionalRequirements (array of strings), nonFunctionalRequirements (array of strings),
                userStories (array of strings), acceptanceCriteria (array of strings), ambiguousNotes (array of strings).
                Each list item must be concise, specific, and testable where applicable.

                USER ANALYSIS PREFERENCE: %s
                REQUIREMENT TITLE: %s
                REQUIREMENT DESCRIPTION (preserve the user's meaning): %s
                """.formatted(
                languageInstruction,
                Objects.toString(u.getAiPreference(), "No special preference"),
                r.getTitle(),
                r.getRawDescription()
        );
    }

    private AiAnalysisResponse parse(String text, Long id, int in, int out, int total, String provider) throws Exception {
        JsonNode n = mapper.readTree(text);
        return new AiAnalysisResponse(
                id,
                n.path("summary").asText(),
                n.path("functionalRequirements").toString(),
                n.path("nonFunctionalRequirements").toString(),
                n.path("userStories").toString(),
                n.path("acceptanceCriteria").toString(),
                n.path("ambiguousNotes").toString(),
                in, out, total, provider
        );
    }

    // Dung khi chua cau hinh LLM_API_KEY - cho phep demo/chay thu he thong ma khong can goi AI that
    private AiAnalysisResponse mockResult(Requirement r, User u, String prompt) {
        if (u.getAnalysisLanguageOrDefault().equals("VI")) {
            return new AiAnalysisResponse(
                    r.getId(),
                    "Yêu cầu đã được chuẩn hóa theo hướng enterprise (chế độ demo, chưa cấu hình LLM_API_KEY).",
                    "[\"Hệ thống phải kiểm tra tính hợp lệ của yêu cầu được gửi lên.\",\"Hệ thống phải lưu yêu cầu kèm chủ sở hữu và trạng thái.\"]",
                    "[\"Thời gian phản hồi phải nằm trong ngưỡng chấp nhận được cho tương tác trực tuyến.\",\"Dữ liệu yêu cầu chỉ hiển thị cho chủ sở hữu hoặc quản trị viên.\"]",
                    "[\"As a project analyst, I want to have my requirement analyzed, so that I can turn natural language into a testable specification.\"]",
                    "[\"Given a valid requirement, when analysis is requested, then the system returns structured Functional and Non-functional Requirements.\"]",
                    "[\"Cần xác nhận lại với các bên liên quan về quy tắc nghiệp vụ, vai trò người dùng và các trường hợp đặc biệt.\"]",
                    0, 0, 0, "Demo analyzer (LLM_API_KEY not configured)"
            );
        }
        return new AiAnalysisResponse(
                r.getId(),
                "The requirement has been normalized into an enterprise-oriented specification (demo mode; LLM_API_KEY is not configured).",
                "[\"The system shall validate the submitted requirement.\",\"The system shall store the requirement with its owner and status.\"]",
                "[\"The response time shall remain within an acceptable threshold for interactive use.\",\"Requirement data shall be visible only to the owner or an administrator.\"]",
                "[\"As a project analyst, I want to have my requirement analyzed, so that I can turn natural language into a testable specification.\"]",
                "[\"Given a valid requirement, when analysis is requested, then the system returns structured Functional and Non-functional Requirements.\"]",
                "[\"The business rules, user roles, and edge cases should be confirmed with relevant stakeholders.\"]",
                0, 0, 0, "Demo analyzer (LLM_API_KEY not configured)"
        );
    }

    @Transactional
    protected void persist(Requirement r, User u, String prompt, AiAnalysisResponse a) {
        r.setStatus(RequirementStatus.ANALYZED);
        requirements.save(r); // Fix: truoc day thieu dong nay nen trang thai khong duoc luu that su

        AnalysisResult ar = results.findByRequirementId(r.getId()).orElse(new AnalysisResult());
        ar.setRequirement(r);
        ar.setSummary(a.getSummary());
        ar.setFunctionalRequirements(a.getFunctionalRequirements());
        ar.setNonFunctionalRequirements(a.getNonFunctionalRequirements());
        ar.setAmbiguousNotes(a.getAmbiguousNotes());
        results.save(ar);

        try {
            JsonNode us = mapper.readTree(a.getUserStories());
            JsonNode ac = mapper.readTree(a.getAcceptanceCriteria());

            // Khi phan tich lai, thay the UserStory/Acceptance Criteria cu de khong tao ban ghi trung
            for (UserStory oldStory : stories.findByRequirementId(r.getId())) {
                criteria.deleteAllByUserStoryId(oldStory.getId());
                stories.delete(oldStory);
            }

            if (us.isArray()) {
                int i = 0;
                for (JsonNode x : us) {
                    UserStory story = new UserStory(null, r, x.isTextual() ? x.asText() : x.toString(), "MEDIUM", null);
                    story = stories.save(story);
                    if (ac.isArray() && ac.size() > 0) {
                        JsonNode c = ac.get(Math.min(i, ac.size() - 1));
                        criteria.save(new AcceptanceCriteria(null, story, c.isTextual() ? c.asText() : c.toString(), null));
                    }
                    i++;
                }
            }
        } catch (Exception ignored) {
            // Neu AI tra ve JSON khong dung dinh dang cho phan nay, van giu lai summary/FR/NFR da luu o tren
        }

        usage.save(new AiUsage(null, u, r, prompt, a.getPromptTokens(), a.getOutputTokens(), a.getTotalTokens(), a.getProvider(), null));
    }

    @Transactional(readOnly = true)
    public AiAnalysisResponse getSaved(Requirement req, User user) {
        AnalysisResult ar = results.findByRequirementId(req.getId())
                .orElseThrow(() -> new RuntimeException("Chua co ket qua phan tich cho yeu cau nay"));
        try {
            var us = mapper.createArrayNode();
            var ac = mapper.createArrayNode();
            for (UserStory story : stories.findByRequirementId(req.getId())) {
                us.add(story.getContent());
                for (AcceptanceCriteria c : criteria.findByUserStoryId(story.getId())) {
                    ac.add(c.getContent());
                }
            }

            Optional<AiUsage> last = usage.findByRequirementIdOrderByCreatedAtDesc(req.getId()).stream().findFirst();
            int in = last.map(x -> Objects.requireNonNullElse(x.getPromptTokens(), 0)).orElse(0);
            int out = last.map(x -> Objects.requireNonNullElse(x.getOutputTokens(), 0)).orElse(0);
            int total = last.map(x -> Objects.requireNonNullElse(x.getTotalTokens(), 0)).orElse(0);
            String provider = last.map(AiUsage::getProvider).orElse("Gemini");

            return new AiAnalysisResponse(
                    req.getId(),
                    Objects.toString(ar.getSummary(), ""),
                    Objects.toString(ar.getFunctionalRequirements(), "[]"),
                    Objects.toString(ar.getNonFunctionalRequirements(), "[]"),
                    us.toString(),
                    ac.toString(),
                    Objects.toString(ar.getAmbiguousNotes(), "[]"),
                    in, out, total, provider
            );
        } catch (Exception e) {
            throw new RuntimeException("Khong the doc ket qua phan tich: " + e.getMessage());
        }
    }
}
