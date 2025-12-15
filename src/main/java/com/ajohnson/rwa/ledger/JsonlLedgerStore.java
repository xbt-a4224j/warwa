package com.ajohnson.rwa.ledger;

import com.ajohnson.rwa.domain.LedgerEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JsonlLedgerStore {

    private final Path ledgerPath;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public JsonlLedgerStore(String filePath) {
        this.ledgerPath = Path.of(filePath);
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            if (!Files.exists(ledgerPath)) {
                if (ledgerPath.getParent() != null) {
                    Files.createDirectories(ledgerPath.getParent());
                }
                Files.createFile(ledgerPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize ledger file", e);
        }
    }

    public synchronized void append(LedgerEvent event) {
        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(ledgerPath.toFile(), true))) {

            writer.write(objectMapper.writeValueAsString(event));
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            log.error("error",e);
            throw new RuntimeException("Failed to append ledger event", e);
        }
    }

    public List<LedgerEvent> readByToken(String tokenId) {
        return readAll().stream()
                .filter(e -> tokenId.equals(e.getTokenId()))
                .toList();
    }

    public List<LedgerEvent> readAll() {
        List<LedgerEvent> events = new ArrayList<>();

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(ledgerPath.toFile()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                events.add(objectMapper.readValue(line, LedgerEvent.class));
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read ledger events", e);
        }

        return events;
    }

    public synchronized void clearAll() {
        try {
            Files.write(
                    ledgerPath,
                    new byte[0],
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear ledger", e);
        }
    }


}