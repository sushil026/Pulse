package com.pulse.pulse_api.api.controller;

import com.pulse.pulse_api.api.dto.TrackEventRequest;
import com.pulse.pulse_api.api.dto.TrackEventResponse;
import com.pulse.pulse_api.application.service.TrackEventService;
import com.pulse.pulse_api.common.exception.InvalidApiKeyException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final TrackEventService trackEventService;

    @PostMapping("/track")
    public ResponseEntity<TrackEventResponse> track(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody @Valid TrackEventRequest request) {

        if (authorization == null || !authorization.startsWith("Bearer pk_")) {
            throw new InvalidApiKeyException("Missing or invalid Authorization header");
        }

        String publicKey = authorization.substring("Bearer ".length());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(trackEventService.track(publicKey, request));
    }
}
