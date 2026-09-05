AI Finance Controller — Razorpay Buildathon

Provider-native AI-assisted financial reconciliation and exception-resolution system.

AI Finance Controller is a finance-operations backend and dashboard designed to process payment and settlement data, reconcile financial records deterministically, identify and manage exceptions, collect structured evidence, investigate ambiguous cases with AI, recommend bounded resolutions, and maintain an auditable trail of important decisions.

The system is built around a simple principle:

Financial truth must remain deterministic and auditable; AI should assist with investigation and decision support, not invent financial facts.

Goal

Build one complete finance-operations loop:

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
Confidence-Based Decision
      ↓
Human Escalation
      ↓
Audit Trail

The core deterministic reconciliation, evidence, AI investigation, decision, audit, and dashboard workflow is implemented. The current focus is evaluation and final demonstration using sufficiently large synthetic batches.

Build Philosophy

Financial truth comes from structured data and deterministic rules.

AI investigates ambiguity; it does not invent financial facts.

Every reconciliation result should be explainable from the underlying payment, settlement, refund, adjustment, and order records.

AI receives bounded structured evidence rather than unrestricted database access.

Automated decisions must have bounded confidence and an auditable basis.

Synthetic ground truth is kept separate from reconciliation logic so the system can objectively evaluate classifications.

Human escalation is a valid outcome when evidence is insufficient.

Customer-facing explanations should use readable financial language rather than exposing internal implementation enums.

Prefer a small, working system over unnecessary infrastructure.

Current Implementation Status

The repository has progressed beyond the original Foundation phase.

The current backend contains dedicated layers for:

config/
controller/
domain/
dto/
health/
ingestion/
razorpay/
repository/
service/

The current implementation includes:

Spring Boot backend

PostgreSQL persistence

Flyway database migrations

Financial domain model

Payment/order/settlement/refund/adjustment records

External payment-provider client abstraction

Synthetic financial data generation

Synthetic reconciliation scenarios

Synthetic ground truth

Deterministic reconciliation engine

Reconciliation result persistence

Reconciliation summary APIs

Exception persistence and APIs

Structured evidence collection

Deterministic financial analysis

AI investigation through a backend provider abstraction

Structured AI investigation responses

AI confidence and evidence validation

Confidence-aware decisioning

Automatic resolution and manual-review outcomes

Decision persistence

Audit event persistence and retrieval

CSV ingestion structure

React/Vite/Tailwind frontend

Overview metrics

Reconciliation results view

Exception queue

Investigation detail view

AI investigation and decision workflow

Audit timeline

Batch-run visibility

Human-review/status actions

Merchant-readable financial formatting

The implementation now demonstrates the complete finance-operations loop rather than only the deterministic reconciliation foundation.

Core Reconciliation Engine

The reconciliation engine operates independently of synthetic ground truth.

For each payment, the system evaluates available financial evidence such as:

Merchant order

Payment

Settlement

Refund

Adjustment

Settlement timing

Duplicate payment records

Amount differences

Settlement fees and taxes

The engine currently supports the following reconciliation outcomes:

Scenario

Description

EXACT_MATCH

Payment and settlement amounts reconcile exactly

FEE_DIFFERENCE

Difference is explained by settlement fee information

TAX_DIFFERENCE

Difference is explained by settlement tax information

REFUND

Refund evidence explains the exception

ADJUSTMENT

Adjustment evidence explains the exception

TIMING_DIFFERENCE

Settlement occurs outside the configured timing threshold

MISSING_SETTLEMENT

No settlement exists for the payment

DUPLICATE

Multiple payments are associated with the same order

UNEXPLAINED_MISMATCH

Available evidence does not explain the difference

The reconciliation engine also produces:

Expected amount

Actual/settlement amount

Difference

Match type

Reconciliation status

Confidence score

Associated exception records where applicable

Important financial-model rule

Settlement fees and taxes are treated as evidence explaining settlement differences, rather than being blindly deducted again from the settlement amount during reconciliation.

This prevents double-counting and allows the reconciliation engine to distinguish fee/tax differences from genuinely unexplained mismatches.

Synthetic Data & Evaluation

A controlled synthetic-data framework is implemented to validate the reconciliation engine.

Synthetic batches generate combinations of:

Merchant orders

Payments

Settlements

Refunds

Adjustments

Duplicate payments

Timing anomalies

Missing settlements

Fee differences

Tax differences

Unexplained mismatches

Supported synthetic scenarios:

EXACT_MATCH
FEE_DIFFERENCE
TAX_DIFFERENCE
REFUND
ADJUSTMENT
TIMING_DIFFERENCE
MISSING_SETTLEMENT
DUPLICATE
UNEXPLAINED_MISMATCH

Each synthetic batch receives a unique batch identifier:

synthetic_<UUID>

Synthetic records can be generated at different scales, including evaluation batches containing 50+ records.

The final demonstration should use a sufficiently large batch to show throughput, match rate, exceptions, and the cases the controller does not safely resolve.

Synthetic Ground Truth

Synthetic ground truth is deliberately stored separately from the reconciliation engine.

A ground-truth record contains information such as:

batchId
scenario
orderId
paymentId
settlementId
expectedOutcome
expectedDifference
createdAt

The reconciliation engine does not use ground truth to determine its classification.

Instead:

Synthetic Generator
        ↓
Financial Records ──────────────→ Reconciliation Engine
        ↓                                  ↓
Ground Truth                         Reconciliation Result
        ↓                                  ↓
        └──────────── Evaluation ──────────┘

This allows the project to measure:

Classification accuracy

Precision

Recall

False positives

False negatives

Confidence calibration

AI-assisted investigation quality

Resolution recommendation quality

Exception Management

Reconciliation exceptions are persisted independently from reconciliation results.

The current system includes:

Exception creation

Exception retrieval APIs

Exception filtering

Exception resolution service

Resolution state handling

Investigation persistence

Decision persistence

Audit-related information

The implemented workflow is:

Reconciliation Result
        ↓
     Exception
        ↓
     Evidence
        ↓
 AI Investigation
        ↓
 Decision Policy
        ↓
 ┌─────────────────────┐
 │ Auto-Resolve        │
 │ Manual Review       │
 │ Insufficient        │
 │ Evidence            │
 └─────────────────────┘
        ↓
    Audit Trail

Evidence & Financial Analysis

Before AI investigation, the backend constructs a bounded evidence package from deterministic financial records.

Depending on the exception, evidence can include:

Exception details

Expected amount

Actual amount

Reconciliation difference

Payment information

Order information

Settlement information

Settlement fees

Settlement tax

Refund information

Adjustment information

Evidence identifiers

The backend also calculates deterministic financial analysis, including:

Payment/order agreement

Payment/expected-amount agreement

Settlement presence

Settlement amount

Settlement fees

Settlement tax

Refund totals

Adjustment totals

Known deductions

Explained difference

Unexplained difference

Candidate causes

Contradictory evidence

Missing evidence

These deterministic financial facts are supplied to the AI as authoritative evidence.

AI Investigator

The AI investigation layer is implemented behind a backend provider abstraction.

The current implementation uses Gemini through the backend. The API key is kept outside source control through environment configuration.

The AI does not independently determine financial truth. Instead, the backend first collects deterministic financial evidence and provides that bounded evidence to the AI.

Structured investigation response

The investigation result contains structured fields such as:

Conclusion
Explanation
What happened
Root cause
Financial impact
Supporting evidence
Alternative explanations
Missing evidence
Confidence reasoning
Recommended action
Evidence references
Confidence
Recommended status

The investigation is persisted so that the reasoning can be inspected later.

AI safety boundaries

The investigator must not fabricate:

Transactions

Amounts

Settlements

Refunds

Fees

Taxes

Dates

Customer information

Evidence references are validated against evidence supplied by the backend.

Confidence is validated to remain within a bounded range, and recommendations are restricted to supported resolution states.

Customer-facing explanations are formatted into readable financial language and avoid exposing internal technical classification names unnecessarily.

AI Decision & Resolution

The AI investigation is followed by a bounded decision step.

Deterministic Result
        +
Structured Evidence
        +
AI Investigation
        ↓
Decision Policy
        ↓
┌──────────────────────────┐
│ Automatically Resolve    │
│ Manual Review             │
│ Insufficient Evidence     │
└──────────────────────────┘

The decision layer validates:

Investigation structure

Confidence

Evidence references

Recommendation validity

Investigation consistency

Required evidence for resolution

The decision is persisted separately from the investigation, allowing the system to distinguish between what the AI concluded and what the controller decided.

Audit Trail

Important financial operations are persisted as audit events.

The audit trail provides traceability across:

Source Record
      ↓
Evidence
      ↓
Reconciliation
      ↓
AI Investigation
      ↓
Decision
      ↓
Resolution / Review

The investigation detail view exposes the relevant history so that an operator can understand how a case progressed from discrepancy to decision.

Audit records are intended to preserve the history of important controller actions rather than silently overwriting the history of a financial decision.

Dashboard

The React dashboard provides an operations-facing view of the controller.

Overview

Total records

Matched records

AI-resolved cases

Unresolved cases

Match and resolution metrics

Processing information

Exception breakdown

Reconciliation

Batch-level reconciliation results

Match classifications

Search and filtering

Expandable financial details

Exception Queue

Open exceptions

Severity

Category

Status

Search and filtering

Investigation access

Investigation Detail

An operator can inspect:

Source financial records

Expected vs actual amounts

Difference

Deterministic financial analysis

AI explanation

Root cause

Financial impact

Supporting evidence

Missing evidence

Confidence

Recommended action

Decision

Audit history

Batch Runs

The dashboard also exposes ingestion/reconciliation batch activity and processing state.

The UI is designed so that an operator can quickly understand the controller's operation and trace an automated decision back to the financial evidence behind it.

External Provider Integration

The backend contains a provider client abstraction:

razorpay/

├── RazorpayClient.java
└── DefaultRazorpayClient.java

This keeps external payment-provider integration behind a backend interface rather than coupling the reconciliation engine directly to a provider implementation.

The intended architecture is:

Provider API
     ↓
Provider Client
     ↓
Ingestion / Normalization
     ↓
Internal Financial Domain
     ↓
Reconciliation

Provider integration is developed independently from deterministic reconciliation logic.

Ingestion

The ingestion layer is organized separately from the core domain:

ingestion/

├── controller/
├── dto/
├── model/
└── service/

The ingestion layer provides the foundation for bringing controlled provider/API data and file-based inputs such as CSV into the internal financial model.

It is responsible for input handling, validation, normalization, and batch tracking without embedding reconciliation decisions inside the ingestion layer.

CSV ingestion and broader normalization hardening remain future reliability work.

Backend Architecture

Current backend structure:

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

Core services include:

ReconciliationService
SyntheticDataGeneratorService
FinancialAnalysisService
AiInvestigationEvidenceService
AiInvestigationPromptBuilder
ExceptionResolutionService

The architecture keeps deterministic financial computation, evidence collection, AI investigation, decisioning, and persistence behind separate service boundaries.

Primary Stack

Backend

Java 21

Spring Boot 3.x

Maven

Spring Data JPA

PostgreSQL

Flyway

Spring Web / WebFlux

Frontend

React 18

Vite

Tailwind CSS

Integration

Payment-provider test-mode/API integration

Provider abstraction through RazorpayClient

AI

Gemini through a backend provider abstraction

Structured evidence-grounded investigation

Structured AI outputs

Confidence-aware recommendations

Evidence-reference validation

Bounded resolution decisions

Local Setup

Prerequisites

Java 21+

Maven 3.9+

Node.js 18+

PostgreSQL 14+

1. Configure environment

Copy the example environment file if present:

Copy-Item ".env.example" ".env"

Configure the required environment variables, including:

DB_URL
DB_USERNAME
DB_PASSWORD

For AI-enabled execution, configure the Gemini-related variables used by the backend, including the API key.

Never commit real credentials or API keys.

2. Create the database

Create a PostgreSQL database matching the configured DB_URL.

For example:

CREATE DATABASE ai_fin_controller;

Use your own PostgreSQL username/password through environment variables.

Flyway migrations create and update the application schema automatically when the backend starts.

3. Run the backend

From the project root:

cd backend
mvn spring-boot:run

Backend:

http://localhost:8080

4. Run the frontend

Open another PowerShell terminal:

cd frontend
npm install
npm run dev

Frontend:

http://localhost:5173

5. Health check

Invoke-RestMethod "http://localhost:8080/api/health"

The response should indicate that the service is available and report the configured dependency/database health.

API Endpoints

Health

Method

Path

Description

GET

/api/health

Service and database health

Reconciliation

Method

Path

Description

POST

/api/v1/reconciliation/run?batchId=<batchId>

Run reconciliation for a synthetic/imported batch

GET

/api/v1/reconciliation/results?batchId=<batchId>

Retrieve reconciliation results

GET

/api/v1/reconciliation/exceptions

Retrieve/filter reconciliation exceptions

Example:

$batchId = "synthetic_<UUID>"

$result = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/reconciliation/run?batchId=$batchId" `
    -Method POST

Inspect results:

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

Group classifications:

$result |
    Group-Object matchType |
    Sort-Object Name |
    Select-Object Name, Count |
    Format-Table -AutoSize

Investigation and decision flow

The frontend uses the backend investigation and decision APIs to execute the exception-resolution workflow:

Exception
   ↓
AI Investigation
   ↓
Controller Decision
   ↓
Audit Event

The exact API contracts are maintained in the backend controllers and frontend API modules.

Development Roadmap

Phase 1 — Foundation

Status: Complete

Spring Boot backend

React/Vite frontend foundation

PostgreSQL integration

Environment configuration

Health checks

Base project architecture

Phase 2 — Financial Data Layer

Status: Complete

Phase 2A — Financial Domain

Payment model

Merchant order model

Settlement model

Refund model

Adjustment model

JPA repositories

Database persistence

Phase 2B — Provider & Synthetic Data

Provider client abstraction

Provider integration foundation

Synthetic financial data generator

Batch-based synthetic records

Multiple controlled reconciliation scenarios

Synthetic ground truth

Phase 2C — Reconciliation

Batch reconciliation

Payment-level reconciliation

Duplicate detection

Missing settlement detection

Refund detection

Adjustment detection

Timing detection

Exact matching

Fee difference detection

Tax difference detection

Unexplained mismatch detection

Confidence scoring

Reconciliation result persistence

Phase 2D — Exception & Evaluation Foundation

Reconciliation exception persistence

Exception APIs

Exception resolution service

Audit-related resolution data

Synthetic ground-truth evaluation foundation

Phase 3 — Robust Ingestion & Normalization

Status: Foundation implemented; further hardening remains

Implemented/foundation work:

CSV ingestion structure

Input validation foundation

Schema validation foundation

Normalization structure

Batch-level ingestion tracking

Provider-to-internal-model mapping foundation

Remaining hardening:

Broader duplicate/input-record handling

Comprehensive ingestion error reporting

Stronger reconciliation-ready data validation

Phase 4 — Evidence & Exception Intelligence

Status: Complete

Implemented:

Structured evidence collection

Related financial-record loading

Deterministic financial analysis

Evidence identifiers

Difference explanation

Missing-evidence analysis

Evidence-grounded investigation input

Phase 5 — AI Investigator

Status: Complete

Implemented:

Backend AI provider abstraction

Gemini integration

Structured investigation prompts

Evidence-grounded investigation

Deterministic financial analysis supplied as authoritative context

Structured AI response

Confidence validation

Evidence-reference validation

Investigation persistence

Customer-readable explanations

Missing-evidence handling

Alternative-explanation handling

Phase 6 — Controller / Decision Engine

Status: Complete

Implemented:

AI investigation → decision workflow

Bounded recommendation states

Confidence checks

Evidence checks

Consistency validation

Automatic resolution

Manual review

Insufficient-evidence handling

Decision persistence

Audit event creation

Phase 7 — Dashboard & Audit

Status: Complete

Implemented:

Overview metrics

Reconciliation results table

Exception queue

Investigation detail

AI decision visibility

Audit timeline

Human-review/status actions

Batch-run visibility

Readable financial formatting

Merchant-facing explanations

The dashboard is intended to make the controller understandable within a short operational review and make an automated decision traceable back to its evidence.

Phase 8 — Reliability, Evaluation & Final Demo

Status: Current

Focus areas:

50+ record synthetic evaluation batches

Match-rate measurement

Reconciliation classification evaluation

AI investigation evaluation

Throughput measurement

Failure handling

Retry behavior

Idempotency validation

Observability

Security review

Final end-to-end demo workflow

The current submission focus is to demonstrate throughput, measured reconciliation outcomes, honest exception reporting, and a traceable AI-assisted investigation without introducing unnecessary deployment infrastructure.

Evaluation Strategy

The synthetic framework will be used to evaluate both deterministic and AI-assisted components.

Reconciliation

Evaluate:

Scenario classification accuracy

Precision

Recall

False positive rate

False negative rate

Confusion matrix where applicable

Confidence

Evaluate:

Confidence calibration

High-confidence correctness

Low-confidence escalation behavior

AI Investigation

Evaluate:

Evidence grounding

Explanation correctness

Unsupported-claim rate

Recommended-action quality

Escalation quality

Resolution

Evaluate:

Automatic resolution correctness

Human escalation correctness

Financial-risk containment

For the final demonstration, the most important evidence is a sufficiently large synthetic batch, the resulting match/exception statistics, the exceptions the controller could not safely resolve, and at least one traceable AI-assisted investigation.

Design Principles

1. Deterministic first

If a financial fact can be established directly from structured records, use deterministic logic.

2. AI as an investigator

AI should reason over evidence and explain ambiguous cases.

3. No hallucinated financial facts

The AI must not fabricate:

Transactions

Amounts

Settlements

Refunds

Fees

Taxes

Dates

Customer information

4. Confidence is not truth

A high AI confidence score does not make an unsupported conclusion valid.

5. Human escalation is a feature

Uncertainty should result in controlled escalation rather than forced automation.

6. Auditability is mandatory

Every important decision should be traceable to:

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

7. Keep evaluation independent

Synthetic ground truth must remain separate from production reconciliation logic so that the system can genuinely measure its own performance.

8. Keep financial language merchant-readable

Internal implementation details should remain internal. The dashboard should communicate discrepancies, reasoning, evidence, and outcomes in language an operations user can understand.

Project Status

Current status: Core finance-operations loop implemented and demo-ready.

The project now demonstrates:

Financial Data
      ↓
Ingestion
      ↓
Deterministic Reconciliation
      ↓
Exception
      ↓
Evidence
      ↓
AI Investigation
      ↓
Confidence / Policy
      ↓
Resolution or Human Review
      ↓
Audit Trail
      ↓
Operations Dashboard

The immediate focus is final evaluation and demonstration using 50+ record synthetic batches, measured reconciliation outcomes, honest exception reporting, and an end-to-end traceable AI investigation.

The application is currently intended to be demonstrated locally rather than introducing deployment infrastructure solely for the submission.