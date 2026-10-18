package com.tracemind.backend.controller;

import com.tracemind.backend.entity.ReplayRunEntity;
import com.tracemind.backend.repository.ReplayRunRepository;
import com.tracemind.backend.service.ReplayService;
import com.tracemind.backend.service.ReplayService.ReplayRequestDto;
import com.tracemind.backend.service.ReplayService.ReplayValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/replay")
@Tag(name = "Controlled Replay", description = "Safe Sandboxed Scenario Reproduction and Verification")
public class ReplayController {

    private final ReplayService replayService;
    private final ReplayRunRepository replayRunRepository;

    public ReplayController(ReplayService replayService, ReplayRunRepository replayRunRepository) {
        this.replayService = replayService;
        this.replayRunRepository = replayRunRepository;
    }

    @PostMapping("/preview")
    @Operation(summary = "Validate and preview safe replay configuration before execution")
    public ResponseEntity<ReplayValidationResult> previewReplay(@RequestBody ReplayRequestDto request) {
        return ResponseEntity.ok(replayService.previewAndValidate(request));
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute sandboxed replay scenario with explicit confirmation")
    public ResponseEntity<ReplayRunEntity> executeReplay(@RequestBody ReplayRequestDto request) {
        return ResponseEntity.ok(replayService.executeReplay(request));
    }

    @GetMapping("/history")
    @Operation(summary = "Get historical audit log of replay runs")
    public ResponseEntity<List<ReplayRunEntity>> getReplayHistory() {
        return ResponseEntity.ok(replayRunRepository.findAllByOrderByStartedAtDesc());
    }
}
