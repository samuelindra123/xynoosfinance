# XynoosFinance Security Specification (Phase 0 TDD & Red Team Audit)

## 1. Data Invariants
1. **Strict Ownership Isolation**: Every document under `/users/{userId}` and all its subcollections (`accounts`, `transactions`, `categories`, `budgets`, `goals`, `goal_contributions`, `recurring_transactions`, `reminders`, `business_profiles`, `contacts`, `cost_centers`, `invoices`, `ai_conversations`, `ai_messages`, `report_preferences`) MUST belong exclusively to `request.auth.uid == userId` AND have `data.userId == request.auth.uid`.
2. **Exact Integer Minor Units (IDR)**: All monetary amounts (`amountIdr`, `openingBalanceIdr`, `limitAmountIdr`, `targetAmountIdr`, `currentSavedIdr`) MUST be stored as exact integers (`int`) in Indonesian Rupiah (IDR), never floating-point numbers, and bounded within valid financial limits.
3. **Immutable Audit Metadata**: `userId` and `createdAt` are immutable across all updates (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
4. **Server Timestamp Enforcement**: `createdAt` and `updatedAt` MUST be valid Firestore `timestamp` objects with `<= request.time`.
5. **Path Variable Hardening**: Every document ID (`userId`, `accountId`, `transactionId`, etc.) MUST satisfy `isValidId(id)` (`^[a-zA-Z0-9_\-]+$`, max 128 chars) on single-document operations (`get`, `create`, `update`, `delete`).
6. **Secure List Queries (Query Enforcer)**: Every `allow list` rule enforces `isSignedIn() && request.auth.uid == userId && resource.data.userId == request.auth.uid`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/Write**: Request with `request.auth == null` attempting `get` or `list` on `/users/alice_123/transactions`.
2. **Cross-User Read (PII & Ledger Leak)**: Authenticated `bob_456` attempting `get` or `list` on `/users/alice_123` or `/users/alice_123/transactions`.
3. **Identity Spoofing on Create**: Authenticated `alice_123` writing to `/users/alice_123/transactions/tx_1` with `{"userId": "bob_456", ...}`.
4. **Ownership Mutation on Update**: Authenticated `alice_123` updating `userId` from `"alice_123"` to `"bob_456"`.
5. **Shadow Update (Ghost Field Injection)**: Authenticated owner sending an unlisted field `{"isVerifiedAdmin": true}` on create or update.
6. **Creation Timestamp Tampering**: Authenticated owner modifying `createdAt` during an `update` operation.
7. **Future Timestamp Spoofing**: Authenticated owner submitting a `createdAt` or `updatedAt` timestamp far in the future (`> request.time`).
8. **Negative or Non-Integer Financial Amount**: Submitting `amountIdr: -50000` or string `"50000"` for a transaction amount.
9. **Invalid Transaction Type Enum**: Submitting `type: "HACKED_TYPE"` instead of allowed `['INCOME', 'EXPENSE', 'TRANSFER', 'REFUND', 'ADJUSTMENT']`.
10. **Denial-of-Wallet String Overflow**: Submitting a 10,000-character string in `title` or `notes`.
11. **ID Poisoning**: Creating a document with an oversized or special-character ID outside `^[a-zA-Z0-9_\-]+$`.
12. **Unfiltered Collection List Scraping**: Querying a collection without matching `userId` ownership constraints.

## 3. Red Team Audit & Conflict Report
- **Shadow Update Test**: PASS — Every `create` checks `incoming().keys().hasOnly(...)` and every `update` checks `incoming().diff(existing()).affectedKeys().hasOnly(...)`.
- **PII Blanket Test**: PASS — `/users/{userId}` and `/users/{userId}/contacts/{contactId}` restrict `get` and `list` strictly to `request.auth.uid == userId && resource.data.userId == request.auth.uid`.
- **Query Trust Test**: PASS — Every `allow list` validates `request.auth.uid == userId && resource.data.userId == request.auth.uid`.
- **Value Poisoning Test**: PASS — Every `allow update` starts with `isSignedIn() && isValidId(...) && isValid[Entity](incoming())`.
