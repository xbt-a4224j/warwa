package com.ajohnson.rwa;

import com.ajohnson.rwa.domain.EventType;
import com.ajohnson.rwa.domain.LedgerEvent;
import com.ajohnson.rwa.ledger.JsonlLedgerStore;
import com.ajohnson.rwa.service.TokenLedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static com.ajohnson.rwa.domain.EventType.TOKENS_MINTED;
import static com.ajohnson.rwa.domain.EventType.TOKEN_TRANSFER_APPROVED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

    @Test
    public void testLedgerEvent() {
        JsonlLedgerStore store = new JsonlLedgerStore("data/ledger.jsonl");
        store.clearAll();

        // mint 100 tokens to Alice
        LedgerEvent le = new LedgerEvent(TOKENS_MINTED);
        le.setEventId(UUID.randomUUID());
        le.setTokenId("PCI-A");
        le.setData(Map.of(
                "toClient", "Alice",
                "amount", 100
        ));

        LedgerEvent le2 = new LedgerEvent(TOKEN_TRANSFER_APPROVED);
        le2.setEventId(UUID.randomUUID());
        le2.setTokenId("PCI-A");
        le2.setData(Map.of(
                        "fromClient", "Alice",
                        "toClient", "Bob",
                        "amount", 40
                ));

        store.append(le);
        store.append(le2);


        TokenLedgerService service = new TokenLedgerService(store);


        assertEquals(60, service.balanceOf("PCI-A", "Alice"));
        assertEquals(40, service.balanceOf("PCI-A", "Bob"));

    }

}
