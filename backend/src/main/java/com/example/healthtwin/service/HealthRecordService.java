package com.example.healthtwin.service;

import com.example.healthtwin.dto.HealthDataRequest;
import com.example.healthtwin.dto.HealthRecordDto;
import com.example.healthtwin.dto.HealthSummaryDto;
import com.example.healthtwin.entity.HealthRecord;
import com.example.healthtwin.repository.HealthRecordRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthRecordService {
    private static final Logger log = LoggerFactory.getLogger(HealthRecordService.class);

    private final HealthRecordRepository repository;
    private final SseHealthBroadcaster broadcaster;

    public HealthRecordService(HealthRecordRepository repository, SseHealthBroadcaster broadcaster) {
        this.repository = repository;
        this.broadcaster = broadcaster;
    }

    @Transactional
    public HealthRecord saveHealthData(HealthDataRequest request) {
        String dedupeKey = buildDedupeKey(request);
        Optional<HealthRecord> existing = repository.findByUserIdAndDedupeKey(request.getUserId(), dedupeKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        LocalDateTime eventTime = Instant.ofEpochMilli(request.getTimestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        HealthRecord record = new HealthRecord();
        record.setUserId(request.getUserId());
        record.setPlatform(request.getPlatform());
        record.setEventTimestamp(request.getTimestamp());
        record.setEventTime(eventTime);
        record.setSteps(request.getHealthParameters().getSteps());
        record.setCalories(request.getHealthParameters().getCalories());
        record.setDistance(request.getHealthParameters().getDistance());
        record.setSpo2(request.getHealthParameters().getSpo2());
        record.setTemperature(request.getHealthParameters().getTemperature());
        record.setSyncCount(request.getHealthParameters().getSyncCount());
        record.setDedupeKey(dedupeKey);
        record.setCreatedAt(LocalDateTime.now());

        HealthRecord saved = repository.saveAndFlush(record);
        broadcaster.broadcast(saved);
        return saved;
    }

    public Optional<HealthRecordDto> getLatestRecord(String userId) {
        return repository.findFirstByUserIdOrderByEventTimestampDesc(userId).map(this::toDto);
    }

    public List<HealthRecordDto> getRecentRecords(String userId, int limit) {
        return repository.findTop10ByUserIdOrderByEventTimestampDesc(userId).stream()
                .limit(limit)
                .sorted(Comparator.comparing(HealthRecord::getEventTimestamp).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<HealthRecordDto> getHistory(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventTimestamp"));
        return repository.findByUserIdOrderByEventTimestampDesc(userId, pageable).map(this::toDto);
    }

    public List<HealthRecordDto> getHistoryByHours(String userId, long hours) {
        long cutoff = Instant.now().minusSeconds(hours * 3600L).toEpochMilli();
        return repository.findByUserIdAndEventTimestampAfterOrderByEventTimestampAsc(userId, cutoff)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public HealthSummaryDto getSummary(String userId) {
        List<HealthRecord> records = repository.findTop10ByUserIdOrderByEventTimestampDesc(userId);
        HealthSummaryDto summary = new HealthSummaryDto();
        summary.setUserId(userId);
        summary.setTotalRecords(repository.countByUserId(userId));

        if (records.isEmpty()) {
            return summary;
        }

        List<HealthRecordDto> recentRecords = records.stream().map(this::toDto).collect(Collectors.toList());
        summary.setRecentRecords(recentRecords);
        summary.setLatestSteps(Double.valueOf(recentRecords.get(0).getSteps()));
        summary.setLatestCalories(recentRecords.get(0).getCalories());
        summary.setLatestDistance(recentRecords.get(0).getDistance());
        summary.setLatestSpO2(recentRecords.get(0).getSpo2());
        summary.setLatestTemperature(recentRecords.get(0).getTemperature());

        double avgSteps = records.stream().mapToDouble(r -> r.getSteps()).average().orElse(0.0);
        double avgCalories = records.stream().mapToDouble(r -> r.getCalories()).average().orElse(0.0);
        double avgDistance = records.stream().mapToDouble(r -> r.getDistance()).average().orElse(0.0);
        double avgSpo2 = records.stream().mapToDouble(r -> r.getSpo2()).average().orElse(0.0);
        double avgTemperature = records.stream().mapToDouble(r -> r.getTemperature()).average().orElse(0.0);

        summary.setAverageSteps(avgSteps);
        summary.setAverageCalories(avgCalories);
        summary.setAverageDistance(avgDistance);
        summary.setAverageSpo2(avgSpo2);
        summary.setAverageTemperature(avgTemperature);
        return summary;
    }

    public List<HealthRecordDto> getAllRecordsForUser(String userId, int page, int size) {
        return repository.findByUserIdOrderByEventTimestampDesc(userId, PageRequest.of(page, size)).stream().map(this::toDto).collect(Collectors.toList());
    }

    private HealthRecordDto toDto(HealthRecord record) {
        HealthRecordDto dto = new HealthRecordDto();
        dto.setId(record.getId());
        dto.setUserId(record.getUserId());
        dto.setPlatform(record.getPlatform());
        dto.setEventTimestamp(record.getEventTimestamp());
        dto.setEventTime(record.getEventTime());
        dto.setSteps(record.getSteps());
        dto.setCalories(record.getCalories());
        dto.setDistance(record.getDistance());
        dto.setSpo2(record.getSpo2());
        dto.setTemperature(record.getTemperature());
        dto.setSyncCount(record.getSyncCount());
        return dto;
    }

    private String buildDedupeKey(HealthDataRequest request) {
        String raw = request.getUserId() + "|" + request.getPlatform() + "|" + request.getTimestamp() + "|" +
                request.getHealthParameters().getSteps() + "|" + request.getHealthParameters().getCalories() + "|" +
                request.getHealthParameters().getDistance() + "|" + request.getHealthParameters().getSpo2() + "|" +
                request.getHealthParameters().getTemperature() + "|" + request.getHealthParameters().getSyncCount();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Unable to build dedupe key", e);
            return raw;
        }
    }
}
