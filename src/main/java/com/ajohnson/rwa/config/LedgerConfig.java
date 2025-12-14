package com.ajohnson.rwa.config;

import com.ajohnson.rwa.ledger.JsonlLedgerStore;
import com.ajohnson.rwa.service.ExplanationService;
import com.ajohnson.rwa.service.TokenLedgerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerConfig {

    @Bean
    public JsonlLedgerStore jsonlLedgerStore() {
        return new JsonlLedgerStore("data/ledger.jsonl");
    }

    @Bean
    public TokenLedgerService tokenLedgerService(JsonlLedgerStore store, ExplanationService explanationService) {
        return new TokenLedgerService(store, explanationService);
    }
}