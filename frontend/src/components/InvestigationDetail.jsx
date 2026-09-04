import { useEffect, useState } from 'react'
import {
  getException,
  investigateException,
  getExceptionDecision,
  getExceptionInvestigation,
  getExceptionAudit,
  updateExceptionStatus,
} from '../api/investigationApi.js'

function Badge({ children, type = 'default' }) {
  const classes = {
    default: 'bg-slate-100 text-slate-700',
    high: 'bg-red-100 text-red-700',
    medium: 'bg-amber-100 text-amber-700',
    low: 'bg-green-100 text-green-700',
    open: 'bg-blue-100 text-blue-700',
    investigating: 'bg-amber-100 text-amber-700',
    resolved: 'bg-green-100 text-green-700',
  }

  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${
        classes[type] || classes.default
      }`}
    >
      {children}
    </span>
  )
}

function formatAmount(value) {
  if (value === null || value === undefined) return '�'
  return Number(value).toFixed(4)
}

function formatDate(value) {
  if (!value) return '�'

  return new Date(value).toLocaleString()
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

      const [
        refreshedDecision,
        refreshedAudit,
      ] = await Promise.all([
        getExceptionDecision(exceptionId),
        getExceptionAudit(exceptionId),
      ])

      setDecision(refreshedDecision)
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
      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <button
          onClick={onBack}
          className="mb-5 text-sm font-medium text-slate-600 hover:text-slate-900"
        >
          ? Back to exceptions
        </button>

        <p className="text-sm text-slate-500">
          Loading investigation detail...
        </p>
      </section>
    )
  }

  if (error && !exception) {
    return (
      <section className="rounded-xl border border-red-200 bg-red-50 p-6">
        <button
          onClick={onBack}
          className="mb-5 text-sm font-medium text-slate-600 hover:text-slate-900"
        >
          ? Back to exceptions
        </button>

        <p className="text-sm text-red-700">{error}</p>
      </section>
    )
  }

  return (
    <section className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <button
            onClick={onBack}
            className="mb-2 text-sm font-medium text-slate-600 hover:text-slate-900"
          >
            ? Back to exceptions
          </button>

          <h2 className="text-2xl font-semibold text-slate-900">
            Investigation Detail
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            Exception #{exception.id}
          </p>
        </div>

        <button
          onClick={handleInvestigate}
          disabled={investigating}
          className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
        >
          {investigating ? 'Investigating...' : 'Investigate'}
        </button>
      </div>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h3 className="mb-4 text-lg font-semibold text-slate-900">
          Exception Summary
        </h3>

        <div className="grid gap-4 md:grid-cols-3">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">
              Payment Reference
            </p>
            <p className="mt-1 break-all text-sm font-medium text-slate-900">
              {paymentReference || exception.paymentReference || '�'}
            </p>
          </div>

          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">
              Category
            </p>
            <p className="mt-1 text-sm font-medium text-slate-900">
              {exception.category || '�'}
            </p>
          </div>

          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">
              Severity
            </p>
            <div className="mt-1">
              <Badge type={String(exception.severity || '').toLowerCase()}>
                {exception.severity || '�'}
              </Badge>
            </div>
          </div>

          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">
              Status
            </p>
            <div className="mt-1">
              <Badge type={String(exception.status || '').toLowerCase()}>
                {exception.status || '�'}
              </Badge>
            </div>
          </div>

          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">
              Expected Amount
            </p>
            <p className="mt-1 text-sm font-medium text-slate-900">
              {formatAmount(exception.expectedAmount)}
            </p>
          </div>

          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">
              Actual Amount
            </p>
            <p className="mt-1 text-sm font-medium text-slate-900">
              {formatAmount(exception.actualAmount)}
            </p>
          </div>

          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">
              Difference
            </p>
            <p className="mt-1 text-sm font-semibold text-slate-900">
              {formatAmount(exception.difference)}
            </p>
          </div>

          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">
              AI Confidence
            </p>
            <p className="mt-1 text-sm font-medium text-slate-900">
              {exception.aiConfidence != null
                ? `${(Number(exception.aiConfidence) * 100).toFixed(1)}%`
                : '�'}
            </p>
          </div>
        </div>

        {exception.evidenceSummary && (
          <div className="mt-5 rounded-lg bg-slate-50 p-4">
            <p className="text-xs uppercase tracking-wide text-slate-400">
              Existing Evidence Summary
            </p>
            <p className="mt-2 text-sm leading-6 text-slate-700">
              {exception.evidenceSummary}
            </p>
          </div>
        )}
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">
            AI Investigation
          </h3>

          {investigation && (
            <Badge type="investigating">
              {investigation.recommendedStatus}
            </Badge>
          )}
        </div>

        {!investigation ? (
          <div className="mt-4 rounded-lg bg-slate-50 p-5">
            <p className="text-sm text-slate-600">
              No investigation result has been generated in this view yet.
            </p>
            <p className="mt-1 text-xs text-slate-400">
              Select Investigate to analyze the available reconciliation
              evidence.
            </p>
          </div>
        ) : (
          <div className="mt-5 space-y-5">
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">
                Conclusion
              </p>
              <p className="mt-1 text-sm font-medium text-slate-900">
                {investigation.conclusion}
              </p>
            </div>

            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">
                Explanation
              </p>
              <p className="mt-1 text-sm leading-6 text-slate-700">
                {investigation.explanation}
              </p>
            </div>

            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">
                Confidence
              </p>
              <p className="mt-1 text-sm font-medium text-slate-900">
                {investigation.confidence != null
                  ? `${(Number(investigation.confidence) * 100).toFixed(1)}%`
                  : '�'}
              </p>
            </div>

            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">
                Evidence References
              </p>

              <div className="mt-2 flex flex-wrap gap-2">
                {investigation.evidenceReferences?.map(
                  (reference) => (
                    <span
                      key={reference}
                      className="rounded-md bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-700"
                    >
                      {reference}
                    </span>
                  )
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h3 className="text-lg font-semibold text-slate-900">
          Decision
        </h3>

        {!decision ? (
          <p className="mt-4 text-sm text-slate-500">
            No decision has been recorded for this exception.
          </p>
        ) : (
          <div className="mt-4 grid gap-4 md:grid-cols-3">
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">
                Outcome
              </p>
              <p className="mt-1 text-sm font-medium text-slate-900">
                {decision.outcome || '�'}
              </p>
            </div>

            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">
                Reason
              </p>
              <p className="mt-1 text-sm text-slate-700">
                {decision.reason || '�'}
              </p>
            </div>

            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">
                Created
              </p>
              <p className="mt-1 text-sm text-slate-700">
                {formatDate(decision.createdAt)}
              </p>
            </div>
          </div>
        )}
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h3 className="text-lg font-semibold text-slate-900">
          Human Review
        </h3>

        <p className="mt-1 text-sm text-slate-500">
          Record a merchant decision and keep the action in the audit trail.
        </p>

        <textarea
          value={reviewNote}
          onChange={(event) => setReviewNote(event.target.value)}
          placeholder="Optional review note or resolution reason"
          rows={3}
          className="mt-4 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-500"
          disabled={reviewing}
        />

        <div className="mt-4 flex flex-wrap gap-3">
          {exception.status === 'OPEN' && (
            <>
              <button
                onClick={() => handleReviewStatus('INVESTIGATING')}
                disabled={reviewing}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Mark Investigating
              </button>

              <button
                onClick={() => handleReviewStatus('IGNORED')}
                disabled={reviewing}
                className="rounded-lg border border-amber-300 px-4 py-2 text-sm font-medium text-amber-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Ignore
              </button>

              <button
                onClick={() => handleReviewStatus('RESOLVED')}
                disabled={reviewing}
                className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                Resolve
              </button>
            </>
          )}

          {exception.status === 'INVESTIGATING' && (
            <button
              onClick={() => handleReviewStatus('RESOLVED')}
              disabled={reviewing}
              className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
            >
              Resolve
            </button>
          )}

          {reviewing && (
            <span className="self-center text-sm text-slate-500">
              Saving review...
            </span>
          )}
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h3 className="text-lg font-semibold text-slate-900">
          Audit Timeline
        </h3>

        {auditHistory.length === 0 ? (
          <p className="mt-4 text-sm text-slate-500">
            No audit events recorded for this exception.
          </p>
        ) : (
          <div className="mt-5 space-y-4">
            {auditHistory.map((event) => (
              <div
                key={event.id}
                className="border-l-2 border-slate-200 pl-4"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-sm font-semibold text-slate-900">
                    {event.action}
                  </span>

                  <span className="text-xs text-slate-400">
                    {event.entityType}
                  </span>
                </div>

                <p className="mt-1 text-xs text-slate-500">
                  Actor: {event.actor || '�'} � {formatDate(event.createdAt)}
                </p>

                {event.decision && (
                  <p className="mt-2 text-sm text-slate-600">
                    {event.decision}
                  </p>
                )}

                {event.evidenceReference && (
                  <p className="mt-1 break-all text-xs text-slate-500">
                    Evidence: {event.evidenceReference}
                  </p>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}





