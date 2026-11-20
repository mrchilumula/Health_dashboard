package com.example.healthbridge.data

data class HealthParameters(
    val steps: Int,
    val calories: Double?,
    val distance: Double,
    val spo2: Double?,
    val temperature: Double?,
    val heartRate: Double?,
    val restingHeartRate: Double?,
    val hrvRmssd: Double?,
    val respiratoryRate: Double?,
    val systolicBloodPressure: Double?,
    val diastolicBloodPressure: Double?,
    val bloodGlucose: Double?,
    val weightKg: Double?,
    val heightM: Double?,
    val floorsClimbed: Double?,
    val hydrationLiters: Double?,
    val sleepHours: Double?,
    val exerciseMinutes: Double?,
    val syncCount: Int
)

data class HealthDataRequest(
    val userId: String,
    val platform: String,
    val timestamp: Long,
    val healthParameters: HealthParameters
)

data class UploadResponse(
    val success: Boolean?,
    val message: String?,
    val userId: String?,
    val timestamp: Long?
)
