package com.ajohnson.rwa.service;

import com.ajohnson.rwa.domain.LedgerEvent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExplanationService {

    private final ChatClient chatClient;

    public ExplanationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String explainTransferOutcome(
            String tokenId,
            LedgerEvent outcomeEvent,
            List<LedgerEvent> relatedEvents
    ) {

        String systemPrompt = """
            You are an assistant explaining tokenized asset behavior
            to a wealth advisor. You must:
            - Use only the provided facts
            - Never speculate
            - Never mention AI or models
            - Explain in plain, professional English
            """;

        String userPrompt = buildPrompt(tokenId, outcomeEvent, relatedEvents);

        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    private String buildPrompt(
            String tokenId,
            LedgerEvent outcome,
            List<LedgerEvent> context
    ) {
        return """
                You are generating a short operational explanation for an internal tool.
                
                Rules:
                - Focus ONLY on the most recent outcome event.
                - Do NOT summarize history unless directly relevant.
                - Do NOT add commentary, implications, or other extra context
                - Do NOT use more than 2 sentences.
                - Be factual and concise.
                
                Token ID: %s
                
                Outcome event:
                %s
                
                If the event was rejected, state the reason.
                If the event was approved, state what changed.
                
                """.formatted(
                tokenId,
                outcome,
                context
        );
    }
}
