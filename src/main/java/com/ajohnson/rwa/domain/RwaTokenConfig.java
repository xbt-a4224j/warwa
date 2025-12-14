package com.ajohnson.rwa.domain;

import java.time.Instant;

public class RwaTokenConfig {
    private final String tokenId;
    private final Instant lockupUntil;
    private final boolean accreditedOnly;

    public RwaTokenConfig(String tokenId, Instant lockupUntil, boolean accreditedOnly) {
        this.tokenId = tokenId;
        this.lockupUntil = lockupUntil;
        this.accreditedOnly = accreditedOnly;
    }

    public String getTokenId() { return tokenId; }
    public Instant getLockupUntil() { return lockupUntil; }
    public boolean isAccreditedOnly() { return accreditedOnly; }
}