package com.pulse.pulse_api.api.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackEventResponse {

    private String instanceId;
    private String status;
    private String currentState;
}
