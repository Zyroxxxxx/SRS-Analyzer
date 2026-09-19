package com.vlu.srsanalyzer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlu.srsanalyzer.dto.*;
import com.vlu.srsanalyzer.entity.Requirement;
import com.vlu.srsanalyzer.entity.User;
import com.vlu.srsanalyzer.security.AuthService;
import com.vlu.srsanalyzer.service.AiAnalysisService;
import com.vlu.srsanalyzer.service.RequirementService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class RequirementController {

    private final RequirementService service;
    private final AuthService auth;
    private final AiAnalysisService ai;
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping
    public List<RequirementResponse> getAll(HttpServletRequest r) {
        return service.getAll(auth.current(r));
    }

    @GetMapping("/{id}")
    public RequirementResponse getById(@PathVariable Long id, HttpServletRequest r) {
        return service.getById(id, auth.current(r));
    }

    @PostMapping
    public ResponseEntity<RequirementResponse> create(@Valid @RequestBody RequirementCreateRequest q, HttpServletRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(q, auth.current(r)));
    }

    @PutMapping("/{id}")
    public RequirementResponse update(@PathVariable Long id, @Valid @RequestBody RequirementCreateRequest q, HttpServletRequest r) {
        return service.update(id, q, auth.current(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest r) {
        service.delete(id, auth.current(r));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/analyze")
    public AiAnalysisResponse analyze(@PathVariable Long id, HttpServletRequest r) {
        User u = auth.current(r);
        return ai.analyze(service.entity(id, u), u);
    }

    @GetMapping("/{id}/analysis")
    public AiAnalysisResponse getAnalysis(@PathVariable Long id, HttpServletRequest r) {
        User u = auth.current(r);
        return ai.getSaved(service.entity(id, u), u);
    }

    // Ma tran truy vet dang JSON (ban Excel day du hon nam o /api/export/requirements/{id}/excel)
    @GetMapping("/{id}/traceability")
    public TraceabilityResponse traceability(@PathVariable Long id, HttpServletRequest r) {
        User u = auth.current(r);
        Requirement req = service.entity(id, u);
        AiAnalysisResponse a = ai.getSaved(req, u);
        return new TraceabilityResponse(
                req.getId(),
                req.getTitle(),
                req.getRawDescription(),
                req.getStatus().name(),
                a.getSummary(),
                toList(a.getFunctionalRequirements()),
                toList(a.getNonFunctionalRequirements()),
                toList(a.getUserStories()),
                toList(a.getAcceptanceCriteria()),
                toList(a.getAmbiguousNotes())
        );
    }

    private List<String> toList(String json) {
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
}
