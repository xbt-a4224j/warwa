package com.ajohnson.rwa.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
public class LedgerEvent {

    private UUID eventId;
    private Instant timestamp;
    private EventType type;
    private String tokenId;
    private Map<String, Object> data = new HashMap<>();

    public LedgerEvent(EventType type) {
        this.eventId = UUID.randomUUID();
        this.type = type;
    }


    public LedgerEvent(EventType eventType, String tokenId, Map<String, Object> data) {
        this.eventId = UUID.randomUUID();
        this.type = eventType;
        this.tokenId = tokenId;
        this.data = data;
    }
}
