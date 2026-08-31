# AI Finance Controller — Razorpay Buildathon

**Provider-native AI-assisted financial reconciliation and exception-resolution system.**

AI Finance Controller is a finance-operations backend and dashboard designed to process payment and settlement data, reconcile financial records deterministically, identify and manage exceptions, and eventually use AI to investigate ambiguous cases and recommend bounded resolutions.

The system is built around a simple principle:

> **Financial truth must remain deterministic and auditable; AI should assist with investigation and decision support, not invent financial facts.**

---

## Goal

Build one complete finance-operations loop:

```text
Financial Data
        ↓
Ingestion & Normalization
        ↓
Deterministic Reconciliation
        ↓
Exception Detection
        ↓
Evidence Collection
        ↓
AI Investigation
        ↓
Confidence-Based Resolution
        ↓
Human Escalation
        ↓
Audit Trail
```

The current implementation focuses on establishing a reliable deterministic reconciliation foundation before introducing the AI investigation layer.

---

## Build Philosophy

* **Financial truth comes from structured data and deterministic rules.**
* **AI investigates ambiguity; it does not invent financial facts.**
* Every reconciliation result should be explainable from the underlying payment, settlement, refund, adjustment, and order records.
* Automated decisions must have bounded confidence and an auditable basis.
* Synthetic ground truth is kept separate from reconciliation logic so that the system can objectively evaluate its own classifications.
* Prefer a small, working system over unnecessary infrastructure.
* Build and validate the deterministic core before introducing LLM-based reasoning.
* Optional infrastructure and features are postponed until the core finance-operations loop is reliable.

---

## Current Implementation Status

The repository has progressed beyond the original Foundation phase.

The current backend contains dedicated layers for:

```text
config/
controller/
domain/
dto/
health/
ingestion/
razorpay/
repository/
service/
```

The current implementation includes:

* Spring Boot backend
* PostgreSQL persistence
* Financial domain model
* Payment/order/settlement/refund/adjustment records
* External payment-provider client abstraction
* Synthetic financial data generation
* Synthetic reconciliation scenarios
* Synthetic ground truth
* Deterministic reconciliation engine
* Reconciliation result persistence
* Reconciliation summary APIs
* Exception persistence and APIs
* Exception resolution service
* Audit trail support
* CSV ingestion structure
* React/Vite frontend foundation

The repository history reflects this progression, with recent commits covering the financial data foundation, synthetic data/provider integration, reconciliation APIs, exception resolution/audit trail, and deterministic synthetic reconciliation.

---

## Core Reconciliation Engine

The reconciliation engine operates independently of synthetic ground truth.

For each payment, the system evaluates available financial evidence such as:

* Merchant order
* Payment
* Settlement
* Refund
* Adjustment
* Settlement timing
* Duplicate payment records
* Amount differences
* Settlement fees and taxes

The engine currently supports the following reconciliation outcomes:

| Scenario               | Description                                               |
| ---------------------- | --------------------------------------------------------- |
| `EXACT_MATCH`          | Payment and settlement amounts reconcile exactly          |
| `FEE_DIFFERENCE`       | Difference is explained by settlement fee information     |
| `TAX_DIFFERENCE`       | Difference is explained by settlement tax information     |
| `REFUND`               | Refund evidence explains the exception                    |
| `ADJUSTMENT`           | Adjustment evidence explains the exception                |
| `TIMING_DIFFERENCE`    | Settlement occurs outside the configured timing threshold |
| `MISSING_SETTLEMENT`   | No settlement exists for the payment                      |
| `DUPLICATE`            | Multiple payments are associated with the same order      |
| `UNEXPLAINED_MISMATCH` | Available evidence does not explain the difference        |

The reconciliation engine also produces:

* Expected amount
* Actual/settlement amount
* Difference
* Match type
* Reconciliation status
* Confidence score
* Associated exception records where applicable

### Important financial-model rule

Settlement fees and taxes are treated as **evidence explaining settlement differences**, rather than being blindly deducted again from the settlement amount during reconciliation.

This prevents double-counting and allows the reconciliation engine to distinguish fee/tax differences from genuinely unexplained mismatches.

---

## Synthetic Data & Evaluation

A controlled synthetic-data framework is implemented to validate the reconciliation engine.

Synthetic batches generate combinations of:

* Merchant orders
* Payments
* Settlements
* Refunds
* Adjustments
* Duplicate payments
* Timing anomalies
* Missing settlements
* Fee differences
* Tax differences
* Unexplained mismatches

Supported synthetic scenarios:

```text
EXACT_MATCH
FEE_DIFFERENCE
TAX_DIFFERENCE
REFUND
ADJUSTMENT
TIMING_DIFFERENCE
MISSING_SETTLEMENT
DUPLICATE
UNEXPLAINED_MISMATCH
```

Each synthetic batch receives a unique batch identifier:

```text
synthetic_<UUID>
```

Synthetic records can be generated at different scales, including small validation batches and larger evaluation datasets.

---

## Synthetic Ground Truth

Synthetic ground truth is deliberately stored separately from the reconciliation engine.

A ground-truth record contains information such as:

```text
batchId
scenario
orderId
paymentId
settlementId
expectedOutcome
expectedDifference
createdAt
```

The reconciliation engine does **not** use ground truth to determine its classification.

Instead:

```text
Synthetic Generator
        ↓
Financial Records ──────────────→ Reconciliation Engine
        ↓                                  ↓
Ground Truth                        Reconciliation Result
        ↓                                  ↓
        └──────────── Evaluation ──────────┘
```

This allows the project to later measure:

* Classification accuracy
* Precision
* Recall
* Confusion matrix
* False positives
* False negatives
* Confidence calibration
* AI-assisted investigation accuracy
* Resolution recommendation quality

---

## Exception Management

Reconciliation exceptions are persisted independently from reconciliation results.

The current system includes:

* Exception creation
* Exception retrieval APIs
* Exception resolution service
* Resolution state handling
* Audit-related information

The exception layer is intended to become the bridge between deterministic reconciliation and the future AI investigation system.

The long-term flow is:

```text
Reconciliation Result
        ↓
Exception
        ↓
Evidence
        ↓
Investigation
        ↓
Resolution Recommendation
        ↓
Confidence Check
        ↓
Auto-Resolve OR Human Escalation
```

---

## External Provider Integration

The backend contains a provider client abstraction:

```text
razorpay/
├── RazorpayClient.java
└── DefaultRazorpayClient.java
```

This keeps external payment-provider integration behind a backend interface rather than coupling the reconciliation engine directly to the provider implementation.

The intended architecture is:

```text
Provider API
     ↓
Provider Client
     ↓
Ingestion / Normalization
     ↓
Internal Financial Domain
     ↓
Reconciliation
```

Test-mode/provider integration is developed independently from the deterministic reconciliation logic.

---

## Ingestion

The ingestion layer is organized separately from the core domain:

```text
ingestion/
├── controller/
├── dto/
├── model/
└── service/
```

The project is moving toward supporting financial data ingestion through controlled provider/API data as well as file-based inputs such as CSV.

The ingestion layer is responsible for bringing external data into the internal financial model.

It should not contain reconciliation decisions.

---

## Backend Architecture

Current backend structure:

```text
backend/
└── src/
    └── main/
        └── java/
            └── com/
                └── aifincontroller/
                    ├── config/
                    ├── controller/
                    ├── domain/
                    ├── dto/
                    ├── health/
                    ├── ingestion/
                    │   ├── controller/
                    │   ├── dto/
                    │   ├── model/
                    │   └── service/
                    ├── razorpay/
                    ├── repository/
                    └── service/
```

Core services currently include:

```text
ReconciliationService
SyntheticDataGeneratorService
ExceptionResolutionService
```

The reconciliation service contains the deterministic matching and classification logic, while exception resolution is kept as a separate service boundary.

---

## Primary Stack

### Backend

* Java 21
* Spring Boot 3.x
* Maven
* Spring Data JPA
* PostgreSQL

### Frontend

* React 18
* Vite
* Tailwind CSS

### Integration

* Payment-provider test-mode/API integration
* Provider abstraction through `RazorpayClient`

### AI

Planned:

* LLM API
* Backend AI abstraction
* Evidence-grounded investigation
* Structured AI outputs
* Confidence-aware recommendations

AI integration is intentionally deferred until the deterministic reconciliation and exception workflow is sufficiently stable.

---

## Local Setup

### Prerequisites

* Java 21+
* Maven 3.9+
* Node.js 18+
* PostgreSQL 14+

---

### 1. Configure environment

Copy the example environment file:

```powershell
Copy-Item ".env.example" ".env"
```

Then configure:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Do not commit the real `.env` file.

---

### 2. Create the database

```sql
CREATE DATABASE aifincontroller;

CREATE USER aifincontroller WITH PASSWORD 'your_password';

GRANT ALL PRIVILEGES ON DATABASE aifincontroller TO aifincontroller;
```

---

### 3. Run the backend

From the project root:

```powershell
cd backend

mvn spring-boot:run
```

Backend:

```text
http://localhost:8080
```

---

### 4. Run the frontend

Open another PowerShell terminal:

```powershell
cd frontend

npm install

npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

### 5. Health check

```powershell
Invoke-RestMethod "http://localhost:8080/api/health"
```

Expected response should indicate that the service and database are available.

---

# API Endpoints

## Health

| Method | Path          | Description                 |
| ------ | ------------- | --------------------------- |
| `GET`  | `/api/health` | Service and database health |

## Reconciliation

| Method | Path                                           | Description                                       |
| ------ | ---------------------------------------------- | ------------------------------------------------- |
| `POST` | `/api/v1/reconciliation/run?batchId=<batchId>` | Run reconciliation for a synthetic/imported batch |

Example:

```powershell
$batchId = "synthetic_<UUID>"

$result = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/reconciliation/run?batchId=$batchId" `
    -Method POST
```

Inspect results:

```powershell
$result |
    Select-Object `
        paymentReference,
        matchType,
        status,
        expectedAmount,
        actualAmount,
        difference,
        confidenceScore |
    Format-Table -AutoSize
```

Group classifications:

```powershell
$result |
    Group-Object matchType |
    Sort-Object Name |
    Select-Object Name, Count |
    Format-Table -AutoSize
```

---

# Development Roadmap

The project is organized around the following implementation phases.

### Phase 1 — Foundation

**Status: Complete**

* Spring Boot backend
* React/Vite frontend foundation
* PostgreSQL integration
* Environment configuration
* Health checks
* Base project architecture

---

### Phase 2 — Financial Data Layer

**Status: Substantially complete / deterministic core implemented**

#### Phase 2A — Financial Domain

* Payment model
* Merchant order model
* Settlement model
* Refund model
* Adjustment model
* JPA repositories
* Database persistence

#### Phase 2B — Provider & Synthetic Data

* Provider client abstraction
* Provider integration foundation
* Synthetic financial data generator
* Batch-based synthetic records
* Multiple controlled reconciliation scenarios
* Synthetic ground truth

#### Phase 2C — Reconciliation

* Batch reconciliation
* Payment-level reconciliation
* Duplicate detection
* Missing settlement detection
* Refund detection
* Adjustment detection
* Timing detection
* Exact matching
* Fee difference detection
* Tax difference detection
* Unexplained mismatch detection
* Confidence scoring
* Reconciliation result persistence

#### Phase 2D — Exception & Evaluation Foundation

* Reconciliation exception persistence
* Exception APIs
* Exception resolution service
* Audit-related resolution data
* Synthetic ground-truth evaluation foundation

---

### Phase 3 — Robust Ingestion & Normalization

**Status: Next major implementation area**

Planned work:

* Complete CSV ingestion
* Input validation
* Schema validation
* Normalization
* Batch-level ingestion tracking
* Duplicate/input-record handling
* Provider-to-internal-model mapping
* Ingestion error reporting
* Reconciliation-ready data validation

The objective is to make the pipeline reliable for both synthetic and externally sourced financial data.

---

### Phase 4 — Evidence & Exception Intelligence

**Planned**

Build a structured evidence layer around exceptions:

```text
Exception
   ↓
Related Financial Records
   ↓
Evidence
   ↓
Explanation
```

This phase should make it possible to answer:

* What happened?
* Which records caused the exception?
* What amount was expected?
* What amount was received?
* What is the difference?
* Which evidence explains the difference?
* How confident are we?

---

### Phase 5 — AI Investigator

**Planned**

Introduce an LLM behind a backend abstraction.

The AI investigator will receive **bounded structured evidence**, rather than unrestricted database access.

Expected output:

```text
Investigation Result
├── explanation
├── suspected cause
├── supporting evidence
├── recommended action
├── confidence
└── escalation recommendation
```

The AI must not invent missing financial records or override deterministic financial facts.

---

### Phase 6 — Controller / Decision Engine

**Planned**

Introduce policy-driven decisioning:

```text
Deterministic Result
        +
Evidence
        +
AI Investigation
        ↓
Decision Policy
        ↓
┌─────────────────────┐
│ Auto Resolve        │
│ Review Required     │
│ Human Escalation    │
└─────────────────────┘
```

Resolution policies should be bounded by confidence, exception type, financial impact, and available evidence.

---

### Phase 7 — Dashboard & Audit

**Planned**

Build an operations-facing dashboard for:

* Reconciliation batches
* Match statistics
* Exception counts
* Exception severity
* Investigation results
* Resolution status
* Human escalations
* Audit history

The dashboard should expose the reasoning behind financial decisions rather than simply showing final statuses.

---

### Phase 8 — Reliability & Final Demo

**Planned**

* Larger synthetic evaluations
* Performance testing
* Idempotency validation
* Failure handling
* Retry behavior
* Observability
* Security review
* AI evaluation
* End-to-end demo workflow

The final demonstration should show the complete loop:

```text
Financial Data
      ↓
Ingestion
      ↓
Reconciliation
      ↓
Exception
      ↓
Evidence
      ↓
AI Investigation
      ↓
Confidence / Policy
      ↓
Resolution or Human Escalation
      ↓
Audit Trail
```

---

# Evaluation Strategy

The synthetic framework will be used to evaluate both deterministic and AI-assisted components.

At minimum, evaluation should cover:

### Reconciliation

* Scenario classification accuracy
* Precision
* Recall
* Confusion matrix
* False positive rate
* False negative rate

### Confidence

* Confidence calibration
* High-confidence correctness
* Low-confidence escalation behavior

### AI Investigation

* Evidence grounding
* Explanation correctness
* Unsupported-claim rate
* Recommended-action accuracy
* Escalation accuracy

### Resolution

* Automatic resolution correctness
* Human escalation correctness
* Financial-risk containment

---

# Design Principles for Future Work

### 1. Deterministic first

If a financial fact can be established directly from structured records, use deterministic logic.

### 2. AI as an investigator

AI should reason over evidence and explain ambiguous cases.

### 3. No hallucinated financial facts

The AI must not fabricate:

* transactions
* amounts
* settlements
* refunds
* fees
* taxes
* dates
* customer information

### 4. Confidence is not truth

A high AI confidence score does not make an unsupported conclusion valid.

### 5. Human escalation is a feature

Uncertainty should result in controlled escalation rather than forced automation.

### 6. Auditability is mandatory

Every important decision should be traceable to:

```text
Source Record
    ↓
Evidence
    ↓
Rule / Investigation
    ↓
Classification
    ↓
Confidence
    ↓
Decision
    ↓
Resolution
```

### 7. Keep evaluation independent

Synthetic ground truth must remain separate from production reconciliation logic so that the system can genuinely measure its own performance.

---

# Project Status

**Current focus:** strengthening the financial data and reconciliation pipeline before introducing the AI investigation layer.

The deterministic reconciliation engine and synthetic scenario framework are already implemented and form the foundation for the next stages of the project.

See the repository history for the implementation progression and individual feature commits.
