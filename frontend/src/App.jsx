import { Fragment, useEffect, useState } from 'react'
import { getIngestionBatches, uploadCsv } from './api/ingestionBatchApi.js'
import InvestigationDetail from './components/InvestigationDetail.jsx'
import { getDashboardMetrics } from './api/dashboardApi.js'
import {
  getReconciliationResults,
  getReconciliationExceptions,
  runReconciliation,
} from './api/reconciliationApi.js'
import {
  formatCurrency,
  formatPercentage,
  formatExceptionCategory,
  formatMatchType,
  formatStatus,
  formatSeverity,
  formatDateTime,
} from './utils/financialFormatting.js'

const MAX_RENDERED_ROWS = 100
const INITIAL_VISIBLE_ROWS = 5
const MAX_VISIBLE_ROWS = 20

function toLogicalBatches(rawBatches) {
  const groups = new Map()

  rawBatches.forEach((batch) => {
    const startedAt = batch.startedAt || batch.completedAt
    const timestamp = startedAt ? new Date(startedAt).getTime() : 0
    const groupKey = timestamp
      ? new Date(timestamp).toISOString().slice(0, 16)
      : batch.batchId

    if (!groups.has(groupKey)) {
      groups.set(groupKey, {
        ...batch,
        logicalBatchId: `BATCH-${groupKey.replace(/[-:T]/g, '').slice(0, 12)}`,
        paymentBatchId: null,
        paymentRecords: 0,
        sourceBatchIds: [],
        fileCount: 0,
      })
    }

    const group = groups.get(groupKey)
    group.sourceBatchIds.push(batch.batchId)
    group.fileCount += 1

    if (batch.entityType === 'PAYMENT') {
      group.paymentBatchId = batch.batchId
      group.paymentRecords = batch.importedRows ?? batch.totalRows ?? 0
      group.startedAt = batch.startedAt || group.startedAt
      group.completedAt = batch.completedAt || group.completedAt
    }
  })

  return [...groups.values()].sort(
    (first, second) =>
      new Date(second.startedAt || 0) - new Date(first.startedAt || 0)
  )
}

function StatusDot({ status }) {
  const colors = {
    UP: 'bg-emerald-400',
    DOWN: 'bg-red-400',
    PROCESSING: 'bg-sky-400',
    COMPLETED: 'bg-emerald-400',
    COMPLETED_WITH_ERRORS: 'bg-amber-400',
    FAILED: 'bg-red-400',
  }

  return (
    <span
      className={`h-2 w-2 rounded-full ${
        colors[status] || 'bg-slate-400'
      }`}
    />
  )
}

function StatusBadge({ status, type = 'health' }) {
  const styles =
    type === 'batch'
      ? {
          PROCESSING: 'bg-sky-50 text-sky-700 ring-sky-200',
          COMPLETED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
          COMPLETED_WITH_ERRORS:
            'bg-amber-50 text-amber-700 ring-amber-200',
          FAILED: 'bg-red-50 text-red-700 ring-red-200',
        }
      : {
          UP: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
          DOWN: 'bg-red-50 text-red-700 ring-red-200',
        }

  const style =
    styles[status] || 'bg-slate-50 text-slate-600 ring-slate-200'

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-[11px] font-semibold tracking-wide ring-1 ${style}`}
    >
      <StatusDot status={status} />
      {formatStatus(status)}
    </span>
  )
}

function ExceptionStatusBadge({ status }) {
  const styles = {
    OPEN: 'bg-red-50 text-red-700 ring-red-200',
    INVESTIGATING: 'bg-sky-50 text-sky-700 ring-sky-200',
    RESOLVED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  }

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-[11px] font-semibold tracking-wide ring-1 ${
        styles[status] || 'bg-slate-50 text-slate-600 ring-slate-200'
      }`}
    >
      <span
        className={`h-1.5 w-1.5 rounded-full ${
          status === 'OPEN'
            ? 'bg-red-500'
            : status === 'INVESTIGATING'
              ? 'bg-sky-500'
              : status === 'RESOLVED'
                ? 'bg-emerald-500'
                : 'bg-slate-400'
        }`}
      />
      {formatStatus(status)}
    </span>
  )
}

function SeverityBadge({ severity }) {
  const styles = {
    HIGH: 'text-red-700',
    MEDIUM: 'text-amber-700',
    LOW: 'text-emerald-700',
  }

  const dots = {
    HIGH: 'bg-red-500',
    MEDIUM: 'bg-amber-500',
    LOW: 'bg-emerald-500',
  }

  return (
    <span
      className={`inline-flex items-center gap-2 text-xs font-semibold ${
        styles[severity] || 'text-slate-500'
      }`}
    >
      <span
        className={`h-2 w-2 rounded-full ${
          dots[severity] || 'bg-slate-400'
        }`}
      />
      {formatSeverity(severity)}
    </span>
  )
}

function SectionHeading({ eyebrow, title, description, right }) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        {eyebrow && (
          <p className="mb-2 text-[11px] font-bold uppercase tracking-[0.18em] text-cyan-700">
            {eyebrow}
          </p>
        )}

        <h2 className="text-2xl font-semibold tracking-tight text-slate-950">
          {title}
        </h2>

        {description && (
          <p className="mt-1.5 max-w-2xl text-sm leading-6 text-slate-500">
            {description}
          </p>
        )}
      </div>

      {right}
    </div>
  )
}

function EmptyState({ title, description }) {
  return (
    <div className="flex min-h-40 flex-col items-center justify-center px-6 py-12 text-center">
      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-500">
        <span className="text-lg">—</span>
      </div>

      <p className="mt-4 text-sm font-semibold text-slate-800">
        {title}
      </p>

      <p className="mt-1 max-w-md text-xs leading-5 text-slate-500">
        {description}
      </p>
    </div>
  )
}

function OverviewMetrics({
  metrics,
  loading,
  error,
  batches,
  selectedBatchId,
  onBatchChange,
}) {
  if (loading) {
    return (
      <section>
        <SectionHeading
          eyebrow="Controller overview"
          title="Reconciliation pulse"
          description="Current financial reconciliation and exception activity."
        />

        <div className="animate-pulse rounded-2xl bg-white p-8 shadow-sm ring-1 ring-slate-200/70">
          <div className="h-5 w-32 rounded bg-slate-200" />
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            {[1, 2, 3, 4, 5].map((item) => (
              <div
                key={item}
                className="h-28 rounded-xl bg-slate-100"
              />
            ))}
          </div>
        </div>
      </section>
    )
  }

  if (error) {
    return (
      <section>
        <div className="rounded-2xl border border-red-200 bg-red-50 px-5 py-4">
          <p className="text-sm font-medium text-red-800">
            {error}
          </p>
        </div>
      </section>
    )
  }

  if (!metrics) {
    return null
  }

  const cards = [
    {
      label: 'Total records',
      value: metrics.totalRecords,
      description: 'Records processed',
      emphasis: 'text-slate-950',
    },
    {
      label: 'Matched',
      value: metrics.matchedRecords,
      description: 'Successfully reconciled',
      emphasis: 'text-emerald-700',
    },
    {
      label: 'Exceptions',
      value: metrics.exceptionRecords,
      description: 'Require attention',
      emphasis: 'text-amber-700',
    },
    {
      label: 'AI resolved',
      value: metrics.aiResolvedRecords,
      description: 'Resolved automatically',
      emphasis: 'text-cyan-700',
    },
    {
      label: 'Unresolved',
      value: metrics.unresolvedRecords,
      description: 'Awaiting action',
      emphasis: 'text-red-700',
    },
  ]

  return (
    <section>
      <SectionHeading
        eyebrow="Controller overview"
        title="Reconciliation pulse"
        description="A concise view of financial records, risk, and automated resolution."
        right={
          <select
            value={selectedBatchId}
            onChange={(event) => onBatchChange(event.target.value)}
            className="min-w-56 rounded-xl border-0 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 shadow-sm ring-1 ring-slate-200 transition focus:outline-none focus:ring-2 focus:ring-cyan-500"
          >
            <option value="">Select batch</option>

            {batches.map((batch) => (
              <option
                key={batch.logicalBatchId}
                value={batch.paymentBatchId || ''}
              >
                {batch.logicalBatchId} · {batch.paymentRecords} payments
              </option>
            ))}
          </select>
        }
      />

      <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/70">
        <div className="grid grid-cols-1 divide-y divide-slate-100 sm:grid-cols-2 sm:divide-x sm:divide-y-0 lg:grid-cols-5">
          {cards.map((card) => (
            <div
              key={card.label}
              className="group px-5 py-6 transition hover:bg-slate-50/70"
            >
              <p className="text-xs font-medium text-slate-500">
                {card.label}
              </p>

              <p
                className={`mt-3 text-3xl font-semibold tracking-tight ${card.emphasis}`}
              >
                {card.value}
              </p>

              <p className="mt-1 text-[11px] text-slate-400">
                {card.description}
              </p>
            </div>
          ))}
        </div>

        <div className="grid border-t border-slate-100 md:grid-cols-2">
          <div className="px-6 py-6 md:border-r md:border-slate-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
                  Match rate
                </p>

                <p className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
                  {formatPercentage(
                    Number(metrics.matchRate || 0) / 100
                  )}
                </p>
              </div>

              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50 text-sm font-bold text-emerald-700">
                ✓
              </div>
            </div>

            <div className="mt-5 h-1.5 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-emerald-500 transition-all"
                style={{
                  width: `${Math.min(
                    Math.max(Number(metrics.matchRate) || 0, 0),
                    100
                  )}%`,
                }}
              />
            </div>

            <p className="mt-2 text-xs text-slate-400">
              Matched records / total records
            </p>
          </div>

          <div className="px-6 py-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
                  Resolution rate
                </p>

                <p className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
                  {formatPercentage(
                    Number(metrics.resolutionRate || 0) / 100
                  )}
                </p>
              </div>

              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-cyan-50 text-sm font-bold text-cyan-700">
                ✦
              </div>
            </div>

            <div className="mt-5 h-1.5 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-cyan-500 transition-all"
                style={{
                  width: `${Math.min(
                    Math.max(Number(metrics.resolutionRate) || 0, 0),
                    100
                  )}%`,
                }}
              />
            </div>

            <p className="mt-2 text-xs text-slate-400">
              AI-resolved records / exception records
            </p>
          </div>
        </div>
      </div>

      <div className="mt-6">
        <div className="mb-4 flex items-end justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
              Exception intelligence
            </p>

            <h3 className="mt-1 text-lg font-semibold text-slate-950">
              Where attention is going
            </h3>
          </div>
        </div>

        <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/70">
          {Object.keys(metrics.exceptionBreakdown || {}).length === 0 ? (
            <EmptyState
              title="No exceptions found"
              description="The controller has not detected any exception categories for this dataset."
            />
          ) : (
            <div className="grid divide-y divide-slate-100 sm:grid-cols-2 sm:divide-x sm:divide-y-0 lg:grid-cols-4">
              {Object.entries(metrics.exceptionBreakdown).map(
                ([category, count]) => (
                  <div
                    key={category}
                    className="px-5 py-5 transition hover:bg-slate-50/70"
                  >
                    <p className="truncate text-xs font-semibold uppercase tracking-[0.08em] text-slate-400">
                      {formatExceptionCategory(category)}
                    </p>

                    <p className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
                      {count}
                    </p>

                    <p className="mt-1 text-[11px] text-slate-400">
                      exceptions
                    </p>
                  </div>
                )
              )}
            </div>
          )}
        </div>
      </div>
    </section>
  )
}

function ReconciliationResults({
  batches,
  selectedBatchId,
  onBatchChange,
  results,
  loading,
  error,
}) {
  const [activeTab, setActiveTab] = useState('MATCHED')
  const [search, setSearch] = useState('')
  const [expandedId, setExpandedId] = useState(null)
  const [displayMode, setDisplayMode] = useState('initial')

  const filteredResults = results
    .filter((result) => result.status === activeTab)
    .filter(
      (result) =>
        !search ||
        String(result.paymentReference || '')
          .toLowerCase()
          .includes(search.toLowerCase())
    )

  const visibleResults = filteredResults.slice(
    0,
    displayMode === 'all'
      ? filteredResults.length
      : displayMode === 'more'
        ? MAX_VISIBLE_ROWS
        : INITIAL_VISIBLE_ROWS
  )

  const matchedCount = results.filter(
    (result) => result.status === 'MATCHED'
  ).length

  const exceptionCount = results.filter(
    (result) => result.status === 'EXCEPTION'
  ).length

  return (
    <section>
      <SectionHeading
        eyebrow="Financial control"
        title="Reconciliation"
        description="Compare payment records against provider outcomes and inspect discrepancies."
        right={
          <select
            value={selectedBatchId}
            onChange={(event) => onBatchChange(event.target.value)}
            className="min-w-56 rounded-xl border-0 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 shadow-sm ring-1 ring-slate-200 transition focus:outline-none focus:ring-2 focus:ring-cyan-500"
          >
            <option value="">Select batch</option>

            {batches.map((batch) => (
              <option
                key={batch.logicalBatchId}
                value={batch.paymentBatchId || batch.logicalBatchId}
              >
                {batch.logicalBatchId} · {batch.paymentRecords} payments
              </option>
            ))}
          </select>
        }
      />

      <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/70">
        <div className="flex flex-col gap-4 border-b border-slate-100 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="inline-flex w-fit rounded-xl bg-slate-100 p-1">
            {[
              ['MATCHED', 'Matched', matchedCount],
              ['EXCEPTION', 'Exceptions', exceptionCount],
            ].map(([tab, label, count]) => (
              <button
                key={tab}
                onClick={() => {
                  setActiveTab(tab)
                  setDisplayMode('initial')
                  setExpandedId(null)
                }}
                className={`rounded-lg px-4 py-2 text-xs font-semibold transition ${
                  activeTab === tab
                    ? 'bg-white text-slate-950 shadow-sm'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                {label}
                <span
                  className={`ml-2 ${
                    activeTab === tab
                      ? 'text-slate-500'
                      : 'text-slate-400'
                  }`}
                >
                  {count}
                </span>
              </button>
            ))}
          </div>

          <div className="relative">
            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
              ⌕
            </span>

            <input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
                setDisplayMode('initial')
              }}
              placeholder="Search payment reference"
              className="w-full rounded-xl border-0 bg-slate-50 py-2.5 pl-9 pr-4 text-xs text-slate-700 ring-1 ring-slate-200 transition placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-cyan-500 sm:w-64"
            />
          </div>
        </div>

        {loading && (
          <div className="px-6 py-16 text-center text-sm text-slate-500">
            Loading reconciliation results...
          </div>
        )}

        {!loading && error && (
          <div className="m-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}

        {!loading && !error && !selectedBatchId && (
          <EmptyState
            title="Select a batch"
            description="Choose an ingestion batch above to view its reconciliation results."
          />
        )}

        {!loading &&
          !error &&
          selectedBatchId &&
          filteredResults.length === 0 && (
            <EmptyState
              title="No reconciliation results"
              description="There are no results matching the current view and search criteria."
            />
          )}

        {!loading &&
          !error &&
          filteredResults.length > 0 && (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[700px] text-sm">
                  <colgroup>
                    <col className="w-[42%]" />
                    <col className="w-[35%]" />
                    <col className="w-[23%]" />
                  </colgroup>

                  <thead className="bg-slate-50/80">
                    <tr>
                      {['Payment', 'Match type', 'Status'].map(
                        (heading) => (
                          <th
                            key={heading}
                            className="px-7 py-3.5 text-left text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400"
                          >
                            {heading}
                          </th>
                        )
                      )}
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-slate-100">
                    {visibleResults.map((result) => (
                      <Fragment key={result.id}>
                        <tr
                          onClick={() =>
                            setExpandedId(
                              expandedId === result.id
                                ? null
                                : result.id
                            )
                          }
                          className="cursor-pointer transition hover:bg-slate-50/70"
                        >
                          <td className="px-7 py-4">
                            <span className="font-semibold text-slate-900">
                              {result.paymentReference || '—'}
                            </span>
                          </td>

                          <td className="px-7 py-4">
                            <span className="text-xs font-medium text-slate-500">
                              {formatMatchType(result.matchType)}
                            </span>
                          </td>

                          <td className="px-7 py-4">
                            <span
                              className={`inline-flex items-center gap-2 text-[11px] font-semibold ${
                                result.status === 'MATCHED'
                                  ? 'text-emerald-700'
                                  : 'text-amber-700'
                              }`}
                            >
                              <span
                                className={`h-2 w-2 rounded-full ${
                                  result.status === 'MATCHED'
                                    ? 'bg-emerald-500'
                                    : 'bg-amber-500'
                                }`}
                              />

                              {result.status === 'MATCHED'
                                ? 'Matched'
                                : formatStatus(result.status)}
                            </span>
                          </td>
                        </tr>

                        {expandedId === result.id && (
                          <tr className="bg-slate-50">
                            <td colSpan="3" className="px-7 py-5">
                              <div className="grid gap-5 sm:grid-cols-4">
                                <div>
                                  <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
                                    Payment
                                  </p>
                                  <p className="mt-1 text-sm font-semibold text-slate-800">
                                    {result.paymentReference || '—'}
                                  </p>
                                </div>

                                <div>
                                  <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
                                    Expected
                                  </p>
                                  <p className="mt-1 text-sm font-semibold text-slate-800">
                                    {formatCurrency(result.expectedAmount)}
                                  </p>
                                </div>

                                <div>
                                  <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
                                    Actual
                                  </p>
                                  <p className="mt-1 text-sm font-semibold text-slate-800">
                                    {formatCurrency(result.actualAmount)}
                                  </p>
                                </div>

                                <div>
                                  <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
                                    Difference
                                  </p>
                                  <p className="mt-1 text-sm font-semibold text-slate-800">
                                    {formatCurrency(result.difference)}
                                  </p>
                                </div>
                              </div>
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3">
                <p className="text-[11px] text-slate-400">
                  Showing {visibleResults.length} of {filteredResults.length} results
                </p>

                {displayMode === 'initial' &&
                  filteredResults.length > INITIAL_VISIBLE_ROWS && (
                    <button
                      onClick={() => setDisplayMode('more')}
                      className="text-xs font-semibold text-cyan-700 transition hover:text-cyan-800"
                    >
                      Show more →
                    </button>
                  )}

                {displayMode === 'more' &&
                  filteredResults.length > MAX_VISIBLE_ROWS && (
                    <button
                      onClick={() => setDisplayMode('all')}
                      className="text-xs font-semibold text-cyan-700 transition hover:text-cyan-800"
                    >
                      Show all →
                    </button>
                  )}
              </div>
            </>
          )}
      </div>
    </section>
  )
}

function ExceptionQueue({
  batches,
  filters,
  onFilterChange,
  onClearFilters,
  exceptions,
  loading,
  error,
  onInvestigate,
}) {
  const [search, setSearch] = useState('')
  const [expandedId, setExpandedId] = useState(null)
  const [displayMode, setDisplayMode] = useState('initial')

  const filteredExceptions = exceptions.filter(
    (exception) =>
      !search ||
      String(exception.paymentReference || '')
        .toLowerCase()
        .includes(search.toLowerCase())
  )

  const visibleExceptions = filteredExceptions.slice(
    0,
    displayMode === 'all'
      ? filteredExceptions.length
      : displayMode === 'more'
        ? MAX_VISIBLE_ROWS
        : INITIAL_VISIBLE_ROWS
  )

  const categories = [
    ...new Set(
      exceptions
        .map((exception) => exception.category)
        .filter(Boolean)
    ),
  ]

  const matchTypes = [
    ...new Set(
      exceptions
        .map((exception) => exception.matchType)
        .filter(Boolean)
    ),
  ]

  return (
    <section>
      <SectionHeading
        eyebrow="Exception management"
        title="Exception queue"
        description="Prioritize discrepancies, inspect evidence, and move each case toward resolution."
        right={
          <div className="text-right">
            <p className="text-2xl font-semibold tracking-tight text-slate-950">
              {loading ? '—' : exceptions.length}
            </p>

            <p className="text-[11px] text-slate-400">
              active cases in view
            </p>
          </div>
        }
      />

      <div className="mb-5 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200/70">
        <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-6">
          <select
            value={filters.batchId}
            onChange={(event) =>
              onFilterChange('batchId', event.target.value)
            }
            className="rounded-xl border-0 bg-slate-50 px-3.5 py-2.5 text-xs font-medium text-slate-700 ring-1 ring-slate-200 focus:outline-none focus:ring-2 focus:ring-cyan-500"
          >
            <option value="">All batches</option>

            {batches.map((batch) => (
              <option
                key={batch.logicalBatchId}
                value={batch.paymentBatchId || batch.logicalBatchId}
              >
                {batch.logicalBatchId}
              </option>
            ))}
          </select>

          <select
            value={filters.matchType}
            onChange={(event) =>
              onFilterChange('matchType', event.target.value)
            }
            className="rounded-xl border-0 bg-slate-50 px-3.5 py-2.5 text-xs font-medium text-slate-700 ring-1 ring-slate-200 focus:outline-none focus:ring-2 focus:ring-cyan-500"
          >
            <option value="">All match types</option>

            {matchTypes.map((matchType) => (
              <option key={matchType} value={matchType}>
                {formatMatchType(matchType)}
              </option>
            ))}
          </select>

          <select
            value={filters.status}
            onChange={(event) =>
              onFilterChange('status', event.target.value)
            }
            className="rounded-xl border-0 bg-slate-50 px-3.5 py-2.5 text-xs font-medium text-slate-700 ring-1 ring-slate-200 focus:outline-none focus:ring-2 focus:ring-cyan-500"
          >
            <option value="">All statuses</option>
            <option value="OPEN">Open</option>
            <option value="INVESTIGATING">Investigating</option>
            <option value="RESOLVED">Resolved</option>
          </select>

          <select
            value={filters.severity}
            onChange={(event) =>
              onFilterChange('severity', event.target.value)
            }
            className="rounded-xl border-0 bg-slate-50 px-3.5 py-2.5 text-xs font-medium text-slate-700 ring-1 ring-slate-200 focus:outline-none focus:ring-2 focus:ring-cyan-500"
          >
            <option value="">All severities</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>

          <select
            value={filters.category}
            onChange={(event) =>
              onFilterChange('category', event.target.value)
            }
            className="rounded-xl border-0 bg-slate-50 px-3.5 py-2.5 text-xs font-medium text-slate-700 ring-1 ring-slate-200 focus:outline-none focus:ring-2 focus:ring-cyan-500"
          >
            <option value="">All categories</option>

            {categories.map((category) => (
              <option key={category} value={category}>
                {formatExceptionCategory(category)}
              </option>
            ))}
          </select>

          <button
            onClick={onClearFilters}
            className="rounded-xl bg-slate-900 px-3.5 py-2.5 text-xs font-semibold text-white transition hover:bg-slate-800"
          >
            Clear filters
          </button>
        </div>

        <div className="mt-3">
          <div className="relative">
            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
              ⌕
            </span>

            <input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
                setDisplayMode('initial')
              }}
              placeholder="Search by payment reference..."
              className="w-full rounded-xl border-0 bg-slate-50 py-2.5 pl-9 pr-4 text-xs text-slate-700 ring-1 ring-slate-200 transition placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-cyan-500"
            />
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/70">
        {loading && (
          <div className="px-6 py-16 text-center text-sm text-slate-500">
            Loading exceptions...
          </div>
        )}

        {!loading && error && (
          <div className="m-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}

        {!loading && !error && exceptions.length === 0 && (
          <EmptyState
            title="No exceptions found"
            description="No exceptions match the selected filters."
          />
        )}

        {!loading && !error && exceptions.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-sm">
                <colgroup>
                  <col className="w-[32%]" />
                  <col className="w-[25%]" />
                  <col className="w-[15%]" />
                  <col className="w-[18%]" />
                  <col className="w-[10%]" />
                </colgroup>

                <thead className="bg-slate-50/80">
                  <tr>
                    {[
                      'Payment',
                      'Category',
                      'Severity',
                      'Status',
                      '',
                    ].map((heading, index) => (
                      <th
                        key={`${heading}-${index}`}
                        className={`px-7 py-3.5 text-left text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400 ${
                          heading === '' ? 'text-right' : ''
                        }`}
                      >
                        {heading}
                      </th>
                    ))}
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-100">
                  {visibleExceptions.map((exception) => (
                    <Fragment key={exception.id}>
                      <tr
                        onClick={() =>
                          setExpandedId(
                            expandedId === exception.id
                              ? null
                              : exception.id
                          )
                        }
                        className="cursor-pointer transition hover:bg-slate-50/70"
                      >
                        <td className="px-7 py-4">
                          <div>
                            <p className="font-semibold text-slate-900">
                              {exception.paymentReference || '—'}
                            </p>

                            <p className="mt-0.5 text-[10px] text-slate-400">
                              #{exception.id}
                            </p>
                          </div>
                        </td>

                        <td className="px-7 py-4">
                          <span className="text-xs font-medium text-slate-600">
                            {formatExceptionCategory(
                              exception.category
                            )}
                          </span>
                        </td>

                        <td className="px-7 py-4">
                          <SeverityBadge
                            severity={exception.severity}
                          />
                        </td>

                        <td className="px-7 py-4">
                          <ExceptionStatusBadge
                            status={exception.status}
                          />
                        </td>

                        <td className="px-7 py-4 text-right">
                          <button
                            onClick={(event) => {
                              event.stopPropagation()
                              onInvestigate(exception)
                            }}
                            className="rounded-lg px-3 py-2 text-xs font-semibold text-slate-600 transition hover:bg-slate-100 hover:text-slate-950"
                          >
                            Investigate →
                          </button>
                        </td>
                      </tr>

                      {expandedId === exception.id && (
                        <tr className="bg-slate-50">
                          <td colSpan="5" className="px-7 py-5">
                            <div className="grid gap-5 sm:grid-cols-4">
                              <div>
                                <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
                                  Category
                                </p>

                                <p className="mt-1 text-sm font-semibold text-slate-800">
                                  {formatExceptionCategory(
                                    exception.category
                                  )}
                                </p>
                              </div>

                              <div>
                                <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
                                  Expected
                                </p>

                                <p className="mt-1 text-sm font-semibold text-slate-800">
                                  {formatCurrency(
                                    exception.expectedAmount
                                  )}
                                </p>
                              </div>

                              <div>
                                <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
                                  Actual
                                </p>

                                <p className="mt-1 text-sm font-semibold text-slate-800">
                                  {formatCurrency(
                                    exception.actualAmount
                                  )}
                                </p>
                              </div>

                              <div>
                                <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
                                  Difference
                                </p>

                                <p className="mt-1 text-sm font-semibold text-slate-800">
                                  {formatCurrency(
                                    exception.difference
                                  )}
                                </p>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3">
              <p className="text-[11px] text-slate-400">
                Showing {visibleExceptions.length} of{' '}
                {filteredExceptions.length} exceptions
              </p>

              {displayMode === 'initial' &&
                filteredExceptions.length > INITIAL_VISIBLE_ROWS && (
                  <button
                    onClick={() => setDisplayMode('more')}
                    className="text-xs font-semibold text-cyan-700 transition hover:text-cyan-800"
                  >
                    Show 20 →
                  </button>
                )}

              {displayMode === 'more' &&
                filteredExceptions.length > MAX_VISIBLE_ROWS && (
                  <button
                    onClick={() => setDisplayMode('all')}
                    className="text-xs font-semibold text-cyan-700 transition hover:text-cyan-800"
                  >
                    Show all →
                  </button>
                )}
            </div>
          </>
        )}
      </div>
    </section>
  )
}

function BatchRuns({ batches, loading, error }) {
  const visibleBatches = batches.slice(0, MAX_RENDERED_ROWS)

  return (
    <section>
      <SectionHeading
        eyebrow="Data operations"
        title="Recent runs"
        description="Monitor ingestion activity and understand what entered the controller."
      />

      <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/70">
        {loading && (
          <div className="px-6 py-16 text-center text-sm text-slate-500">
            Loading ingestion batches...
          </div>
        )}

        {!loading && error && (
          <div className="m-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}

        {!loading && !error && batches.length === 0 && (
          <EmptyState
            title="No ingestion runs"
            description="Upload a CSV to create your first financial data batch."
          />
        )}

        {!loading && !error && batches.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-50/80">
                  <tr>
                    {['Batch', 'Date', 'Time', 'Payment records'].map(
                      (heading) => (
                        <th
                          key={heading}
                          className="px-5 py-3.5 text-left text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400"
                        >
                          {heading}
                        </th>
                      )
                    )}
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-100">
                  {visibleBatches.map((batch) => (
                    <tr
                      key={batch.logicalBatchId}
                      className="transition hover:bg-slate-50/70"
                    >
                      <td className="whitespace-nowrap px-5 py-4">
                        <p className="font-semibold text-slate-900">
                          {batch.logicalBatchId}
                        </p>

                        <p className="mt-0.5 text-[10px] text-slate-400">
                          {batch.fileCount}{' '}
                          {batch.fileCount === 1 ? 'file' : 'files'}
                        </p>
                      </td>

                      <td className="whitespace-nowrap px-5 py-4 text-xs text-slate-500">
                        {batch.startedAt
                          ? formatDateTime(batch.startedAt).split(
                              ','
                            )[0]
                          : '—'}
                      </td>

                      <td className="whitespace-nowrap px-5 py-4 text-xs text-slate-500">
                        {batch.startedAt
                          ? formatDateTime(batch.startedAt).split(
                              ','
                            )[1]?.trim() || '—'
                          : '—'}
                      </td>

                      <td className="whitespace-nowrap px-5 py-4">
                        <span className="text-sm font-semibold text-slate-900">
                          {batch.paymentRecords}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
    </section>
  )
}

function IntroPage({ onStart }) {
  return (
    <main className="relative min-h-screen overflow-hidden bg-[#071a1d] px-6 py-8 text-white sm:px-10 lg:px-16">
      <div className="pointer-events-none absolute -right-40 -top-40 h-[42rem] w-[42rem] rounded-full border border-cyan-300/10" />
      <div className="pointer-events-none absolute -right-20 -top-20 h-[30rem] w-[30rem] rounded-full border border-cyan-300/5" />
      <div className="pointer-events-none absolute -bottom-56 -left-48 h-[38rem] w-[38rem] rounded-full border border-emerald-300/10" />

      <div className="relative mx-auto flex min-h-[calc(100vh-4rem)] max-w-[1400px] flex-col justify-between">
        <header className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-300 text-sm font-black text-[#071a1d]">
              L
            </div>

            <div>
              <p className="text-sm font-semibold tracking-tight">
                Ledgerline
              </p>

              <p className="text-[10px] uppercase tracking-[0.16em] text-slate-500">
                AI finance controller
              </p>
            </div>
          </div>

          <span className="hidden text-[10px] font-semibold uppercase tracking-[0.18em] text-slate-500 sm:block">
            Merchant control room
          </span>
        </header>

        <section className="max-w-5xl py-20 lg:py-28">
          <div className="flex items-center gap-3 text-xs font-semibold uppercase tracking-[0.18em] text-cyan-300">
            <span className="h-1.5 w-1.5 rounded-full bg-cyan-300" />
            Financial intelligence infrastructure
          </div>

          <h1 className="mt-7 max-w-5xl text-5xl font-semibold leading-[0.98] tracking-[-0.04em] sm:text-7xl lg:text-[6.5rem]">
            Make every financial discrepancy
            <span className="text-cyan-300"> understandable.</span>
          </h1>

          <p className="mt-8 max-w-2xl text-base leading-7 text-slate-400 sm:text-lg">
            Reconcile transactions, surface what needs attention,
            and trace every automated decision back to the evidence
            that supports it.
          </p>

          <button
            onClick={onStart}
            className="group mt-10 inline-flex items-center gap-5 rounded-xl bg-cyan-300 px-6 py-3.5 text-sm font-bold text-[#071a1d] transition hover:bg-cyan-200"
          >
            Enter control room

            <span className="transition-transform group-hover:translate-x-1">
              →
            </span>
          </button>
        </section>

        <footer className="flex flex-col gap-3 border-t border-white/10 py-5 text-[10px] uppercase tracking-[0.12em] text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <span>
            Deterministic reconciliation · Evidence · AI assistance · Audit
          </span>

          <span>Built for accountable finance operations</span>
        </footer>
      </div>
    </main>
  )
}

function UploadPanel({ onUploaded }) {
  const [entityType, setEntityType] = useState('PAYMENT')
  const [selectedFile, setSelectedFile] = useState(null)
  const [uploadState, setUploadState] = useState('idle')
  const [message, setMessage] = useState('')

  function chooseFile(file) {
    setMessage('')

    if (!file) return

    if (!file.name.toLowerCase().endsWith('.csv')) {
      setSelectedFile(null)
      setMessage('Please select a CSV file.')
      return
    }

    if (file.size > 25 * 1024 * 1024) {
      setSelectedFile(null)
      setMessage('CSV files must be smaller than 25 MB.')
      return
    }

    setSelectedFile(file)
  }

  async function handleUpload() {
    if (!selectedFile) return

    setUploadState('uploading')
    setMessage('')

    try {
      const result = await uploadCsv(selectedFile, entityType)

      setUploadState('complete')
      setMessage(
        `${result.importedRows} rows imported, ${result.failedRows} rows failed.`
      )

      await onUploaded(result)
    } catch (error) {
      setUploadState('error')
      setMessage(
        error.message || 'Upload failed. Please try again.'
      )
    }
  }

  return (
    <section className="mt-8">
      <div className="overflow-hidden rounded-2xl bg-slate-950 text-white shadow-xl shadow-slate-900/10">
        <div className="grid lg:grid-cols-[0.8fr_1.2fr]">
          <div className="flex flex-col justify-between p-7 sm:p-9">
            <div>
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-300 text-sm font-black text-slate-950">
                ↑
              </div>

              <p className="mt-7 text-[10px] font-bold uppercase tracking-[0.18em] text-cyan-300">
                Start a reconciliation
              </p>

              <h3 className="mt-2 text-2xl font-semibold tracking-tight">
                Bring your financial data in.
              </h3>

              <p className="mt-3 max-w-md text-sm leading-6 text-slate-400">
                Upload a CSV and activate reconciliation,
                exception detection, investigation, and audit
                history.
              </p>
            </div>

            <label className="mt-8 block text-xs font-semibold text-slate-300">
              Data source

              <select
                value={entityType}
                onChange={(event) =>
                  setEntityType(event.target.value)
                }
                className="mt-2 block w-full rounded-xl border-0 bg-white/10 px-3.5 py-3 text-xs font-medium text-white ring-1 ring-white/10 focus:outline-none focus:ring-2 focus:ring-cyan-300"
              >
                <option
                  value="PAYMENT"
                  className="text-slate-900"
                >
                  Payments
                </option>

                <option
                  value="SETTLEMENT"
                  className="text-slate-900"
                >
                  Settlements
                </option>

                <option
                  value="REFUND"
                  className="text-slate-900"
                >
                  Refunds
                </option>

                <option
                  value="ADJUSTMENT"
                  className="text-slate-900"
                >
                  Adjustments
                </option>
              </select>
            </label>
          </div>

          <div className="bg-white p-5 text-slate-900 sm:p-7">
            <label
              className="flex min-h-64 cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 bg-slate-50 px-6 text-center transition hover:border-cyan-400 hover:bg-cyan-50/40"
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => {
                event.preventDefault()
                chooseFile(event.dataTransfer.files[0])
              }}
            >
              <input
                type="file"
                accept=".csv,text/csv"
                className="sr-only"
                onChange={(event) =>
                  chooseFile(event.target.files[0])
                }
              />

              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-cyan-50 text-xl text-cyan-700">
                ↑
              </div>

              <span className="mt-4 text-sm font-semibold text-slate-900">
                Drop a CSV here or browse files
              </span>

              <span className="mt-1 text-xs text-slate-400">
                CSV only · maximum 25 MB
              </span>

              {selectedFile && (
                <span className="mt-5 max-w-full truncate rounded-lg bg-white px-4 py-2.5 text-xs font-semibold text-cyan-800 shadow-sm ring-1 ring-slate-200">
                  {selectedFile.name} ·{' '}
                  {(selectedFile.size / 1024).toFixed(1)} KB
                </span>
              )}
            </label>

            {message && (
              <p
                className={`mt-4 rounded-xl px-4 py-3 text-xs font-medium ring-1 ${
                  uploadState === 'error'
                    ? 'bg-red-50 text-red-700 ring-red-200'
                    : 'bg-emerald-50 text-emerald-700 ring-emerald-200'
                }`}
              >
                {message}
              </p>
            )}

            <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
              <button
                onClick={handleUpload}
                disabled={
                  !selectedFile || uploadState === 'uploading'
                }
                className="rounded-xl bg-slate-950 px-5 py-3 text-xs font-bold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
              >
                {uploadState === 'uploading'
                  ? 'Uploading...'
                  : 'Upload CSV'}
              </button>

              {uploadState === 'complete' && (
                <span className="text-xs font-semibold text-emerald-700">
                  Batch ready in Operations.
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default function App() {
  const [started, setStarted] = useState(false)
  const [activePage, setActivePage] = useState('home')

  const [batches, setBatches] = useState([])
  const [batchLoading, setBatchLoading] = useState(true)
  const [batchError, setBatchError] = useState(null)

  const [metrics, setMetrics] = useState(null)
  const [metricsError, setMetricsError] = useState(null)
  const [metricsLoading, setMetricsLoading] = useState(true)

  const [selectedBatchId, setSelectedBatchId] = useState('')

  const [reconciliationResults, setReconciliationResults] =
    useState([])
  const [reconciliationLoading, setReconciliationLoading] =
    useState(false)
  const [reconciliationError, setReconciliationError] =
    useState(null)

  const [exceptionFilters, setExceptionFilters] = useState({
    batchId: '',
    matchType: '',
    status: '',
    category: '',
    severity: '',
  })

  const [exceptions, setExceptions] = useState([])
  const [exceptionLoading, setExceptionLoading] =
    useState(false)
  const [exceptionError, setExceptionError] =
    useState(null)

  const [selectedException, setSelectedException] =
    useState(null)

  const fetchBatches = async () => {
    setBatchLoading(true)
    setBatchError(null)

    try {
      const data = await getIngestionBatches()

      const logicalBatches = Array.isArray(data)
        ? toLogicalBatches(data)
        : []

      setBatches(logicalBatches)

      setSelectedBatchId((currentBatchId) =>
        currentBatchId ||
        logicalBatches[0]?.paymentBatchId ||
        ''
      )
    } catch (err) {
      setBatchError(err.message)
      setBatches([])
    } finally {
      setBatchLoading(false)
    }
  }

  const fetchMetrics = async () => {
    setMetricsLoading(true)
    setMetricsError(null)

    try {
      const data = await getDashboardMetrics()
      setMetrics(data)
    } catch (err) {
      setMetricsError(err.message)
      setMetrics(null)
    } finally {
      setMetricsLoading(false)
    }
  }

  const fetchReconciliationResults = async (batchId) => {
    if (!batchId) {
      setReconciliationResults([])
      return
    }

    setReconciliationLoading(true)
    setReconciliationError(null)

    try {
      const data = await getReconciliationResults(batchId)

      setReconciliationResults(
        Array.isArray(data) ? data : []
      )
    } catch (err) {
      setReconciliationError(err.message)
      setReconciliationResults([])
    } finally {
      setReconciliationLoading(false)
    }
  }

  const fetchExceptions = async (currentFilters) => {
    setExceptionLoading(true)
    setExceptionError(null)

    try {
      const data = await getReconciliationExceptions(
        currentFilters
      )

      setExceptions(Array.isArray(data) ? data : [])
    } catch (err) {
      setExceptionError(err.message)
      setExceptions([])
    } finally {
      setExceptionLoading(false)
    }
  }

  const handleExceptionFilterChange = (
    filter,
    value
  ) => {
    const nextFilters = {
      ...exceptionFilters,
      [filter]: value,
    }

    setExceptionFilters(nextFilters)
    fetchExceptions(nextFilters)
  }

  const clearExceptionFilters = () => {
    const emptyFilters = {
      batchId: '',
      matchType: '',
      status: '',
      category: '',
      severity: '',
    }

    setExceptionFilters(emptyFilters)
    fetchExceptions(emptyFilters)
  }

  const handleInvestigate = (exception) => {
    setSelectedException(exception)
    setActivePage('investigation')
  }

  const handleUploaded = (result) => {
    if (!result?.batchId) return Promise.resolve()

    if (result.entityType !== 'PAYMENT') {
      if (!selectedBatchId) return fetchBatches()

      return runReconciliation(selectedBatchId).then(() =>
        Promise.all([
          fetchBatches(),
          fetchMetrics(),
          fetchReconciliationResults(selectedBatchId),
          fetchExceptions({
            ...exceptionFilters,
            batchId: selectedBatchId,
          }),
        ])
      )
    }

    setActivePage('reconciliation')

    return runReconciliation(result.batchId).then(
      async () => {
        setSelectedBatchId(result.batchId)

        await Promise.all([
          fetchBatches(),
          fetchMetrics(),
          fetchReconciliationResults(result.batchId),
          fetchExceptions({
            ...exceptionFilters,
            batchId: result.batchId,
          }),
        ])

        setExceptionFilters((currentFilters) => ({
          ...currentFilters,
          batchId: result.batchId,
        }))

        setActivePage('overview')
      }
    )
  }

  useEffect(() => {
    if (!started) return

    fetchBatches()
  }, [started])

  useEffect(() => {
    if (batches.length === 0) {
      setReconciliationResults([])
      return
    }
  }, [batches])

  useEffect(() => {
    if (!started) return
    if (!selectedBatchId) return

    fetchMetrics()
    fetchReconciliationResults(selectedBatchId)
  }, [selectedBatchId, started])

  useEffect(() => {
    if (!started) return
    if (!selectedBatchId) return

    fetchExceptions({
      ...exceptionFilters,
      batchId: selectedBatchId,
    })
  }, [selectedBatchId, started])

  if (!started) {
    return <IntroPage onStart={() => setStarted(true)} />
  }

  const navigation = [
    {
      page: 'home',
      label: 'Overview',
      description: 'Controller pulse',
      icon: '◫',
    },
    {
      page: 'reconciliation',
      label: 'Reconciliation',
      description: 'Match outcomes',
      icon: '⇄',
    },
    {
      page: 'exceptions',
      label: 'Exceptions',
      description: 'Cases requiring attention',
      icon: '!',
    },
    {
      page: 'operations',
      label: 'Data runs',
      description: 'Ingestion activity',
      icon: '↥',
    },
  ]

  return (
    <div className="h-[100dvh] overflow-hidden bg-[#f6f7f9] text-slate-900">
      <div className="mx-auto flex h-full min-h-0 max-w-[1800px] flex-col lg:flex-row">
        <aside className="z-20 flex min-h-0 flex-shrink-0 flex-col bg-[#091b1e] text-white lg:h-[100dvh] lg:w-[260px]">
          <div className="px-5 py-6">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-300 text-sm font-black text-[#091b1e]">
                L
              </div>

              <div>
                <p className="text-sm font-semibold tracking-tight">
                  Ledgerline
                </p>

                <p className="text-[9px] uppercase tracking-[0.16em] text-slate-500">
                  AI finance controller
                </p>
              </div>
            </div>
          </div>

          <div className="px-5">
            <div className="h-px bg-white/10" />
          </div>

          <nav
            className="flex gap-1.5 overflow-x-auto px-3 py-5 lg:block lg:space-y-1"
            aria-label="Main navigation"
          >
            <p className="hidden px-3 pb-2 text-[9px] font-bold uppercase tracking-[0.18em] text-slate-600 lg:block">
              Workspace
            </p>

            {navigation.map(
              ({ page, label, description, icon }) => (
                <button
                  key={page}
                  onClick={() => {
                    setSelectedException(null)
                    setActivePage(page)
                  }}
                  className={`group flex min-w-max items-center gap-3 rounded-xl px-3 py-3 text-left transition lg:w-full ${
                    activePage === page
                      ? 'bg-white/10 text-white'
                      : 'text-slate-400 hover:bg-white/5 hover:text-white'
                  }`}
                >
                  <span
                    className={`flex h-8 w-8 items-center justify-center rounded-lg text-xs font-bold ${
                      activePage === page
                        ? 'bg-cyan-300 text-[#091b1e]'
                        : 'bg-white/5 text-slate-500 group-hover:text-slate-300'
                    }`}
                  >
                    {icon}
                  </span>

                  <span className="text-left">
                    <span className="block text-xs font-semibold">
                      {label}
                    </span>

                    <span
                      className={`hidden text-[10px] lg:block ${
                        activePage === page
                          ? 'text-slate-400'
                          : 'text-slate-600'
                      }`}
                    >
                      {description}
                    </span>
                  </span>
                </button>
              )
            )}
          </nav>

          <div className="mt-auto hidden px-5 pb-6 lg:block">
            <div className="rounded-xl bg-white/5 p-4 ring-1 ring-white/5">
              <div className="flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-emerald-400" />

                <span className="text-[10px] font-semibold text-slate-300">
                  Controller healthy
                </span>
              </div>

              <p className="mt-3 text-[10px] leading-5 text-slate-600">
                Reconciliation engine, evidence layer and AI
                services are available.
              </p>
            </div>

            <p className="mt-5 px-1 text-[9px] leading-4 text-slate-700">
              Built for fast exception triage, evidence review,
              and accountable financial decisions.
            </p>
          </div>
        </aside>

        <main className="min-h-0 min-w-0 flex-1 overflow-y-auto overscroll-contain bg-[#f6f7f9] lg:h-[100dvh]">
          <div className="mx-auto max-w-[1500px] px-5 py-6 sm:px-7 lg:px-10 lg:py-9">
            <header className="mb-9 flex flex-col gap-5 border-b border-slate-200/80 pb-7 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <div className="flex items-center gap-2 text-[10px] font-bold uppercase tracking-[0.16em] text-cyan-700">
                  <span className="h-1.5 w-1.5 rounded-full bg-cyan-600" />
                  Merchant operations
                </div>

                <h1 className="mt-2 text-3xl font-semibold tracking-[-0.03em] text-slate-950 sm:text-4xl">
                  {activePage === 'home' &&
                    'Controller overview'}

                  {activePage === 'reconciliation' &&
                    'Reconciliation'}

                  {activePage === 'exceptions' &&
                    'Exception queue'}

                  {activePage === 'operations' &&
                    'Data runs'}

                  {activePage === 'investigation' &&
                    'Investigation'}
                </h1>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">
                  {activePage === 'home' &&
                    'A concise view of financial records, risk, and automated resolution.'}

                  {activePage === 'reconciliation' &&
                    'Compare payment records against provider outcomes and inspect discrepancies.'}

                  {activePage === 'exceptions' &&
                    'Prioritize discrepancies and move each case toward resolution.'}

                  {activePage === 'operations' &&
                    'Monitor ingestion activity and understand what entered the controller.'}

                  {activePage === 'investigation' &&
                    'Trace one discrepancy from source evidence to an accountable decision.'}
                </p>
              </div>

              <div className="hidden items-center gap-3 sm:flex">
                <div className="text-right">
                  <p className="text-[9px] font-bold uppercase tracking-[0.14em] text-slate-400">
                    System
                  </p>

                  <div className="mt-1 flex items-center justify-end gap-2">
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />

                    <span className="text-xs font-semibold text-slate-700">
                      Operational
                    </span>
                  </div>
                </div>
              </div>
            </header>

            {selectedException ? (
              <InvestigationDetail
                exceptionId={selectedException.id}
                paymentReference={
                  selectedException.paymentReference
                }
                onBack={() => {
                  setSelectedException(null)
                  setActivePage('exceptions')
                }}
              />
            ) : (
              <>
                {activePage === 'home' && (
                  <>
                    <OverviewMetrics
                      metrics={metrics}
                      loading={metricsLoading}
                      error={metricsError}
                      batches={batches}
                      selectedBatchId={selectedBatchId}
                      onBatchChange={setSelectedBatchId}
                    />

                    <UploadPanel
                      onUploaded={handleUploaded}
                    />

                    <div className="mt-10">
                      <BatchRuns
                        batches={batches}
                        loading={batchLoading}
                        error={batchError}
                      />
                    </div>
                  </>
                )}

                {activePage === 'reconciliation' && (
                  <ReconciliationResults
                    batches={batches}
                    selectedBatchId={selectedBatchId}
                    onBatchChange={setSelectedBatchId}
                    results={reconciliationResults}
                    loading={reconciliationLoading}
                    error={reconciliationError}
                  />
                )}

                {activePage === 'exceptions' && (
                  <ExceptionQueue
                    batches={batches}
                    filters={exceptionFilters}
                    onFilterChange={
                      handleExceptionFilterChange
                    }
                    onClearFilters={clearExceptionFilters}
                    exceptions={exceptions}
                    loading={exceptionLoading}
                    error={exceptionError}
                    onInvestigate={handleInvestigate}
                  />
                )}

                {activePage === 'operations' && (
                  <BatchRuns
                    batches={batches}
                    loading={batchLoading}
                    error={batchError}
                  />
                )}
              </>
            )}
          </div>
        </main>
      </div>
    </div>
  )
}