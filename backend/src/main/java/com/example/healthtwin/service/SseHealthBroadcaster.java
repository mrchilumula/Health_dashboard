package com.example.healthtwin.service;

import com.example.healthtwin.entity.HealthRecord;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseHealthBroadcaster {
    private static final Logger log = LoggerFactory.getLogger(SseHealthBroadcaster.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(600_000L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((err) -> emitters.remove(emitter));

        return emitter;
    }

    public void broadcast(HealthRecord record) {
        if (emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("health-record")
                        .data(record));
            } catch (IOException e) {
                log.warn("Failed to deliver SSE update for user {}", record.getUserId(), e);
                emitters.remove(emitter);
            }
        }
    }
}
