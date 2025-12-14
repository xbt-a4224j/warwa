package com.ajohnson.rwa.domain;

public class ClientProfile {
    private final String clientId;
    private final boolean accredited;

    public ClientProfile(String clientId, boolean accredited) {
        this.clientId = clientId;
        this.accredited = accredited;
    }

    public String getClientId() { return clientId; }
    public boolean isAccredited() { return accredited; }
}