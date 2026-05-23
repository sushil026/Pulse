package com.pulse.pulse_api.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackEventRequest {

    @NotBlank
    private String templateId;

    @NotBlank
    private String instanceId;

    @NotBlank
    private String userId;

    @NotBlank
    private String event;

    private Map<String, Object> payload;
}
