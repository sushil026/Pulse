package com.pulse.pulse_api.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.pulse_api.api.dto.TrackEventRequest;
import com.pulse.pulse_api.api.dto.TrackEventResponse;
import com.pulse.pulse_api.common.exception.InvalidApiKeyException;
import com.pulse.pulse_api.common.exception.TemplateNotFoundException;
import com.pulse.pulse_api.domain.entity.ApiKey;
import com.pulse.pulse_api.domain.entity.NotificationInstance;
import com.pulse.pulse_api.domain.entity.Outbox;
import com.pulse.pulse_api.domain.repository.ApiKeyRepository;
import com.pulse.pulse_api.domain.repository.NotificationInstanceRepository;
import com.pulse.pulse_api.domain.repository.NotificationTemplateRepository;
import com.pulse.pulse_api.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackEventService {

    private final ApiKeyRepository apiKeyRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationInstanceRepository notificationInstanceRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TrackEventResponse track(String publicKey, TrackEventRequest request) {
        ApiKey apiKey = apiKeyRepository.findByPublicKeyAndActiveTrue(publicKey)
                .orElseThrow(() -> new InvalidApiKeyException("Invalid or inactive API key"));

        UUID appId = apiKey.getApp().getId();

        notificationTemplateRepository.findByAppIdAndTemplateId(appId, request.getTemplateId())
                .orElseThrow(() -> new TemplateNotFoundException("Template not found: " + request.getTemplateId()));

        NotificationInstance instance = notificationInstanceRepository
                .findByAppIdAndInstanceId(appId, request.getInstanceId())
                .orElse(null);

        if (instance == null) {
            instance = NotificationInstance.builder()
                    .app(apiKey.getApp())
                    .templateId(request.getTemplateId())
                    .instanceId(request.getInstanceId())
                    .userId(request.getUserId())
                    .currentState(null)
                    .payload(toJson(request.getPayload()))
                    .build();
        } else {
            instance.setPayload(toJson(request.getPayload()));
        }

        instance = notificationInstanceRepository.save(instance);

        Outbox outbox = Outbox.builder()
                .aggregateId(request.getInstanceId())
                .eventType(request.getEvent())
                .payload(toJson(request))
                .published(false)
                .build();

        outboxRepository.save(outbox);

        return TrackEventResponse.builder()
                .instanceId(request.getInstanceId())
                .status("ACCEPTED")
                .currentState(instance.getCurrentState())
                .build();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }
}
