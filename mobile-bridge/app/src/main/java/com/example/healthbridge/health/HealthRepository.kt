package com.example.healthbridge.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.healthbridge.data.HealthDataRequest
import com.example.healthbridge.data.HealthParameters
import com.example.healthbridge.data.HealthDatabase
import com.example.healthbridge.data.OutboxItem
import com.example.healthbridge.data.Preferences
import com.example.healthbridge.data.HealthApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.math.BigInteger
import java.security.MessageDigest
import java.time.Instant

data class Preview(
    val steps: Int?,
    val calories: Double?,
    val distance: Double?,
    val spo2: Double?,
    val temperature: Double?,
    val recordCounts: Map<String, Int>,
    val exposedValues: Map<String, Double?>
)

class HealthRepository(
    private val context: Context,
    private val database: HealthDatabase,
    private val preferences: Preferences
) {
    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(FloorsClimbedRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    fun isHealthConnectAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun grantedPermissions(): Set<String> =
        if (!isHealthConnectAvailable()) emptySet()
        else HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()

    suspend fun preview(): Preview {
        if (!isHealthConnectAvailable()) return Preview(null, null, null, null, null, emptyMap(), emptyMap())
        val client = HealthConnectClient.getOrCreate(context)
        val granted = grantedPermissions()
        val end = Instant.now()
        val start = end.minusSeconds(24 * 60 * 60)
        val filter = TimeRangeFilter.between(start, end)

        var steps: Int? = null
        var calories: Double? = null
        var distance: Double? = null
        var spo2: Double? = null
        var temperature: Double? = null
        val counts = mutableMapOf<String, Int>()
        val exposedValues = mutableMapOf<String, Double?>()

        if (HealthPermission.getReadPermission(StepsRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(StepsRecord::class, filter)).records
            counts["Steps"] = records.size
            val aggregate = client.aggregate(
                AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), filter)
            )[StepsRecord.COUNT_TOTAL]
            if (aggregate != null) steps = aggregate.toInt()
        }
        if (HealthPermission.getReadPermission(DistanceRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(DistanceRecord::class, filter)).records
            counts["Distance"] = records.size
            val aggregate = client.aggregate(
                AggregateRequest(setOf(DistanceRecord.DISTANCE_TOTAL), filter)
            )[DistanceRecord.DISTANCE_TOTAL]
            if (aggregate != null) distance = aggregate.inMeters
        }
        if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, filter)).records
            counts["Active calories"] = records.size
            val aggregate = client.aggregate(
                AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), filter)
            )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            if (aggregate != null) calories = aggregate.inKilocalories
        }
        if (HealthPermission.getReadPermission(OxygenSaturationRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, filter)).records
            counts["Oxygen saturation"] = records.size
            if (records.isNotEmpty()) spo2 = records.maxByOrNull { it.time }?.percentage?.value
        }
        if (HealthPermission.getReadPermission(BodyTemperatureRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(BodyTemperatureRecord::class, filter)).records
            counts["Body temperature"] = records.size
            if (records.isNotEmpty()) temperature = records.maxByOrNull { it.time }?.temperature?.inCelsius
        }
        if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, filter)).records
            counts["Heart rate"] = records.size
            val samples = records.flatMap { it.samples }
            if (samples.isNotEmpty()) exposedValues["Heart rate (bpm)"] =
                samples.map { it.beatsPerMinute }.average()
        }
        if (HealthPermission.getReadPermission(RestingHeartRateRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, filter)).records
            counts["Resting heart rate"] = records.size
            exposedValues["Resting heart rate (bpm)"] =
                records.maxByOrNull { it.time }?.beatsPerMinute?.toDouble()
        }
        if (HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, filter)).records
            counts["HRV RMSSD"] = records.size
            exposedValues["HRV RMSSD (ms)"] =
                records.maxByOrNull { it.time }?.heartRateVariabilityMillis
        }
        if (HealthPermission.getReadPermission(RespiratoryRateRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(RespiratoryRateRecord::class, filter)).records
            counts["Respiratory rate"] = records.size
            exposedValues["Respiratory rate (breaths/min)"] =
                records.maxByOrNull { it.time }?.rate
        }
        if (HealthPermission.getReadPermission(BloodPressureRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(BloodPressureRecord::class, filter)).records
            counts["Blood pressure"] = records.size
            records.maxByOrNull { it.time }?.let {
                exposedValues["Systolic (mmHg)"] = it.systolic.inMillimetersOfMercury
                exposedValues["Diastolic (mmHg)"] = it.diastolic.inMillimetersOfMercury
            }
        }
        if (HealthPermission.getReadPermission(BloodGlucoseRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(BloodGlucoseRecord::class, filter)).records
            counts["Blood glucose"] = records.size
            exposedValues["Blood glucose (mmol/L)"] =
                records.maxByOrNull { it.time }?.level?.inMillimolesPerLiter
        }
        if (HealthPermission.getReadPermission(WeightRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(WeightRecord::class, filter)).records
            counts["Weight"] = records.size
            exposedValues["Weight (kg)"] = records.maxByOrNull { it.time }?.weight?.inKilograms
        }
        if (HealthPermission.getReadPermission(HeightRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(HeightRecord::class, filter)).records
            counts["Height"] = records.size
            exposedValues["Height (m)"] = records.maxByOrNull { it.time }?.height?.inMeters
        }
        if (HealthPermission.getReadPermission(FloorsClimbedRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(FloorsClimbedRecord::class, filter)).records
            counts["Floors climbed"] = records.size
            if (records.isNotEmpty()) exposedValues["Floors"] = records.sumOf { it.floors }
        }
        if (HealthPermission.getReadPermission(HydrationRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(HydrationRecord::class, filter)).records
            counts["Hydration"] = records.size
            if (records.isNotEmpty()) exposedValues["Hydration (L)"] =
                records.sumOf { it.volume.inLiters }
        }
        if (HealthPermission.getReadPermission(SleepSessionRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, filter)).records
            counts["Sleep"] = records.size
            if (records.isNotEmpty()) exposedValues["Sleep (hours)"] =
                records.sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }.toDouble() / 60.0
        }
        if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted) {
            val records = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, filter)).records
            counts["Exercise"] = records.size
            if (records.isNotEmpty()) exposedValues["Exercise (minutes)"] =
                records.sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }.toDouble()
        }
        preferences.lastHealthReadMillis = end.toEpochMilli()
        return Preview(steps, calories, distance, spo2, temperature, counts, exposedValues)
    }

    suspend fun synchronize(): Result<String> {
        if (!isHealthConnectAvailable()) return Result.failure(IllegalStateException("Health Connect is unavailable"))
        val preview = preview()
        val missing = listOf(
            "steps" to preview.steps,
            "distance" to preview.distance,
            "spo2" to preview.spo2
        ).filter { it.second == null }.map { it.first }
        if (missing.isNotEmpty()) {
            val message = "Cannot upload: missing ${missing.joinToString()}"
            preferences.lastError = message
            return Result.failure(IllegalStateException(message))
        }

        preferences.syncCount += 1
        val timestamp = System.currentTimeMillis()
        val parameters = HealthParameters(
            steps = preview.steps!!,
            calories = preview.calories,
            distance = preview.distance!!,
            spo2 = preview.spo2,
            temperature = preview.temperature,
            heartRate = preview.exposedValues["Heart rate (bpm)"],
            restingHeartRate = preview.exposedValues["Resting heart rate (bpm)"],
            hrvRmssd = preview.exposedValues["HRV RMSSD (ms)"],
            respiratoryRate = preview.exposedValues["Respiratory rate (breaths/min)"],
            systolicBloodPressure = preview.exposedValues["Systolic (mmHg)"],
            diastolicBloodPressure = preview.exposedValues["Diastolic (mmHg)"],
            bloodGlucose = preview.exposedValues["Blood glucose (mmol/L)"],
            weightKg = preview.exposedValues["Weight (kg)"],
            heightM = preview.exposedValues["Height (m)"],
            floorsClimbed = preview.exposedValues["Floors"],
            hydrationLiters = preview.exposedValues["Hydration (L)"],
            sleepHours = preview.exposedValues["Sleep (hours)"],
            exerciseMinutes = preview.exposedValues["Exercise (minutes)"],
            syncCount = preferences.syncCount
        )
        val request = HealthDataRequest(
            userId = preferences.userId,
            platform = "Samsung Galaxy Watch 9 & Galaxy Phone",
            timestamp = timestamp,
            healthParameters = parameters
        )
        val json = moshiAdapter().toJson(request)
        val eventId = stableEventId(preview, request)
        database.dao().insert(
            OutboxItem(
                eventId = eventId,
                userId = request.userId,
                payloadJson = json,
                createdAtMillis = timestamp,
                attemptedAtMillis = null,
                attemptCount = 0,
                status = "PENDING",
                lastError = null
            )
        )
        return uploadPending()
    }

    suspend fun uploadPending(): Result<String> {
        val api = createApi(preferences.baseUrl)
        var uploaded = 0
        var lastFailure: Result<String>? = null

        while (true) {
            val item = database.dao().firstPending() ?: break
            val now = System.currentTimeMillis()
            val request: HealthDataRequest? = try {
                moshiAdapter().fromJson(item.payloadJson)
            } catch (error: Exception) {
                database.dao().markFailed(item.eventId, now, "Invalid local payload")
                preferences.lastError = "Invalid local payload"
                lastFailure = Result.failure(error)
                break
            }
            if (request == null) {
                val error = IllegalStateException("Invalid local payload")
                database.dao().markFailed(item.eventId, now, "Invalid local payload")
                preferences.lastError = "Invalid local payload"
                lastFailure = Result.failure(error)
                break
            }

            try {
                val response = api.upload(request)
                if (response.isSuccessful) {
                    database.dao().markUploaded(item.eventId, now)
                    preferences.lastSuccessfulUploadMillis = now
                    preferences.lastError = null
                    uploaded++
                } else {
                    val message = httpError(response.code(), response.message())
                    database.dao().markFailed(item.eventId, now, message)
                    preferences.lastError = message
                    lastFailure = Result.failure(IllegalStateException(message))
                    break
                }
            } catch (error: Exception) {
                val message = error.message?.take(180) ?: "Network failure"
                database.dao().markFailed(item.eventId, now, message)
                preferences.lastError = message
                lastFailure = Result.failure(error)
                break
            }
        }

        return lastFailure ?: if (uploaded == 0) {
            Result.success("No pending uploads")
        } else {
            Result.success("Uploaded $uploaded item(s)")
        }
    }

    fun stableEventId(preview: Preview, request: HealthDataRequest): String {
        val source = "${request.userId}|${preview.steps}|${preview.calories}|${preview.distance}|${preview.spo2}|${preview.temperature}|${request.timestamp / 60000}"
        return sha256(source)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return String.format("%064x", BigInteger(1, digest))
    }

    private fun createApi(baseUrl: String): HealthApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        return Retrofit.Builder()
            .baseUrl(Preferences.normalizeUrl(baseUrl))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HealthApi::class.java)
    }

    private fun moshiAdapter(): com.squareup.moshi.JsonAdapter<HealthDataRequest> =
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(HealthDataRequest::class.java)

    private fun httpError(code: Int, fallback: String): String = when (code) {
        400 -> "HTTP 400 validation failed"
        401, 403 -> "HTTP $code authentication denied"
        409 -> "HTTP 409 duplicate/conflict"
        429 -> "HTTP 429 rate limited"
        in 500..599 -> "HTTP $code server failure"
        else -> "HTTP $code $fallback"
    }
}
