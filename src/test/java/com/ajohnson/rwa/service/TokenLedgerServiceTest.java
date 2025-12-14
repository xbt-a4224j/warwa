package com.ajohnson.rwa.service;

import com.ajohnson.rwa.domain.ClientProfile;
import com.ajohnson.rwa.domain.EventType;
import com.ajohnson.rwa.domain.LedgerEvent;
import com.ajohnson.rwa.domain.RwaTokenConfig;
import com.ajohnson.rwa.ledger.JsonlLedgerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ActiveProfiles("test")
@Configuration
@ConditionalOnProperty(
        name = "onchain.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class TokenLedgerServiceTest {


    private JsonlLedgerStore store;
    private TokenLedgerService service;
    private ExplanationService explanationService;

    @BeforeEach
    void setUp() {
        store = new JsonlLedgerStore("data/test-ledger.jsonl");
        store.clearAll();

        explanationService = mock(ExplanationService.class);

        when(explanationService.explainTransferOutcome(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn("Mock explanation");

        service = new TokenLedgerService(store, explanationService);
    }

    @Test
    public void rulesFireInOrder() {
        JsonlLedgerStore store = new JsonlLedgerStore("data/ledger.jsonl");
        store.clearAll();

        // Mint 100 to Alice
        store.append(new LedgerEvent(
                EventType.TOKENS_MINTED,
                "PCI-A",
                Map.of("toClient", "Alice", "amount", 100)
        ));

        TokenLedgerService svc = new TokenLedgerService(store, explanationService);

        // Config: lockup in future + accredited-only
        svc.upsertTokenConfig(new RwaTokenConfig("PCI-A", Instant.now().plusSeconds(3600), true));
        svc.upsertClient(new ClientProfile("Alice", true));
        svc.upsertClient(new ClientProfile("Bob", true));

        // 1) should fail lockup first (even though accredited and balance ok)
        LedgerEvent e1 = svc.requestTransfer("PCI-A", "Alice", "Bob", 10);
        assertEquals(EventType.TOKEN_TRANSFER_REJECTED, e1.getType());
        assertEquals("LOCKUP_NOT_EXPIRED", e1.getData().get("reason"));

        // Remove lockup
        svc.upsertTokenConfig(new RwaTokenConfig("PCI-A", Instant.now().minusSeconds(1), true));

        // Make Bob not accredited -> should fail accreditation next
        svc.upsertClient(new ClientProfile("Bob", false));

        LedgerEvent e2 = svc.requestTransfer("PCI-A", "Alice", "Bob", 10);
        assertEquals("NOT_ACCREDITED", e2.getData().get("reason"));

        // Fix accreditation, request too much -> insufficient balance
        svc.upsertClient(new ClientProfile("Bob", true));
        LedgerEvent e3 = svc.requestTransfer("PCI-A", "Alice", "Bob", 1000);
        assertEquals("INSUFFICIENT_BALANCE", e3.getData().get("reason"));

        // Valid transfer -> approved + balances update
        LedgerEvent e4 = svc.requestTransfer("PCI-A", "Alice", "Bob", 40);
        assertEquals(EventType.TOKEN_TRANSFER_APPROVED, e4.getType());
        assertEquals(60, svc.balanceOf("PCI-A", "Alice"));
        assertEquals(40, svc.balanceOf("PCI-A", "Bob"));
    }

}