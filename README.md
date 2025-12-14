# warwa — Tokenized RWA client e-advisor

<img width="1049" height="543" alt="Screenshot 2025-12-14 at 1 03 46 AM" src="https://github.com/user-attachments/assets/6d10d195-080a-4a82-ab63-7e2af77a01cc" />
<img width="1311" height="565" alt="Screenshot 2025-12-14 at 1 03 39 AM" src="https://github.com/user-attachments/assets/5a973be8-d98f-416d-b435-b4e5148098ea" />



This repo is a small, internal-style prototype modeling tokenized real-world assets in a wealth management context.

It implements an append-only ledger for fund units, rule-based transfers such as lockups and balance checks, and lightweight explainability for rejected actions. A separate read-only view looks up live Ethereum balances and ERC-20 metadata for reference and reconciliation.

The focus is on correctness, auditability, and clarity.

Architecture
-	**Ledger**: append-only JSONL event log representing fund and transfer events
-	**Domain services**: derive balances and enforce transfer rules by replaying events
-	**Explainability**: produces concise, human-readable reasons for rejected actions
-	**UI**: thin Vaadin views for demo and inspection
-	**Ethereum On-chain lookup**: isolated, read-only Ethereum integration for reference data


```aiignore
src/main/java/com/ajohnson/rwa
├── ledger        // event model and JSONL storage
├── service       // domain logic and rule enforcement
├── onchain       // read-only Ethereum access
├── ui            // Vaadin views and layout
└── config        // Spring configuration
```


**Running locally**
```aiignore
./gradlew bootRun
http://localhost:8080
```
