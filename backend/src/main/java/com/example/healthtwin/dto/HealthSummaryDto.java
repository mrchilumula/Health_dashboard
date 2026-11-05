package com.example.healthtwin.dto;

import java.util.List;

public class HealthSummaryDto {
    private String userId;
    private long totalRecords;
    private Double averageSteps;
    private Double averageCalories;
    private Double averageDistance;
    private Double averageSpo2;
    private Double averageTemperature;
    private Double latestSpO2;
    private Double latestTemperature;
    private Double latestSteps;
    private Double latestCalories;
    private Double latestDistance;
    private List<HealthRecordDto> recentRecords;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Double getAverageSteps() {
        return averageSteps;
    }

    public void setAverageSteps(Double averageSteps) {
        this.averageSteps = averageSteps;
    }

    public Double getAverageCalories() {
        return averageCalories;
    }

    public void setAverageCalories(Double averageCalories) {
        this.averageCalories = averageCalories;
    }

    public Double getAverageDistance() {
        return averageDistance;
    }

    public void setAverageDistance(Double averageDistance) {
        this.averageDistance = averageDistance;
    }

    public Double getAverageSpo2() {
        return averageSpo2;
    }

    public void setAverageSpo2(Double averageSpo2) {
        this.averageSpo2 = averageSpo2;
    }

    public Double getAverageTemperature() {
        return averageTemperature;
    }

    public void setAverageTemperature(Double averageTemperature) {
        this.averageTemperature = averageTemperature;
    }

    public Double getLatestSpO2() {
        return latestSpO2;
    }

    public void setLatestSpO2(Double latestSpO2) {
        this.latestSpO2 = latestSpO2;
    }

    public Double getLatestTemperature() {
        return latestTemperature;
    }

    public void setLatestTemperature(Double latestTemperature) {
        this.latestTemperature = latestTemperature;
    }

    public Double getLatestSteps() {
        return latestSteps;
    }

    public void setLatestSteps(Double latestSteps) {
        this.latestSteps = latestSteps;
    }

    public Double getLatestCalories() {
        return latestCalories;
    }

    public void setLatestCalories(Double latestCalories) {
        this.latestCalories = latestCalories;
    }

    public Double getLatestDistance() {
        return latestDistance;
    }

    public void setLatestDistance(Double latestDistance) {
        this.latestDistance = latestDistance;
    }

    public List<HealthRecordDto> getRecentRecords() {
        return recentRecords;
    }

    public void setRecentRecords(List<HealthRecordDto> recentRecords) {
        this.recentRecords = recentRecords;
    }
}
