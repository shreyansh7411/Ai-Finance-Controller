import { useEffect, useState } from 'react'
import {
  getException,
  investigateException,
  getExceptionDecision,
  getExceptionInvestigation,
  getExceptionAudit,
  decideException,
  updateExceptionStatus,
} from '../api/investigationApi.js'
import {
  formatCurrency,
  formatPercentage,
  formatExceptionCategory,
  formatMatchType,
  formatStatus,
  formatSeverity,
  formatAuditAction,
  formatActor,
  formatEvidenceReference,
  formatDateTime,
} from '../utils/financialFormatting.js'

function Badge({ children, type = 'default' }) {
  const classes = {
    default: 'border-slate-200 bg-slate-100 text-slate-700',
    high: 'border-red-200 bg-red-50 text-red-700',
    medium: 'border-amber-200 bg-amber-50 text-amber-700',
    low: 'border-emerald-200 bg-emerald-50 text-emerald-700',
    critical: 'border-red-200 bg-red-50 text-red-700',
    open: 'border-sky-200 bg-sky-50 text-sky-700',
    investigating: 'border-amber-200 bg-amber-50 text-amber-700',
    resolved: 'border-emerald-200 bg-emerald-50 text-emerald-700',
    ignored: 'border-slate-200 bg-slate-100 text-slate-600',
    insufficient_evidence:
      'border-amber-200 bg-amber-50 text-amber-700',
    auto_resolve:
      'border-emerald-200 bg-emerald-50 text-emerald-700',
    manual_review:
      'border-amber-200 bg-amber-50 text-amber-700',
  }

  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-semibold tracking-wide ${
        classes[type] || classes.default
      }`}
    >
      {children}
    </span>
  )
}

function getStatusType(status) {
  return String(status || '').toLowerCase()
}

function getSeverityType(severity) {
  return String(severity || '').toLowerCase()
}

function SectionLabel({ children }) {
  return (
    <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-slate-400">
      {children}
    </p>
  )
}

function Metric({ label, value, emphasis = false }) {
  return (
    <div>
      <SectionLabel>{label}</SectionLabel>

      <p
        className={`mt-2 text-sm font-semibold ${
          emphasis ? 'text-slate-950' : 'text-slate-700'
        }`}
      >
        {value}
      </p>
    </div>
  )
}

function EvidenceList({ items, emptyText = 'No evidence recorded' }) {
  if (!items || items.length === 0) {
    return (
      <p className="mt-3 text-sm text-slate-400">
        {emptyText}
      </p>
    )
  }

  return (
    <ul className="mt-3 space-y-2">
      {items.map((item, index) => (
        <li
          key={`${item}-${index}`}
          className="flex gap-2.5 text-sm leading-6 text-slate-300"
        >
          <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-cyan-300" />
          <span>{item}</span>
        </li>
      ))}
    </ul>
  )
}

function formatEntityType(value) {
  if (!value) {
    return ''
  }

  const labels = {
    EXCEPTION: 'Exception',
    INVESTIGATION: 'Investigation',
    DECISION: 'Decision',
    PAYMENT: 'Payment',
    ORDER: 'Order',
    SETTLEMENT: 'Settlement',
    REFUND: 'Refund',
    ADJUSTMENT: 'Adjustment',
  }

  return labels[value] || humanizeTechnicalText(value)
}

function formatAuditDecision(value) {
  if (!value) {
    return ''
  }

  const text = String(value)

  const replacements = {
    AUTO_RESOLVE:
      'The controller automatically resolved the exception.',
    MANUAL_REVIEW:
      'The exception was sent for merchant review.',
    RESOLVED:
      'The exception was resolved.',
    IGNORED:
      'The exception was marked as ignored.',
    INVESTIGATING:
      'The exception was marked as under investigation.',
    INSUFFICIENT_EVIDENCE:
      'The available evidence was not sufficient to reach a reliable conclusion.',
  }

  if (replacements[text]) {
    return replacements[text]
  }

  return humanizeTechnicalText(text)
}

function humanizeTechnicalText(value) {
  return String(value)
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, (character) => character.toUpperCase())
}

function formatEvidenceSummary(summary) {
  if (!summary) {
    return null
  }

  const raw = String(summary).trim()

  if (!raw) {
    return null
  }

  /*
   * Evidence summaries may arrive as JSON, key/value text, or
   * already-readable prose. Prefer structured rendering when possible.
   */
  let parsed = null

  try {
    parsed = JSON.parse(raw)
  } catch {
    parsed = null
  }

  if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
    const entries = Object.entries(parsed)

    if (entries.length > 0) {
      return (
        <div className="grid gap-x-8 gap-y-3 sm:grid-cols-2">
          {entries.map(([key, value]) => (
            <div key={key}>
              <p className="text-xs font-medium text-slate-400">
                {formatEvidenceReference(key)}
              </p>

              <p className="mt-1 text-sm font-semibold text-slate-700">
                {formatEvidenceValue(key, value)}
              </p>
            </div>
          ))}
        </div>
      )
    }
  }

  /*
   * If the backend returns a technical key/value summary rather
   * than JSON, split the common separators and make the keys readable.
   */
  const parts = raw
    .split(/\s*(?:,|\||;)\s*/)
    .map((part) => part.trim())
    .filter(Boolean)

  const structuredParts = parts
    .map((part) => {
      const match = part.match(
        /^([A-Z][A-Z0-9_]*)\s*[:=]\s*(.+)$/
      )

      if (!match) {
        return null
      }

      return {
        key: match[1],
        value: match[2],
      }
    })
    .filter(Boolean)

  if (structuredParts.length >= 2) {
    return (
      <div className="grid gap-x-8 gap-y-3 sm:grid-cols-2">
        {structuredParts.map(({ key, value }, index) => (
          <div key={`${key}-${index}`}>
            <p className="text-xs font-medium text-slate-400">
              {formatEvidenceReference(key)}
            </p>

            <p className="mt-1 text-sm font-semibold text-slate-700">
              {formatEvidenceValue(key, value)}
            </p>
          </div>
        ))}
      </div>
    )
  }

  /*
   * If the backend already supplies natural language, preserve it
   * rather than attempting to rewrite meaningful prose.
   */
  return (
    <p className="max-w-4xl text-sm leading-6 text-slate-600">
      {humanizeTechnicalText(raw)}
    </p>
  )
}

function formatEvidenceValue(key, value) {
  if (
    value === null ||
    value === undefined ||
    value === ''
  ) {
    return '—'
  }

  const amountKeys = [
    'EXCEPTION_EXPECTED_AMOUNT',
    'EXCEPTION_ACTUAL_AMOUNT',
    'EXCEPTION_DIFFERENCE',
    'PAYMENT_AMOUNT',
    'ORDER_AMOUNT',
    'SETTLEMENT_AMOUNT',
    'SETTLEMENT_FEES',
    'SETTLEMENT_TAX',
    'REFUND_AMOUNT',
    'ADJUSTMENT_AMOUNT',
    'EXPECTED_AMOUNT',
    'ACTUAL_AMOUNT',
    'DIFFERENCE',
    'AMOUNT',
    'FEES',
    'TAX',
  ]

  if (amountKeys.includes(key)) {
    return formatCurrency(value)
  }

  if (key === 'ADJUSTMENT_TYPE') {
    return humanizeTechnicalText(value)
  }

  if (
    key === 'REFUND_ID' ||
    key === 'ADJUSTMENT_ID'
  ) {
    return `Record ${value}`
  }

  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }

  return humanizeTechnicalText(value)
}

export default function InvestigationDetail({
  exceptionId,
  paymentReference,
  onBack,
}) {
  const [exception, setException] = useState(null)
  const [investigation, setInvestigation] = useState(null)
  const [decision, setDecision] = useState(null)
  const [auditHistory, setAuditHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [investigating, setInvestigating] = useState(false)
  const [reviewing, setReviewing] = useState(false)
  const [reviewNote, setReviewNote] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function loadDetail() {
      setLoading(true)
      setError('')

      try {
        const [
          exceptionData,
          investigationData,
          decisionData,
          auditData,
        ] = await Promise.all([
          getException(exceptionId),
          getExceptionInvestigation(exceptionId),
          getExceptionDecision(exceptionId),
          getExceptionAudit(exceptionId),
        ])

        if (cancelled) return

        setException(exceptionData)
        setInvestigation(investigationData)
        setDecision(decisionData)
        setAuditHistory(auditData || [])
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Failed to load investigation detail')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadDetail()

    return () => {
      cancelled = true
    }
  }, [exceptionId])

  async function handleInvestigate() {
    setInvestigating(true)
    setError('')

    try {
      const result = await investigateException(exceptionId)
      setInvestigation(result)

      const decisionResult = await decideException(
        exceptionId,
        result
      )

      setDecision(decisionResult)

      const [refreshedException, refreshedAudit] = await Promise.all([
        getException(exceptionId),
        getExceptionAudit(exceptionId),
      ])

      setException(refreshedException)
      setAuditHistory(refreshedAudit || [])
    } catch (err) {
      setError(err.message || 'Investigation failed')
    } finally {
      setInvestigating(false)
    }
  }

  async function handleReviewStatus(targetStatus) {
    setReviewing(true)
    setError('')

    try {
      const updated = await updateExceptionStatus(
        exceptionId,
        targetStatus,
        'MERCHANT_UI',
        reviewNote || `Human review: ${targetStatus}`,
        targetStatus === 'RESOLVED' ? reviewNote : ''
      )

      setException(updated)

      const refreshedAudit = await getExceptionAudit(exceptionId)
      setAuditHistory(refreshedAudit || [])

      setReviewNote('')
    } catch (err) {
      setError(err.message || 'Failed to update exception status')
    } finally {
      setReviewing(false)
    }
  }

  if (loading) {
    return (
      <section className="mx-auto max-w-6xl">
        <button
          onClick={onBack}
          className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-slate-900"
        >
          <span>←</span>
          Back to exceptions
        </button>

        <div className="flex min-h-[420px] items-center justify-center rounded-2xl border border-slate-200/80 bg-white/70">
          <div className="text-center">
            <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-2 border-slate-200 border-t-slate-700" />

            <p className="text-sm font-medium text-slate-600">
              Loading investigation
            </p>

            <p className="mt-1 text-xs text-slate-400">
              Fetching exception evidence and decision history
            </p>
          </div>
        </div>
      </section>
    )
  }

  if (error && !exception) {
    return (
      <section className="mx-auto max-w-6xl">
        <button
          onClick={onBack}
          className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-slate-900"
        >
          <span>←</span>
          Back to exceptions
        </button>

        <div className="rounded-2xl border border-red-200 bg-red-50/70 p-6">
          <SectionLabel>Investigation error</SectionLabel>

          <p className="mt-2 text-sm font-medium text-red-700">
            {error}
          </p>
        </div>
      </section>
    )
  }

  const severityType = getSeverityType(exception.severity)
  const statusType = getStatusType(exception.status)

  const confidence =
    exception.aiConfidence != null
      ? Number(exception.aiConfidence)
      : null

  const investigationConfidence =
    investigation?.confidence != null
      ? Number(investigation.confidence)
      : null

  return (
    <section className="mx-auto max-w-6xl space-y-7 pb-10">
      <header className="flex flex-col gap-5 border-b border-slate-200/80 pb-6 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <button
            onClick={onBack}
            className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-slate-900"
          >
            <span>←</span>
            Back to exceptions
          </button>

          <div className="flex flex-wrap items-center gap-2">
            <Badge type={severityType}>
              {formatSeverity(exception.severity)}
            </Badge>

            <Badge type={statusType}>
              {formatStatus(exception.status)}
            </Badge>

            <span className="text-xs text-slate-400">
              Exception #{exception.id}
            </span>
          </div>

          <h1 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">
            Investigation
          </h1>

          <p className="mt-1 max-w-2xl text-sm text-slate-500">
            Trace the discrepancy from source records through AI
            analysis, decisioning, and merchant review.
          </p>
        </div>

        <button
          onClick={handleInvestigate}
          disabled={investigating}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <span className="text-cyan-300">✦</span>

          {investigating
            ? 'Analyzing evidence...'
            : 'Run AI investigation'}
        </button>
      </header>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50/70 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Exception overview */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <div>
            <SectionLabel>Exception overview</SectionLabel>

            <p className="mt-1 text-sm text-slate-500">
              Core reconciliation facts
            </p>
          </div>

          {paymentReference && (
            <span className="hidden font-mono text-xs text-slate-400 md:block">
              {paymentReference}
            </span>
          )}
        </div>

        <div className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white/80 shadow-[0_8px_30px_rgba(15,23,42,0.04)]">
          <div className="grid divide-y divide-slate-100 md:grid-cols-2 md:divide-x md:divide-y-0">
            <div className="p-6">
              <div className="grid gap-6 sm:grid-cols-2">
                <Metric
                  label="Payment reference"
                  value={
                    paymentReference ||
                    exception.paymentReference ||
                    '—'
                  }
                  emphasis
                />

                <Metric
                  label="Category"
                  value={formatExceptionCategory(exception.category)}
                  emphasis
                />

                <Metric
                  label="Expected amount"
                  value={formatCurrency(exception.expectedAmount)}
                  emphasis
                />

                <Metric
                  label="Actual amount"
                  value={formatCurrency(exception.actualAmount)}
                  emphasis
                />
              </div>
            </div>

            <div className="p-6">
              <div className="grid gap-6 sm:grid-cols-2">
                <div>
                  <SectionLabel>Difference</SectionLabel>

                  <p className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
                    {formatCurrency(exception.difference)}
                  </p>
                </div>

                <div>
                  <SectionLabel>AI confidence</SectionLabel>

                  <p className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
                    {confidence != null
                      ? formatPercentage(confidence)
                      : '—'}
                  </p>
                </div>

                <Metric
                  label="Match type"
                  value={formatMatchType(exception.matchType)}
                />

                <Metric
                  label="Resolution"
                  value={
                    exception.resolution
                      ? formatStatus(exception.resolution)
                      : 'Pending'
                  }
                />
              </div>
            </div>
          </div>

          {exception.evidenceSummary && (
            <div className="border-t border-slate-100 bg-slate-50/60 px-6 py-5">
              <SectionLabel>Evidence summary</SectionLabel>

              <div className="mt-4">
                {formatEvidenceSummary(exception.evidenceSummary)}
              </div>
            </div>
          )}
        </div>
      </section>

      {/* AI Investigation */}
      <section>
        <div className="mb-3 flex items-end justify-between">
          <div>
            <SectionLabel>AI reasoning</SectionLabel>

            <p className="mt-1 text-sm text-slate-500">
              Evidence-backed investigation generated from
              reconciliation data
            </p>
          </div>

          {investigation && (
            <Badge
              type={getStatusType(
                investigation.recommendedStatus
              )}
            >
              {formatStatus(investigation.recommendedStatus)}
            </Badge>
          )}
        </div>

        {!investigation ? (
          <div className="overflow-hidden rounded-2xl bg-[#102a2e] p-7 text-white shadow-[0_12px_40px_rgba(16,42,46,0.12)]">
            <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
              <div className="max-w-2xl">
                <div className="mb-3 flex items-center gap-2">
                  <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-cyan-400/10 text-cyan-300">
                    ✦
                  </span>

                  <span className="text-xs font-semibold uppercase tracking-[0.16em] text-cyan-300">
                    AI investigator
                  </span>
                </div>

                <h3 className="text-xl font-semibold tracking-tight">
                  Understand what caused the discrepancy.
                </h3>

                <p className="mt-2 text-sm leading-6 text-slate-300">
                  The investigator will analyze the available
                  reconciliation evidence and produce an explanation,
                  confidence score, and recommended status.
                </p>
              </div>

              <button
                onClick={handleInvestigate}
                disabled={investigating}
                className="shrink-0 rounded-xl bg-white px-5 py-2.5 text-sm font-semibold text-slate-900 transition hover:bg-slate-100 disabled:opacity-50"
              >
                {investigating
                  ? 'Analyzing...'
                  : 'Investigate now'}
              </button>
            </div>
          </div>
        ) : (
          <div className="overflow-hidden rounded-2xl bg-[#102a2e] text-white shadow-[0_12px_40px_rgba(16,42,46,0.12)]">
            <div className="border-b border-white/10 px-6 py-5 md:px-7">
              <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                <div className="max-w-3xl">
                  <div className="flex items-center gap-2">
                    <span className="text-cyan-300">✦</span>

                    <span className="text-[10px] font-semibold uppercase tracking-[0.16em] text-cyan-300">
                      AI conclusion
                    </span>
                  </div>

                  <p className="mt-2 text-lg font-medium leading-7 text-white">
                    {investigation.conclusion}
                  </p>
                </div>

                <div className="shrink-0 rounded-xl border border-white/10 bg-white/5 px-4 py-3">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-slate-400">
                    Confidence
                  </p>

                  <p className="mt-1 text-xl font-semibold text-white">
                    {investigationConfidence != null
                      ? formatPercentage(investigationConfidence)
                      : '—'}
                  </p>
                </div>
              </div>
            </div>

            <div className="divide-y divide-white/10">
              <div className="grid divide-y divide-white/10 md:grid-cols-2 md:divide-x md:divide-y-0">
                <div className="p-6 md:p-7">
                  <SectionLabel>What happened</SectionLabel>

                  <p className="mt-3 text-sm leading-7 text-slate-300">
                    {investigation.whatHappened || '—'}
                  </p>
                </div>

                <div className="p-6 md:p-7">
                  <SectionLabel>Root cause</SectionLabel>

                  <p className="mt-3 text-sm leading-7 text-slate-300">
                    {investigation.rootCause || '—'}
                  </p>
                </div>
              </div>

              <div className="grid divide-y divide-white/10 md:grid-cols-2 md:divide-x md:divide-y-0">
                <div className="p-6 md:p-7">
                  <SectionLabel>Financial impact</SectionLabel>

                  <p className="mt-3 text-sm leading-7 text-slate-300">
                    {investigation.financialImpact || '—'}
                  </p>
                </div>

                <div className="p-6 md:p-7">
                  <SectionLabel>Why this conclusion</SectionLabel>

                  <p className="mt-3 text-sm leading-7 text-slate-300">
                    {investigation.confidenceReasoning || '—'}
                  </p>
                </div>
              </div>

              <div className="grid divide-y divide-white/10 md:grid-cols-2 md:divide-x md:divide-y-0">
                <div className="p-6 md:p-7">
                  <SectionLabel>Supporting evidence</SectionLabel>

                  <EvidenceList
                    items={investigation.supportingEvidence}
                  />
                </div>

                <div className="p-6 md:p-7">
                  <SectionLabel>Recommended action</SectionLabel>

                  <p className="mt-3 text-sm leading-7 text-slate-300">
                    {investigation.recommendedAction || '—'}
                  </p>
                </div>
              </div>

              {(investigation.alternativeExplanations?.length > 0 ||
                investigation.missingEvidence?.length > 0) && (
                <div className="grid divide-y divide-white/10 md:grid-cols-2 md:divide-x md:divide-y-0">
                  <div className="p-6 md:p-7">
                    <SectionLabel>
                      Alternative explanations
                    </SectionLabel>

                    <EvidenceList
                      items={investigation.alternativeExplanations}
                      emptyText="No meaningful alternatives identified"
                    />
                  </div>

                  <div className="p-6 md:p-7">
                    <SectionLabel>Missing evidence</SectionLabel>

                    <EvidenceList
                      items={investigation.missingEvidence}
                      emptyText="No additional evidence required"
                    />
                  </div>
                </div>
              )}
            </div>

            <div className="border-t border-white/10 bg-black/10 px-6 py-5 md:px-7">
              <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                <div>
                  <SectionLabel>Recommended handling</SectionLabel>

                  <div className="mt-3">
                    <Badge
                      type={getStatusType(
                        investigation.recommendedStatus
                      )}
                    >
                      {formatStatus(
                        investigation.recommendedStatus
                      )}
                    </Badge>
                  </div>
                </div>

                <div className="max-w-xl md:text-right">
                  <SectionLabel>Evidence used</SectionLabel>

                  <div className="mt-3 flex flex-wrap gap-2 md:justify-end">
                    {investigation.evidenceReferences?.length > 0 ? (
                      investigation.evidenceReferences.map(
                        (reference) => (
                          <span
                            key={reference}
                            className="rounded-lg border border-white/10 bg-white/5 px-2.5 py-1.5 text-[10px] text-slate-300"
                          >
                            {formatEvidenceReference(reference)}
                          </span>
                        )
                      )
                    ) : (
                      <span className="text-xs text-slate-400">
                        No evidence references recorded
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </section>

      {/* Decision */}
      <section>
        <div className="mb-3">
          <SectionLabel>Decision engine</SectionLabel>

          <p className="mt-1 text-sm text-slate-500">
            Recorded outcome derived from the investigation
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200/80 bg-white/70 p-6">
          {!decision ? (
            <div className="flex items-center gap-4">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-400">
                —
              </div>

              <div>
                <p className="text-sm font-semibold text-slate-800">
                  No decision recorded
                </p>

                <p className="mt-1 text-xs text-slate-500">
                  The decision engine has not produced a decision
                  for this exception yet.
                </p>
              </div>
            </div>
          ) : (
            <div className="grid gap-7 md:grid-cols-[0.7fr_1.5fr_0.8fr]">
              <div>
                <SectionLabel>Outcome</SectionLabel>

                <p className="mt-2 text-lg font-semibold text-slate-950">
                  {formatStatus(decision.outcome)}
                </p>
              </div>

              <div>
                <SectionLabel>Reason</SectionLabel>

                <p className="mt-2 text-sm leading-6 text-slate-600">
                  {formatAuditDecision(decision.reason)}
                </p>
              </div>

              <div>
                <SectionLabel>Created</SectionLabel>

                <p className="mt-2 text-sm text-slate-600">
                  {formatDateTime(decision.createdAt)}
                </p>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* Human review */}
      <section>
        <div className="mb-3">
          <SectionLabel>Merchant review</SectionLabel>

          <p className="mt-1 text-sm text-slate-500">
            Override or confirm the controller's handling of this
            exception
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200/80 bg-white/70 p-6">
          <div className="max-w-3xl">
            <textarea
              value={reviewNote}
              onChange={(event) =>
                setReviewNote(event.target.value)
              }
              placeholder="Add an optional review note or resolution reason..."
              rows={3}
              className="w-full resize-none rounded-xl border border-slate-200 bg-slate-50/70 px-4 py-3 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-slate-400 focus:bg-white focus:ring-4 focus:ring-slate-100"
              disabled={reviewing}
            />
          </div>

          <div className="mt-5 flex flex-wrap items-center gap-2.5">
            {exception.status === 'OPEN' && (
              <>
                <button
                  onClick={() =>
                    handleReviewStatus('INVESTIGATING')
                  }
                  disabled={reviewing}
                  className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50 disabled:opacity-50"
                >
                  Mark investigating
                </button>

                <button
                  onClick={() => handleReviewStatus('IGNORED')}
                  disabled={reviewing}
                  className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-2.5 text-sm font-semibold text-amber-700 transition hover:bg-amber-100 disabled:opacity-50"
                >
                  Ignore exception
                </button>

                <button
                  onClick={() => handleReviewStatus('RESOLVED')}
                  disabled={reviewing}
                  className="rounded-xl bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:opacity-50"
                >
                  Resolve exception
                </button>
              </>
            )}

            {exception.status === 'INVESTIGATING' && (
              <button
                onClick={() => handleReviewStatus('RESOLVED')}
                disabled={reviewing}
                className="rounded-xl bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:opacity-50"
              >
                Resolve exception
              </button>
            )}

            {reviewing && (
              <span className="ml-1 text-xs font-medium text-slate-400">
                Saving review...
              </span>
            )}
          </div>
        </div>
      </section>

      {/* Audit timeline */}
      <section>
        <div className="mb-3">
          <SectionLabel>Audit trail</SectionLabel>

          <p className="mt-1 text-sm text-slate-500">
            Every important decision and review action
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200/80 bg-white/70 p-6">
          {auditHistory.length === 0 ? (
            <div className="py-8 text-center">
              <p className="text-sm font-medium text-slate-600">
                No audit events recorded
              </p>

              <p className="mt-1 text-xs text-slate-400">
                Actions taken on this exception will appear here.
              </p>
            </div>
          ) : (
            <div className="relative ml-2">
              <div className="absolute bottom-2 left-[7px] top-2 w-px bg-slate-200" />

              <div className="space-y-7">
                {auditHistory.map((event) => (
                  <div
                    key={event.id}
                    className="relative flex gap-5"
                  >
                    <div className="relative z-10 mt-1 flex h-4 w-4 shrink-0 items-center justify-center rounded-full border-4 border-white bg-slate-400 shadow-sm" />

                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-semibold text-slate-900">
                          {formatAuditAction(event.action)}
                        </span>

                        {event.entityType && (
                          <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[10px] font-semibold tracking-wide text-slate-500">
                            {formatEntityType(event.entityType)}
                          </span>
                        )}
                      </div>

                      <p className="mt-1 text-xs text-slate-400">
                        {formatActor(event.actor)} ·{' '}
                        {formatDateTime(event.createdAt)}
                      </p>

                      {event.decision && (
                        <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
                          {formatAuditDecision(event.decision)}
                        </p>
                      )}

                      {event.evidenceReference && (
                        <div className="mt-3 max-w-3xl rounded-lg bg-slate-50 px-3 py-2">
                          <p className="text-xs leading-5 text-slate-500">
                            Evidence:{' '}
                            {formatEvidenceReference(
                              event.evidenceReference
                            )}
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </section>
    </section>
  )
}
