package com.ajohnson.rwa.config;

import com.ajohnson.rwa.domain.EventType;
import com.ajohnson.rwa.domain.ClientProfile;
import com.ajohnson.rwa.domain.LedgerEvent;
import com.ajohnson.rwa.domain.RwaTokenConfig;
import com.ajohnson.rwa.ledger.JsonlLedgerStore;
import com.ajohnson.rwa.service.TokenLedgerService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
public class RwaBootstrap {

    private final JsonlLedgerStore ledgerStore;
    private final TokenLedgerService ledgerService;

    public RwaBootstrap(JsonlLedgerStore ledgerStore,
                        TokenLedgerService ledgerService) {
        this.ledgerStore = ledgerStore;
        this.ledgerService = ledgerService;
    }

    @PostConstruct
    public void init() {
        // Only bootstrap if ledger is empty
        List<LedgerEvent> existing = ledgerStore.readAll();
        if (!existing.isEmpty()) {
            return;
        }

        bootstrapReferenceData();
        bootstrapLedgerEvents();
    }

    private void bootstrapReferenceData() {
        // Token config: accredited-only, lockup in the future
        ledgerService.upsertTokenConfig(
                new RwaTokenConfig(
                        "PCI-A",
                        Instant.now().plusSeconds(60 * 60 * 24 * 30), // 30 days
                        true
                )
        );

        // Clients
        ledgerService.upsertClient(new ClientProfile("Alice", true));
        ledgerService.upsertClient(new ClientProfile("Bob", true));
        ledgerService.upsertClient(new ClientProfile("Charlie", false)); // non-accredited
    }

    private void bootstrapLedgerEvents() {
        // RWA onboarded
        ledgerStore.append(new LedgerEvent(
                EventType.RWA_CREATED,
                "PCI-A",
                Map.of(
                        "legalName", "Private Credit Fund Series A",
                        "jurisdiction", "US-NY",
                        "authorizedSupply", 1_000
                )
        ));

        // Initial mint
        ledgerStore.append(new LedgerEvent(
                EventType.TOKENS_MINTED,
                "PCI-A",
                Map.of(
                        "toClient", "Alice",
                        "amount", 1_000
                )
        ));
    }
}