package com.example.healthtwin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.healthtwin.dto.HealthDataRequest;
import com.example.healthtwin.entity.HealthRecord;
import com.example.healthtwin.repository.HealthRecordRepository;
import com.example.healthtwin.service.HealthRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class HealthRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HealthRecordService service;

    @Autowired
    private HealthRecordRepository repository;

    @Test
    void shouldAcceptValidPayloadAndPersist() throws Exception {
        String payload = """
            {
              "userId": "user_samsung_watch9_twin",
              "platform": "Samsung Galaxy Watch 9 & Galaxy Phone",
              "timestamp": 1793200661908,
              "healthParameters": {
                "steps": 1250,
                "calories": 180.5,
                "distance": 950.0,
                "spo2": 98.0,
                "temperature": 36.6,
                "syncCount": 5
              }
            }
            """;

        mockMvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userId").value("user_samsung_watch9_twin"));

        assert repository.countByUserId("user_samsung_watch9_twin") >= 1;
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        String payload = """
            {
              "userId": "user_samsung_watch9_twin",
              "platform": "Samsung Galaxy Watch 9 & Galaxy Phone",
              "timestamp": 1793200661908,
              "healthParameters": {
                "steps": -1,
                "calories": 180.5,
                "distance": 950.0,
                "spo2": 98.0,
                "temperature": 36.6,
                "syncCount": 5
              }
            }
            """;

        mockMvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnLatestRecordForUser() throws Exception {
        HealthDataRequest request = new HealthDataRequest();
        request.setUserId("user_samsung_watch9_twin");
        request.setPlatform("Samsung Galaxy Watch 9 & Galaxy Phone");
        request.setTimestamp(1793200661908L);
        HealthDataRequest.HealthParameters hp = new HealthDataRequest.HealthParameters();
        hp.setSteps(1250);
        hp.setCalories(180.5);
        hp.setDistance(950.0);
        hp.setSpo2(98.0);
        hp.setTemperature(36.6);
        hp.setSyncCount(5);
        request.setHealthParameters(hp);

        service.saveHealthData(request);

        mockMvc.perform(get("/api/health-records/latest")
                        .param("userId", "user_samsung_watch9_twin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user_samsung_watch9_twin"));
    }

    @Test
    void shouldDeduplicateRetryPayload() {
        HealthDataRequest request = new HealthDataRequest();
        request.setUserId("user_samsung_watch9_twin");
        request.setPlatform("Samsung Galaxy Watch 9 & Galaxy Phone");
        request.setTimestamp(1793200661908L);
        HealthDataRequest.HealthParameters hp = new HealthDataRequest.HealthParameters();
        hp.setSteps(1000);
        hp.setCalories(150.0);
        hp.setDistance(800.0);
        hp.setSpo2(97.0);
        hp.setTemperature(36.4);
        hp.setSyncCount(1);
        request.setHealthParameters(hp);

        HealthRecord first = service.saveHealthData(request);
        HealthRecord second = service.saveHealthData(request);
        assert first.getId().equals(second.getId());
    }
}
