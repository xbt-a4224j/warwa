package com.ajohnson.rwa.service;

import com.ajohnson.rwa.domain.EventReason;
import com.ajohnson.rwa.domain.EventType;
import com.ajohnson.rwa.domain.ClientProfile;
import com.ajohnson.rwa.domain.LedgerEvent;
import com.ajohnson.rwa.domain.RwaTokenConfig;
import com.ajohnson.rwa.service.ExplanationService;
import com.ajohnson.rwa.ledger.JsonlLedgerStore;

import java.time.Instant;
import java.util.*;

public class TokenLedgerService {

    private final JsonlLedgerStore ledgerStore;
    private final ExplanationService explanationService;

    /**
     * balances[tokenId][clientId] = amount
     */
    private final Map<String, Map<String, Integer>> balances = new HashMap<>();

    /**
     * Reference data (in-memory for prototype scope)
     */
    private final Map<String, RwaTokenConfig> tokenConfigById = new HashMap<>();
    private final Map<String, ClientProfile> clientById = new HashMap<>();

    public TokenLedgerService(JsonlLedgerStore ledgerStore,
                              ExplanationService explanationService) {
        this.ledgerStore = ledgerStore;
        this.explanationService = explanationService;
        replay();
    }

    // ------------------------------------------------------------------
    // Reference data registration (bootstrap / admin only)
    // ------------------------------------------------------------------

    public void upsertTokenConfig(RwaTokenConfig config) {
        tokenConfigById.put(config.getTokenId(), config);
    }

    public void upsertClient(ClientProfile client) {
        clientById.put(client.getClientId(), client);
    }

    // ------------------------------------------------------------------
    // Replay + state derivation
    // ------------------------------------------------------------------

    private void replay() {
        List<LedgerEvent> events = ledgerStore.readAll();
        events.forEach(this::apply);
    }

    private void apply(LedgerEvent event) {
        switch (event.getType()) {
            case TOKENS_MINTED -> applyMint(event);
            case TOKEN_TRANSFER_APPROVED -> applyTransfer(event);
            default -> {
                // Requests, rejections, RWA_CREATED do not affect balances
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

    // ------------------------------------------------------------------
    // Public read APIs
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Transfer flow (deterministic, auditable)
    // ------------------------------------------------------------------

    /**
     * Core transfer request.
     * Emits REQUESTED + APPROVED or REJECTED events.
     */
    public LedgerEvent requestTransfer(
            String tokenId,
            String fromClient,
            String toClient,
            int amount
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        // 1) Record intent (audit trail)
        LedgerEvent requested = new LedgerEvent(
                EventType.TOKEN_TRANSFER_REQUESTED,
                tokenId,
                Map.of(
                        "fromClient", fromClient,
                        "toClient", toClient,
                        "amount", amount
                )
        );
        ledgerStore.append(requested);

        // 2) Evaluate rules in locked order
        EventReason rejection = evaluateRules(
                tokenId, fromClient, toClient, amount, Instant.now()
        );

        if (rejection != null) {
            LedgerEvent rejected = new LedgerEvent(
                    EventType.TOKEN_TRANSFER_REJECTED,
                    tokenId,
                    Map.of(
                            "fromClient", fromClient,
                            "toClient", toClient,
                            "amount", amount,
                            "reason", rejection.name()
                    )
            );
            ledgerStore.append(rejected);
            return rejected;
        }

        // 3) Approve and apply
        LedgerEvent approved = new LedgerEvent(
                EventType.TOKEN_TRANSFER_APPROVED,
                tokenId,
                Map.of(
                        "fromClient", fromClient,
                        "toClient", toClient,
                        "amount", amount
                )
        );
        ledgerStore.append(approved);
        apply(approved); // immediate in-memory update

        return approved;
    }

    /**
     * Convenience wrapper used by UI / API:
     * executes transfer and returns advisor-friendly explanation.
     */
    public String requestTransferWithExplanation(
            String tokenId,
            String fromClient,
            String toClient,
            int amount
    ) {
        LedgerEvent outcome =
                requestTransfer(tokenId, fromClient, toClient, amount);

        List<LedgerEvent> context =
                ledgerStore.readByToken(tokenId);

        return explanationService.explainTransferOutcome(
                tokenId,
                outcome,
                context
        );
    }

    public void mintToAll(String tokenId, int amount) {

        Set<String> clients = balancesForToken(tokenId).keySet();

        for (String client : clients) {
            LedgerEvent e = new LedgerEvent();
            e.setEventId(UUID.randomUUID());
            e.setTimestamp(Instant.now());
            e.setType(EventType.TOKENS_MINTED);
            e.setTokenId(tokenId);

            e.getData().put("toClient", client);
            e.getData().put("amount", amount);

            ledgerStore.append(e);
            balances
                    .computeIfAbsent(tokenId, k -> new HashMap<>())
                    .merge(client, amount, Integer::sum);

        }
    }


    private EventReason evaluateRules(
            String tokenId,
            String fromClient,
            String toClient,
            int amount,
            Instant now
    ) {
        // Rule 1: Lockup window
        RwaTokenConfig cfg = tokenConfigById.get(tokenId);
        if (cfg != null
                && cfg.getLockupUntil() != null
                && now.isBefore(cfg.getLockupUntil())) {
            return EventReason.LOCKUP_NOT_EXPIRED;
        }

        // Rule 2: Accreditation
        if (cfg != null && cfg.isAccreditedOnly()) {
            ClientProfile from = clientById.get(fromClient);
            ClientProfile to = clientById.get(toClient);

            // Safe default: missing profile = not accredited
            if (from == null || !from.isAccredited()) {
                return EventReason.NOT_ACCREDITED;
            }
            if (to == null || !to.isAccredited()) {
                return EventReason.NOT_ACCREDITED;
            }
        }

        // Rule 3: Sufficient balance
        int fromBalance = balanceOf(tokenId, fromClient);
        if (fromBalance < amount) {
            return EventReason.INSUFFICIENT_BALANCE;
        }

        return null;
    }
}