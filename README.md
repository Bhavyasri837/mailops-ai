# MailOps AI — Autonomous Email-to-Action Agent

Supervity Forward Deployed Engineer Technical Screening Assessment — Problem 10.

The application receives simulated business emails, uses an LLM to classify
them, performs the appropriate deterministic business action, routes
ambiguous cases to a human reviewer, and keeps a full audit trail.

```
React (Vite) → Axios → Spring Boot → OpenAI API → MySQL
```

## Tech Stack

**Backend:** Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA/Hibernate,
Bean Validation, Jackson, Maven, JUnit 5, Mockito.
**Database:** MySQL 8.x via MySQL Connector/J.
**AI:** OpenAI API via the official OpenAI Java SDK (with a deterministic
`MockClassifierClient` fallback for `AI_MODE=MOCK`, used in tests).
**Frontend:** React 18, Vite, Tailwind CSS, Axios, React Router, Recharts,
Lucide React.

## Project layout

```
mailops-ai/
├── backend/    Spring Boot API (this document covers its REST layer)
├── frontend/   React operations dashboard (Phase 7)
└── sample-data/emails.json   Seed emails loaded on startup (SEED_ENABLED=true)
```

## Running the backend

```
cd backend
cp .env.example .env   # fill in OPENAI_API_KEY and DB credentials
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`, all routes under `/api`.
CORS is restricted to `CORS_ORIGINS` (default `http://localhost:5173`) —
see `app.cors.allowed-origins` in `application.properties`.

Set `AI_MODE=MOCK` to use `MockClassifierClient` instead of calling OpenAI
(useful for local development and is what the test suite uses).

## Running the frontend

```
cd frontend
cp .env.example .env   # VITE_API_BASE_URL=http://localhost:8080/api
npm install
npm run dev
```

The dashboard runs on `http://localhost:5173` and talks to the backend at
`VITE_API_BASE_URL` (default `http://localhost:8080/api`) via the centralized
Axios client in `src/services/api.js`. Run `npm run build` to produce a
production bundle in `dist/`.

### Pages

| Route | Page | Purpose |
|---|---|---|
| `/dashboard` (also `/`) | Dashboard | Live stats from `GET /api/dashboard/stats`, intent distribution and processing-status charts |
| `/inbox` | Inbox | Searchable, filterable email table; process individual or all pending emails |
| `/inbox/:id` | Email Details | Original email, AI classification, action taken, and full audit timeline |
| `/review` | Human Review | Emails routed to a human because AI confidence was below threshold; submit the correct intent |
| `/invoices` | Invoices | Invoices created from `INVOICE_SUBMISSION` emails |
| `/tasks` | Tasks | Follow-up tasks created from `DISPUTE` emails |
| `/reply-drafts` | Reply Drafts | Draft replies generated for `PAYMENT_QUERY` emails, with copy-to-clipboard |
| `/audit` | Audit Log | Every audit event across every email, aggregated and searchable (see note below) |

**Note on the Audit Log page:** the backend exposes audit trails per-email
(`GET /api/emails/{id}/audit`) rather than through a single global endpoint.
The Audit Log page adapts to this on the frontend by fetching every email
and merging their audit trails into one chronological log — no backend
change was made or needed for the assessment's dataset size. A dedicated
`GET /api/audit` endpoint would be a reasonable addition if this needs to
scale to a much larger email volume.

## Processing pipeline

```
INVOICE_SUBMISSION → CREATE_INVOICE
PAYMENT_QUERY       → CREATE_REPLY_DRAFT
DISPUTE             → CREATE_FOLLOW_UP_TASK
SPAM                → FLAG_AS_SPAM

confidence < 0.85 (CONFIDENCE_THRESHOLD) → NEEDS_REVIEW → human resolves via
POST /api/emails/{id}/review
```

The LLM is consulted exactly once per email, to decide intent. Every
decision after that — threshold check, which action to take, whether it
succeeded — is deterministic Java code in `ActionService`/`ConfidenceService`.

---

# REST API Reference (Phase 6)

Base URL: `http://localhost:8080/api`

All list/detail responses are DTOs — JPA entities are never serialized
directly.

## Emails

### `GET /api/emails`
List inbox emails, most recently received first.

**Query params** (both optional, combinable):
| Param | Type | Description |
|---|---|---|
| `status` | `EmailStatus` | `RECEIVED`, `PROCESSING`, `PROCESSED`, `NEEDS_REVIEW`, `SPAM`, `FAILED` |
| `search` | string | Case-insensitive substring match against sender, subject, or body |

```
GET /api/emails?status=NEEDS_REVIEW
GET /api/emails?search=invoice
```

**Response `200`** — array of:
```json
{
  "id": 12,
  "externalId": "seed-012",
  "sender": "vendor@example.com",
  "recipient": "ap@company.com",
  "subject": "Invoice attached",
  "body": "Please find attached invoice #INV-1001...",
  "receivedAt": "2026-08-20T09:15:00",
  "status": "PROCESSED",
  "createdAt": "2026-08-20T09:15:02",
  "updatedAt": "2026-08-20T09:15:05"
}
```

### `GET /api/emails/{id}`
Full detail view: the email, its latest classification (or `null` if not
yet classified), all actions taken, and the full audit trail.

**Response `200`:**
```json
{
  "email": { ...EmailDto... },
  "latestClassification": { ...ClassificationDto or null... },
  "actions": [ { ...ActionDto... } ],
  "auditLog": [ { ...AuditLogDto... } ]
}
```
**Errors:** `404 EMAIL_NOT_FOUND` if the id doesn't exist.

### `GET /api/emails/{id}/classification`
Latest AI classification for the email (`null` if none exists yet).

**Response `200`:**
```json
{
  "id": 5,
  "emailId": 12,
  "intent": "INVOICE_SUBMISSION",
  "confidence": 0.96,
  "reason": "Email contains an attached invoice with amount and due date.",
  "evidence": ["subject mentions 'invoice'", "body contains invoice number INV-1001"],
  "extractedData": { "invoiceNumber": "INV-1001", "amount": "1250.00", "vendor": "Acme Corp", "dueDate": "2026-09-20" },
  "intentProbabilities": { "INVOICE_SUBMISSION": 0.96, "PAYMENT_QUERY": 0.03, "DISPUTE": 0.01, "SPAM": 0.0 },
  "createdAt": "2026-08-20T09:15:03"
}
```
**Errors:** `404 EMAIL_NOT_FOUND`.

### `GET /api/emails/{id}/actions`
All actions executed for this email, most recent first.

**Response `200`:** array of:
```json
{ "id": 8, "emailId": 12, "actionType": "CREATE_INVOICE", "actionStatus": "SUCCESS", "result": "Invoice INV-1001 created.", "createdAt": "2026-08-20T09:15:04" }
```
**Errors:** `404 EMAIL_NOT_FOUND`.

### `GET /api/emails/{id}/audit`
Full audit trail, chronological (oldest first) — suitable for a React
timeline. `actor` is one of `AI`, `SYSTEM`, `HUMAN`.

**Response `200`:** array of:
```json
{ "id": 21, "emailId": 12, "eventType": "CLASSIFIED", "actor": "AI", "message": "Email classified as INVOICE_SUBMISSION with confidence 0.96.", "metadata": {}, "createdAt": "2026-08-20T09:15:03" }
```
**Errors:** `404 EMAIL_NOT_FOUND`.

### `POST /api/emails/{id}/process`
Runs the full pipeline for one email (classify → confidence check →
autonomous action, or route to human review). Idempotent: an already
`PROCESSED`/`SPAM` email returns `outcome: "ALREADY_PROCESSED"` without
re-running anything.

**Response `200`** (autonomous success):
```json
{
  "emailId": 12,
  "outcome": "PROCESSED",
  "intent": "INVOICE_SUBMISSION",
  "confidence": 0.96,
  "actionType": "CREATE_INVOICE",
  "actionStatus": "SUCCESS",
  "message": "Invoice INV-1001 created."
}
```

**Response `200`** (ambiguous → human review):
```json
{
  "emailId": 15,
  "outcome": "NEEDS_REVIEW",
  "intent": "PAYMENT_QUERY",
  "confidence": 0.61,
  "actionType": null,
  "actionStatus": null,
  "message": "Confidence below threshold; awaiting human review."
}
```
`outcome` is one of `PROCESSED`, `NEEDS_REVIEW`, `FAILED`, `ALREADY_PROCESSED`.
**Errors:** `404 EMAIL_NOT_FOUND`.

### `POST /api/emails/process-all`
Processes every email currently in `RECEIVED` status. One email failing
never stops the batch — each is processed independently and its result
recorded.

**Response `200`:**
```json
{
  "totalAttempted": 20,
  "processed": 15,
  "needsReview": 3,
  "failed": 2,
  "results": [ { ...ProcessResultDto per email... } ]
}
```

### `POST /api/emails/{id}/review`
Human resolution of a `NEEDS_REVIEW` email. The LLM is **not** called again
— the human's intent is authoritative and is run through the same
deterministic `ActionService` used for autonomous processing.

**Request body:**
```json
{ "intent": "DISPUTE" }
```
Allowed values (case-insensitive): `INVOICE_SUBMISSION`, `PAYMENT_QUERY`,
`DISPUTE`, `SPAM`.

**Response `200`:**
```json
{
  "emailId": 15,
  "outcome": "PROCESSED",
  "intent": "DISPUTE",
  "confidence": 0.61,
  "actionType": "CREATE_FOLLOW_UP_TASK",
  "actionStatus": "SUCCESS",
  "message": "Follow-up task created."
}
```
**Errors:**
- `400 VALIDATION_ERROR` — `intent` missing or blank.
- `400 INVALID_REVIEW` — `intent` isn't one of the four allowed values, or
  the email isn't currently `NEEDS_REVIEW`.
- `404 EMAIL_NOT_FOUND`.

## Dashboard

### `GET /api/dashboard/stats`
All figures are computed live from MySQL on every call — nothing is cached
or hardcoded.

**Response `200`:**
```json
{
  "totalEmails": 20,
  "emailsByStatus": { "RECEIVED": 0, "PROCESSING": 0, "PROCESSED": 12, "NEEDS_REVIEW": 3, "SPAM": 3, "FAILED": 2 },
  "classificationsByIntent": { "INVOICE_SUBMISSION": 8, "PAYMENT_QUERY": 5, "DISPUTE": 4, "SPAM": 3 },
  "autonomousActionsExecuted": 12,
  "humanReviewsCompleted": 3,
  "pendingHumanReview": 3,
  "invoicesCreated": 8,
  "tasksCreated": 4,
  "replyDraftsCreated": 5,
  "spamFlagged": 3,
  "actionsFailed": 2
}
```

## Invoices

### `GET /api/invoices`
All invoices created from `INVOICE_SUBMISSION` emails, most recent first.

**Response `200`:** array of:
```json
{ "id": 1, "emailId": 12, "invoiceNumber": "INV-1001", "vendor": "Acme Corp", "amount": 1250.00, "currency": "USD", "dueDate": "2026-09-20", "status": "LOGGED", "createdAt": "2026-08-20T09:15:04" }
```

## Tasks

### `GET /api/tasks`
Follow-up tasks created from `DISPUTE` emails, most recent first.

**Response `200`:** array of:
```json
{ "id": 1, "emailId": 18, "title": "Follow up: billing dispute", "description": "Customer disputes charge on invoice INV-55.", "priority": "HIGH", "status": "OPEN", "createdAt": "2026-08-20T09:20:00" }
```

## Reply Drafts

### `GET /api/reply-drafts`
Draft replies generated for `PAYMENT_QUERY` emails, most recent first.
**No email is ever actually sent** — these are drafts only.

**Response `200`:** array of:
```json
{ "id": 1, "emailId": 9, "draftText": "Hi, your payment for invoice INV-9 was received on...", "status": "DRAFTED", "createdAt": "2026-08-20T09:10:00" }
```

## Error format

All errors are handled centrally by `GlobalExceptionHandler`
(`@RestControllerAdvice`). No stack trace is ever returned.

```json
{
  "timestamp": "2026-08-25T10:00:00",
  "status": 404,
  "error": "EMAIL_NOT_FOUND",
  "message": "Email not found with id: 999",
  "path": "/api/emails/999"
}
```

| Status | `error` code | When |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Request body fails Bean Validation (e.g. blank `intent`) |
| 400 | `INVALID_REVIEW` | Invalid intent value, or email not awaiting review |
| 404 | `EMAIL_NOT_FOUND` | Email id doesn't exist |
| 502 | `AI_CLASSIFICATION_FAILED` | Safety-net only — normal AI failures are already caught and routed to `NEEDS_REVIEW` inside `EmailProcessingService` rather than surfaced as an HTTP error |
| 500 | `INTERNAL_ERROR` | Anything unexpected |

## Testing

```
cd backend
./mvnw clean test
```

- Service-layer tests (`src/test/java/com/mailops/service/`) — business
  logic, confidence thresholds, action execution, idempotency.
- Controller-layer tests (`src/test/java/com/mailops/controller/`) —
  `@WebMvcTest` + `MockMvc` slices per controller, service layer mocked via
  `@MockBean`. Cover happy paths, 404s, validation errors, and the
  process-all partial-failure case. **The real OpenAI API is never called
  in any test.**
# mailops-ai
