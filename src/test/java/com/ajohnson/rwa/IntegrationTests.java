package com.ajohnson.rwa;

import com.ajohnson.rwa.domain.LedgerEvent;
import com.ajohnson.rwa.ledger.JsonlLedgerStore;
import com.ajohnson.rwa.service.ExplanationService;
import com.ajohnson.rwa.service.TokenLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static com.ajohnson.rwa.domain.EventType.TOKENS_MINTED;
import static com.ajohnson.rwa.domain.EventType.TOKEN_TRANSFER_APPROVED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Disabled
public class IntegrationTests {


    private ExplanationService explanationService;
    private TokenLedgerService service;
    private JsonlLedgerStore store;

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
	void contextLoads() {
	}

    @Test
    public void testLedgerEvent() {
        store.clearAll();

        // mint 100 tokens to Alice
        LedgerEvent le = new LedgerEvent(TOKENS_MINTED);
        le.setTokenId("PCI-A");
        le.setData(Map.of(
                "toClient", "Alice",
                "amount", 100
        ));

        LedgerEvent le2 = new LedgerEvent(TOKEN_TRANSFER_APPROVED);
        le2.setTokenId("PCI-A");
        le2.setData(Map.of(
                        "fromClient", "Alice",
                        "toClient", "Bob",
                        "amount", 40
                ));

        store.append(le);
        store.append(le2);

        TokenLedgerService service = new TokenLedgerService(store, explanationService);

        assertEquals(60, service.balanceOf("PCI-A", "Alice"));
        assertEquals(40, service.balanceOf("PCI-A", "Bob"));

    }
}
