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

        @NotNull(message = "calories is required")
        @PositiveOrZero(message = "calories must be zero or greater")
        private Double calories;

        @NotNull(message = "distance is required")
        @PositiveOrZero(message = "distance must be zero or greater")
        private Double distance;

        @NotNull(message = "spo2 is required")
        @PositiveOrZero(message = "spo2 must be zero or greater")
        private Double spo2;

        @NotNull(message = "temperature is required")
        @PositiveOrZero(message = "temperature must be zero or greater")
        private Double temperature;

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
