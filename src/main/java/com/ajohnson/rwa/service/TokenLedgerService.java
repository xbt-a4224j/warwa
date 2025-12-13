package com.ajohnson.rwa.service;

import com.ajohnson.rwa.domain.EventType;
import com.ajohnson.rwa.domain.LedgerEvent;
import com.ajohnson.rwa.ledger.JsonlLedgerStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenLedgerService {

    private final JsonlLedgerStore ledgerStore;

    // balances[tokenId][clientId] = amount
    private final Map<String, Map<String, Integer>> balances = new HashMap<>();

    public TokenLedgerService(JsonlLedgerStore ledgerStore) {
        this.ledgerStore = ledgerStore;
        replay();
    }

    private void replay() {
        List<LedgerEvent> events = ledgerStore.readAll();
        events.forEach(this::apply);
    }

    private void apply(LedgerEvent event) {
        switch (event.getType()) {
            case TOKENS_MINTED -> applyMint(event);
            case TOKEN_TRANSFER_APPROVED -> applyTransfer(event);
            default -> {
                // ignore other events for balance derivation
            }
        }
    }

    private void applyMint(LedgerEvent event) {
        String tokenId = event.getTokenId();
        String toClient = (String) event.getData().get("toClient");
        Integer amount = (Integer) event.getData().get("amount");

        balances
                .computeIfAbsent(tokenId, k -> new HashMap<>())
                .merge(toClient, amount, Integer::sum);
    }

    private void applyTransfer(LedgerEvent event) {
        String tokenId = event.getTokenId();
        String fromClient = (String) event.getData().get("fromClient");
        String toClient = (String) event.getData().get("toClient");
        Integer amount = (Integer) event.getData().get("amount");

        Map<String, Integer> tokenBalances =
                balances.computeIfAbsent(tokenId, k -> new HashMap<>());

        tokenBalances.merge(fromClient, -amount, Integer::sum);
        tokenBalances.merge(toClient, amount, Integer::sum);
    }

    // -------- Public read APIs --------

    public int balanceOf(String tokenId, String clientId) {
        return balances
                .getOrDefault(tokenId, Map.of())
                .getOrDefault(clientId, 0);
    }

    public Map<String, Integer> balancesForToken(String tokenId) {
        return Map.copyOf(
                balances.getOrDefault(tokenId, Map.of())
        );
    }

    public Map<String, Map<String, Integer>> snapshot() {
        return Map.copyOf(balances);
    }
}