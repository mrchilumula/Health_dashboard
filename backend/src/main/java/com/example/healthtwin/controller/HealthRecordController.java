package com.example.healthtwin.controller;

import com.example.healthtwin.dto.HealthDataRequest;
import com.example.healthtwin.dto.HealthRecordDto;
import com.example.healthtwin.dto.HealthSummaryDto;
import com.example.healthtwin.entity.HealthRecord;
import com.example.healthtwin.service.HealthRecordService;
import com.example.healthtwin.service.SseHealthBroadcaster;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;
    private final SseHealthBroadcaster sseHealthBroadcaster;

    public HealthRecordController(HealthRecordService healthRecordService, SseHealthBroadcaster sseHealthBroadcaster) {
        this.healthRecordService = healthRecordService;
        this.sseHealthBroadcaster = sseHealthBroadcaster;
    }

    @PostMapping("/health-records")
    public ResponseEntity<Map<String, Object>> createHealthRecord(@Valid @RequestBody HealthDataRequest request) {
        HealthRecord saved = healthRecordService.saveHealthData(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Health data received successfully");
        response.put("userId", saved.getUserId());
        response.put("timestamp", saved.getEventTimestamp());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/health-records/latest")
    public ResponseEntity<HealthRecordDto> getLatestHealthRecord(@RequestParam String userId) {
        Optional<HealthRecordDto> latest = healthRecordService.getLatestRecord(userId);
        return latest.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/health-records")
    public ResponseEntity<Map<String, Object>> getHealthRecords(@RequestParam String userId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        List<HealthRecordDto> records = healthRecordService.getAllRecordsForUser(userId, page, size);
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", records);
        payload.put("page", page);
        payload.put("size", size);
        payload.put("userId", userId);
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/health-records/history")
    public ResponseEntity<List<HealthRecordDto>> getHistory(@RequestParam String userId,
                                                           @RequestParam(defaultValue = "24") long hours) {
        return ResponseEntity.ok(healthRecordService.getHistoryByHours(userId, hours));
    }

    @GetMapping("/health-records/summary")
    public ResponseEntity<HealthSummaryDto> getSummary(@RequestParam String userId) {
        return ResponseEntity.ok(healthRecordService.getSummary(userId));
    }

    @GetMapping("/health-records/events")
    public SseEmitter streamHealthEvents() {
        return sseHealthBroadcaster.register();
    }

    @GetMapping("/health-records/status")
    public Map<String, Object> status() {
        Map<String, Object> st = new HashMap<>();
        st.put("status", "ok");
        st.put("timestamp", Instant.now().toEpochMilli());
        return st;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("success", false);
        errors.put("message", "Validation failed");
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        errors.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(errors);
    }
}
