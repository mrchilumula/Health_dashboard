package com.example.healthtwin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class HealthDataRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "platform is required")
    private String platform;

    @NotNull(message = "timestamp is required")
    private Long timestamp;

    @NotNull(message = "healthParameters are required")
    @Valid
    @JsonProperty("healthParameters")
    private HealthParameters healthParameters;

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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public HealthParameters getHealthParameters() {
        return healthParameters;
    }

    public void setHealthParameters(HealthParameters healthParameters) {
        this.healthParameters = healthParameters;
    }

    public static class HealthParameters {
        @NotNull(message = "steps is required")
        @PositiveOrZero(message = "steps must be zero or greater")
        private Integer steps;

        @PositiveOrZero(message = "calories must be zero or greater")
        private Double calories;

        @NotNull(message = "distance is required")
        @PositiveOrZero(message = "distance must be zero or greater")
        private Double distance;

        @PositiveOrZero(message = "spo2 must be zero or greater")
        private Double spo2;

        @PositiveOrZero(message = "temperature must be zero or greater")
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

        @NotNull(message = "syncCount is required")
        @PositiveOrZero(message = "syncCount must be zero or greater")
        private Integer syncCount;

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

        public void setDistance(Double distance) {
            this.distance = distance;
        }
    }
}
