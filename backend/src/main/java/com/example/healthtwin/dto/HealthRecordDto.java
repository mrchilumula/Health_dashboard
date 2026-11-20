package com.example.healthtwin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class HealthRecordDto {
    private Long id;
    private String userId;
    private String platform;
    private Long eventTimestamp;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private Integer steps;
    private Double calories;
    private Double distance;
    private Double spo2;
    private Double temperature;
    private Double heartRate;
    private Double restingHeartRate;
    private Double hrvRmssd;
    private Double respiratoryRate;
    private Double systolicBloodPressure;
    private Double diastolicBloodPressure;
    private Double bloodGlucose;
    private Double weightKg;
    private Double heightM;
    private Double floorsClimbed;
    private Double hydrationLiters;
    private Double sleepHours;
    private Double exerciseMinutes;
    private Integer syncCount;

    public HealthRecordDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Long getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Long eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    public Double getCalories() {
        return calories;
    }

    public void setCalories(Double calories) {
        this.calories = calories;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Double getSpo2() {
        return spo2;
    }

    public void setSpo2(Double spo2) {
        this.spo2 = spo2;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHeartRate() { return heartRate; }
    public void setHeartRate(Double heartRate) { this.heartRate = heartRate; }
    public Double getRestingHeartRate() { return restingHeartRate; }
    public void setRestingHeartRate(Double restingHeartRate) { this.restingHeartRate = restingHeartRate; }
    public Double getHrvRmssd() { return hrvRmssd; }
    public void setHrvRmssd(Double hrvRmssd) { this.hrvRmssd = hrvRmssd; }
    public Double getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(Double respiratoryRate) { this.respiratoryRate = respiratoryRate; }
    public Double getSystolicBloodPressure() { return systolicBloodPressure; }
    public void setSystolicBloodPressure(Double systolicBloodPressure) { this.systolicBloodPressure = systolicBloodPressure; }
    public Double getDiastolicBloodPressure() { return diastolicBloodPressure; }
    public void setDiastolicBloodPressure(Double diastolicBloodPressure) { this.diastolicBloodPressure = diastolicBloodPressure; }
    public Double getBloodGlucose() { return bloodGlucose; }
    public void setBloodGlucose(Double bloodGlucose) { this.bloodGlucose = bloodGlucose; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public Double getHeightM() { return heightM; }
    public void setHeightM(Double heightM) { this.heightM = heightM; }
    public Double getFloorsClimbed() { return floorsClimbed; }
    public void setFloorsClimbed(Double floorsClimbed) { this.floorsClimbed = floorsClimbed; }
    public Double getHydrationLiters() { return hydrationLiters; }
    public void setHydrationLiters(Double hydrationLiters) { this.hydrationLiters = hydrationLiters; }
    public Double getSleepHours() { return sleepHours; }
    public void setSleepHours(Double sleepHours) { this.sleepHours = sleepHours; }
    public Double getExerciseMinutes() { return exerciseMinutes; }
    public void setExerciseMinutes(Double exerciseMinutes) { this.exerciseMinutes = exerciseMinutes; }

    public Integer getSyncCount() {
        return syncCount;
    }

    public void setSyncCount(Integer syncCount) {
        this.syncCount = syncCount;
    }
}
