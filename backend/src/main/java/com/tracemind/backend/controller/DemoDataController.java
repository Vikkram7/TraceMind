package com.tracemind.backend.controller;

import com.tracemind.backend.service.DemoDataGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo")
@Tag(name = "Demo Data", description = "Sample Telemetry and Fault Dataset Generator")
public class DemoDataController {

    private final DemoDataGeneratorService demoDataService;

    public DemoDataController(DemoDataGeneratorService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate realistic multi-service distributed telemetry dataset labeled as DEMO")
    public ResponseEntity<Map<String, Object>> generateDemoData() {
        return ResponseEntity.ok(demoDataService.generateDemoScenarioSet());
    }
}
