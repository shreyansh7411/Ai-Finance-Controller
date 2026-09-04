import { useState, useEffect } from 'react'
import { getIngestionBatches, uploadCsv } from './api/ingestionBatchApi.js'
import InvestigationDetail from './components/InvestigationDetail.jsx'
import { getDashboardMetrics } from './api/dashboardApi.js'
import {
  getReconciliationResults,
  getReconciliationExceptions,
  runReconciliation,
} from './api/reconciliationApi.js'

const HEALTH_STATUS_COLORS = {
  UP: 'bg-green-100 text-green-700',
  DOWN: 'bg-red-100 text-red-700',
}

const BATCH_STATUS_COLORS = {
  PROCESSING: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  COMPLETED_WITH_ERRORS: 'bg-yellow-100 text-yellow-700',
  FAILED: 'bg-red-100 text-red-700',
}

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
    (first, second) => new Date(second.startedAt || 0) - new Date(first.startedAt || 0)
  )
}

function StatusBadge({ status, type = 'health' }) {
  const colors =
    type === 'batch'
      ? BATCH_STATUS_COLORS
      : HEALTH_STATUS_COLORS

  const colorClass =
    colors[status] || 'bg-gray-100 text-gray-700'

  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${colorClass}`}
    >
      {status || 'UNKNOWN'}
    </span>
  )
}

function ExceptionStatusBadge({ status }) {
  const colors = {
    OPEN: 'bg-red-100 text-red-700',
    INVESTIGATING: 'bg-blue-100 text-blue-700',
    RESOLVED: 'bg-green-100 text-green-700',
  }

  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${
        colors[status] || 'bg-gray-100 text-gray-700'
      }`}
    >
      {status || 'UNKNOWN'}
    </span>
  )
}

function SeverityBadge({ severity }) {
  const colors = {
    HIGH: 'bg-red-100 text-red-700',
    MEDIUM: 'bg-yellow-100 text-yellow-700',
    LOW: 'bg-green-100 text-green-700',
  }

  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${
        colors[severity] || 'bg-gray-100 text-gray-700'
      }`}
    >
      {severity || 'UNKNOWN'}
    </span>
  )
}

function formatDateTime(value) {
  if (!value) return '�'

  return new Date(value).toLocaleString()
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
      <section className="mb-8">
        <h2 className="mb-4 text-xl font-semibold text-gray-900">
          Overview
        </h2>

        <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
          Loading dashboard metrics...
        </div>
      </section>
    )
  }

  if (error) {
    return (
      <section className="mb-8">
        <div className="rounded-xl border border-red-200 bg-red-50 p-6">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      </section>
    )
  }

  if (!metrics) {
    return null
  }

  const cards = [
    {
      label: 'Total Records',
      value: metrics.totalRecords,
    },
    {
      label: 'Matched',
      value: metrics.matchedRecords,
    },
    {
      label: 'Exceptions',
      value: metrics.exceptionRecords,
    },
    {
      label: 'AI Resolved',
      value: metrics.aiResolvedRecords,
    },
    {
      label: 'Unresolved',
      value: metrics.unresolvedRecords,
    },
  ]

  return (
    <section className="mb-8">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">
            Overview
          </h2>

          <p className="text-sm text-gray-500">
            Current reconciliation and exception metrics.
          </p>
        </div>

      </div>

      <select value={selectedBatchId} onChange={(event) => onBatchChange(event.target.value)} className="mb-4 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm">
        <option value="">Select batch</option>
        {batches.map((batch) => <option key={batch.logicalBatchId} value={batch.paymentBatchId || ''}>{batch.logicalBatchId} · {batch.paymentRecords} payments</option>)}
      </select>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {cards.map((card) => (
          <div
            key={card.label}
            className="rounded-xl border border-gray-200 bg-white p-5"
          >
            <p className="text-sm text-gray-500">
              {card.label}
            </p>

            <p className="mt-2 text-2xl font-semibold text-gray-900">
              {card.value}
            </p>
          </div>
        ))}
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-xl border border-gray-200 bg-white p-5">
          <p className="text-sm text-gray-500">
            Match Rate
          </p>

          <p className="mt-2 text-2xl font-semibold text-gray-900">
            {metrics.matchRate}%
          </p>

          <p className="mt-1 text-xs text-gray-500">
            Matched records / total records
          </p>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-5">
          <p className="text-sm text-gray-500">
            Resolution Rate
          </p>

          <p className="mt-2 text-2xl font-semibold text-gray-900">
            {metrics.resolutionRate}%
          </p>

          <p className="mt-1 text-xs text-gray-500">
            AI-resolved records / exception records
          </p>
        </div>
      </div>

      <div className="mt-4 rounded-xl border border-gray-200 bg-white p-5">
        <h3 className="font-medium text-gray-900">
          Exception Breakdown
        </h3>

        {Object.keys(metrics.exceptionBreakdown || {}).length === 0 ? (
          <p className="mt-3 text-sm text-gray-500">
            No exceptions found.
          </p>
        ) : (
          <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {Object.entries(metrics.exceptionBreakdown).map(
              ([category, count]) => (
                <div
                  key={category}
                  className="rounded-lg bg-gray-50 p-4"
                >
                  <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
                    {category}
                  </p>

                  <p className="mt-1 text-lg font-semibold text-gray-900">
                    {count}
                  </p>
                </div>
              )
            )}
          </div>
        )}
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
  const [showMore, setShowMore] = useState(false)

  const filteredResults = results
    .filter((result) => result.status === activeTab)
    .filter((result) =>
      !search || String(result.paymentReference || '')
        .toLowerCase().includes(search.toLowerCase())
    )
  const visibleResults = filteredResults.slice(
    0,
    showMore ? MAX_VISIBLE_ROWS : INITIAL_VISIBLE_ROWS
  )

  return (
    <section className="mb-8">
      <div className="mb-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Reconciliation Review</h2>

          <p className="text-sm text-gray-500">
            Review reconciliation outcomes for an ingestion batch.
          </p>
        </div>

        <div className="mt-4">
          <select
            value={selectedBatchId}
            onChange={(event) =>
              onBatchChange(event.target.value)
            }
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
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
        </div>
      </div>

      <div className="mb-4 flex flex-col gap-3 rounded-xl border border-gray-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
        <div className="flex rounded-lg bg-gray-100 p-1">
          {['MATCHED', 'EXCEPTION'].map((tab) => (
            <button key={tab} onClick={() => { setActiveTab(tab); setShowMore(false); setExpandedId(null) }} className={`rounded-md px-4 py-2 text-sm font-semibold ${activeTab === tab ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'}`}>
              {tab === 'MATCHED' ? 'Matched' : 'Exceptions'}
              <span className="ml-2 text-xs text-gray-400">{results.filter((result) => result.status === tab).length}</span>
            </button>
          ))}
        </div>
        <input value={search} onChange={(event) => { setSearch(event.target.value); setShowMore(false) }} placeholder="Search payment reference" className="rounded-lg border border-gray-300 px-3 py-2 text-sm sm:w-64" />
      </div>

      {loading && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
          Loading reconciliation results...
        </div>
      )}

      {!loading && error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-6">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {!loading && !error && !selectedBatchId && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
          Select an ingestion batch to view reconciliation results.
        </div>
      )}

      {!loading &&
        !error &&
        selectedBatchId &&
        filteredResults.length === 0 && (
          <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
            No reconciliation results found for this batch.
          </div>
        )}

      {!loading &&
        !error &&
        filteredResults.length > 0 && (
          <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="border-b border-gray-200 bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Payment Reference
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Match Type
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Expected
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Actual
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Difference
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Status
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Confidence
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-gray-100">
                  {visibleResults.map((result) => (
                    <>
                    <tr
                      key={result.id}
                      onClick={() => setExpandedId(expandedId === result.id ? null : result.id)}
                      className="cursor-pointer hover:bg-gray-50"
                    >
                      <td className="whitespace-nowrap px-4 py-3 font-medium text-gray-900">
                        {result.paymentReference || '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {result.matchType || '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {result.expectedAmount ?? '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {result.actualAmount ?? '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {result.difference ?? '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${
                            result.status === 'MATCHED'
                              ? 'bg-green-100 text-green-700'
                              : 'bg-red-100 text-red-700'
                          }`}
                        >
                          {result.status || 'UNKNOWN'}
                        </span>
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {result.confidenceScore != null
                          ? Number(
                              result.confidenceScore
                            ).toFixed(2)
                          : '�'}
                      </td>
                    </tr>
                    {expandedId === result.id && <tr key={`${result.id}-detail`} className="bg-gray-50"><td colSpan="7" className="px-4 py-4"><div className="grid gap-3 text-sm sm:grid-cols-4"><span><b className="text-gray-500">Payment</b><br />{result.paymentReference || '—'}</span><span><b className="text-gray-500">Expected</b><br />{result.expectedAmount ?? '—'}</span><span><b className="text-gray-500">Actual</b><br />{result.actualAmount ?? '—'}</span><span><b className="text-gray-500">Difference</b><br />{result.difference ?? '—'}</span></div></td></tr>}
                    </>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="border-t border-gray-200 px-4 py-3 text-xs text-gray-500">
              Showing {visibleResults.length} of {filteredResults.length} {activeTab.toLowerCase()} result{filteredResults.length === 1 ? '' : 's'}.
              {!showMore && filteredResults.length > INITIAL_VISIBLE_ROWS && <button onClick={() => setShowMore(true)} className="ml-3 font-semibold text-cyan-700">Show more</button>}
            </div>
          </div>
        )}
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
  const [showMore, setShowMore] = useState(false)
  const visibleExceptions = exceptions
    .filter((exception) =>
      !search || String(exception.paymentReference || '')
        .toLowerCase().includes(search.toLowerCase())
    )
    .slice(0, showMore ? MAX_VISIBLE_ROWS : INITIAL_VISIBLE_ROWS)

  const categories = [
    ...new Set(
      exceptions
        .map((exception) => exception.category)
        .filter(Boolean)
    ),
  ]

  return (
    <section className="mb-8">
      <div className="mb-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">
            Exception Queue
          </h2>

          <p className="text-sm text-gray-500">
            Review unresolved reconciliation exceptions.
          </p>
        </div>

        <div className="text-sm text-gray-500">
          {loading
            ? 'Loading...'
            : `${exceptions.length} exception${
                exceptions.length === 1 ? '' : 's'
              }`}
        </div>
      </div>

      <div className="mb-4 rounded-xl border border-gray-200 bg-white p-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <select
            value={filters.batchId}
            onChange={(event) =>
              onFilterChange('batchId', event.target.value)
            }
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
          >
            <option value="">All Batches</option>

            {batches.map((batch) => (
              <option
                key={batch.logicalBatchId}
                value={batch.paymentBatchId || batch.logicalBatchId}
              >
                {batch.logicalBatchId} · {batch.paymentRecords} payments
              </option>
            ))}
          </select>

          <select
            value={filters.matchType}
            onChange={(event) => onFilterChange('matchType', event.target.value)}
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
          >
            <option value="">All Match Types</option>
            {[...new Set(exceptions.map((exception) => exception.matchType).filter(Boolean))].map((matchType) => (
              <option key={matchType} value={matchType}>{matchType}</option>
            ))}
          </select>

          <select
            value={filters.status}
            onChange={(event) =>
              onFilterChange('status', event.target.value)
            }
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
          >
            <option value="">All Statuses</option>
            <option value="OPEN">OPEN</option>
            <option value="INVESTIGATING">
              INVESTIGATING
            </option>
            <option value="RESOLVED">RESOLVED</option>
          </select>

          <select
            value={filters.severity}
            onChange={(event) =>
              onFilterChange('severity', event.target.value)
            }
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
          >
            <option value="">All Severities</option>
            <option value="HIGH">HIGH</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="LOW">LOW</option>
          </select>

          <select
            value={filters.category}
            onChange={(event) =>
              onFilterChange('category', event.target.value)
            }
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
          >
            <option value="">All Categories</option>

            {categories.map((category) => (
              <option
                key={category}
                value={category}
              >
                {category}
              </option>
            ))}
          </select>

          <button
            onClick={onClearFilters}
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Clear Filters
          </button>
          <input value={search} onChange={(event) => { setSearch(event.target.value); setShowMore(false) }} placeholder="Search payment reference" className="rounded-lg border border-gray-300 px-3 py-2 text-sm" />
        </div>
      </div>

      {loading && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
          Loading exceptions...
        </div>
      )}

      {!loading && error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-6">
          <p className="text-sm text-red-700">{error}</p>

        </div>
      )}

      {!loading &&
        !error &&
        exceptions.length === 0 && (
          <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
            No exceptions match the selected filters.
          </div>
        )}

      {!loading &&
        !error &&
        exceptions.length > 0 && (
          <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="border-b border-gray-200 bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Payment Reference
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Category
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Severity
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Expected
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Actual
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Difference
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Confidence
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Status
                    </th>

                    <th className="px-4 py-3 text-right font-medium text-gray-600">
                      Action
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-gray-100">
                  {visibleExceptions.map((exception) => (
                    <>
                    <tr
                      key={exception.id}
                      onClick={() => setExpandedId(expandedId === exception.id ? null : exception.id)}
                      className="cursor-pointer hover:bg-gray-50"
                    >
                      <td className="whitespace-nowrap px-4 py-3 font-medium text-gray-900">
                        {exception.paymentReference || '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {exception.category || '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3">
                        <SeverityBadge
                          severity={exception.severity}
                        />
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {exception.expectedAmount ?? '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {exception.actualAmount ?? '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 font-medium text-gray-700">
                        {exception.difference ?? '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {exception.confidenceScore != null
                          ? Number(
                              exception.confidenceScore
                            ).toFixed(2)
                          : '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3">
                        <ExceptionStatusBadge
                          status={exception.status}
                        />
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-right">
                        <button
                          onClick={(event) => { event.stopPropagation(); onInvestigate(exception) }}
                          className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
                        >
                          View
                        </button>
                      </td>
                    </tr>
                    {expandedId === exception.id && <tr key={`${exception.id}-detail`} className="bg-gray-50"><td colSpan="9" className="px-4 py-4"><div className="grid gap-3 text-sm sm:grid-cols-4"><span><b className="text-gray-500">Category</b><br />{exception.category || '—'}</span><span><b className="text-gray-500">Expected</b><br />{exception.expectedAmount ?? '—'}</span><span><b className="text-gray-500">Actual</b><br />{exception.actualAmount ?? '—'}</span><span><b className="text-gray-500">Difference</b><br />{exception.difference ?? '—'}</span></div></td></tr>}
                    </>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="border-t border-gray-200 px-4 py-3 text-xs text-gray-500">
              Showing {visibleExceptions.length} of {exceptions.length}{' '}
              exception{exceptions.length === 1 ? '' : 's'}.
              {!showMore && exceptions.length > INITIAL_VISIBLE_ROWS && <button onClick={() => setShowMore(true)} className="ml-3 font-semibold text-cyan-700">Show more</button>}
            </div>
          </div>
        )}
    </section>
  )
}

function BatchRuns({
  batches,
  loading,
  error,
}) {
  const visibleBatches = batches.slice(0, MAX_RENDERED_ROWS)

  return (
    <section className="mb-8">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">
            Ingestion Runs
          </h2>

          <p className="text-sm text-gray-500">
            Recent financial data ingestion activity.
          </p>
        </div>

      </div>

      {loading && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
          Loading ingestion batches...
        </div>
      )}

      {!loading && error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-6">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {!loading && !error && batches.length === 0 && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
          No ingestion batches found.
        </div>
      )}

      {!loading &&
        !error &&
        batches.length > 0 && (
          <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="border-b border-gray-200 bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Batch
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">Date</th>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">Time</th>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">Payment records</th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-gray-100">
                  {visibleBatches.map((batch) => (
                    <tr
                      key={batch.logicalBatchId}
                      className="hover:bg-gray-50"
                    >
                      <td className="whitespace-nowrap px-4 py-3 font-medium text-gray-900">
                        {batch.logicalBatchId}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {new Date(batch.startedAt).toLocaleDateString()}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {new Date(batch.startedAt).toLocaleTimeString()}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 font-semibold text-slate-900">
                        {batch.paymentRecords}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
    </section>
  )
}

function IntroPage({ onStart }) {
  return (
    <main className="relative min-h-screen overflow-hidden bg-[#102a2e] px-6 py-8 text-white sm:px-10 lg:px-16">
      <div className="pointer-events-none absolute right-[-8rem] top-[-10rem] h-[34rem] w-[34rem] rounded-full border-[3rem] border-cyan-300/10" />
      <div className="pointer-events-none absolute bottom-[-12rem] left-[-8rem] h-[30rem] w-[30rem] rounded-full border-[2rem] border-emerald-300/10" />
      <div className="relative mx-auto flex min-h-[calc(100vh-4rem)] max-w-7xl flex-col justify-between">
        <header className="flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-cyan-300">AI Finance Controller</p>
            <p className="mt-2 text-sm text-slate-300">Financial reconciliation & exception resolution</p>
          </div>
          <span className="hidden text-xs font-medium uppercase tracking-[0.18em] text-slate-400 sm:block">Merchant control room</span>
        </header>
        <section className="max-w-5xl py-16 lg:py-24">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">From payment data to confident action</p>
          <h1 className="mt-6 max-w-4xl text-5xl font-semibold leading-[0.98] tracking-tight sm:text-7xl lg:text-8xl">Make every financial discrepancy understandable.</h1>
          <p className="mt-8 max-w-2xl text-lg leading-8 text-slate-300 sm:text-xl">Reconcile transactions, surface what needs attention, and trace every decision back to evidence.</p>
          <button onClick={onStart} className="mt-10 rounded-lg bg-cyan-300 px-7 py-4 text-sm font-semibold text-[#102a2e] transition hover:bg-cyan-200">Get started <span className="ml-3" aria-hidden="true">→</span></button>
        </section>
        <footer className="flex flex-col gap-3 border-t border-white/10 py-5 text-xs text-slate-400 sm:flex-row sm:items-center sm:justify-between"><span>Deterministic reconciliation · Evidence · AI assistance · Audit</span><span>Built for accountable finance operations</span></footer>
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
      setMessage(`${result.importedRows} rows imported, ${result.failedRows} rows failed.`)
      await onUploaded(result)
    } catch (error) {
      setUploadState('error')
      setMessage(error.message || 'Upload failed. Please try again.')
    }
  }

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
        <div><p className="text-sm font-semibold uppercase tracking-[0.16em] text-cyan-700">Start here</p><h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Upload financial data</h3><p className="mt-2 max-w-xl text-sm leading-6 text-slate-500">Add a CSV to activate reconciliation, exceptions, investigations, and audit history.</p></div>
        <label className="text-sm font-medium text-slate-700">File type<select value={entityType} onChange={(event) => setEntityType(event.target.value)} className="mt-2 block rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"><option value="PAYMENT">Payments</option><option value="SETTLEMENT">Settlements</option><option value="REFUND">Refunds</option><option value="ADJUSTMENT">Adjustments</option></select></label>
      </div>
      <label className="mt-6 flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-cyan-200 bg-cyan-50/60 px-6 py-12 text-center transition hover:border-cyan-400 hover:bg-cyan-50" onDragOver={(event) => event.preventDefault()} onDrop={(event) => { event.preventDefault(); chooseFile(event.dataTransfer.files[0]) }}><input type="file" accept=".csv,text/csv" className="sr-only" onChange={(event) => chooseFile(event.target.files[0])} /><span className="text-4xl font-light text-cyan-700" aria-hidden="true">↑</span><span className="mt-3 text-sm font-semibold text-slate-900">Drop a CSV here or browse files</span><span className="mt-1 text-xs text-slate-500">CSV only · maximum 25 MB</span>{selectedFile && <span className="mt-4 rounded-md bg-white px-3 py-2 text-sm font-medium text-cyan-800 shadow-sm">{selectedFile.name} · {(selectedFile.size / 1024).toFixed(1)} KB</span>}</label>
      {message && <p className={`mt-4 rounded-lg border px-4 py-3 text-sm ${uploadState === 'error' ? 'border-red-200 bg-red-50 text-red-700' : 'border-emerald-200 bg-emerald-50 text-emerald-800'}`}>{message}</p>}
      <div className="mt-5 flex items-center gap-4"><button onClick={handleUpload} disabled={!selectedFile || uploadState === 'uploading'} className="rounded-lg bg-[#102a2e] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#173d42] disabled:cursor-not-allowed disabled:opacity-40">{uploadState === 'uploading' ? 'Uploading...' : 'Upload CSV'}</button>{uploadState === 'complete' && <span className="text-sm font-medium text-emerald-700">Batch is ready in Operations.</span>}</div>
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
        currentBatchId || logicalBatches[0]?.paymentBatchId || ''
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

  const [selectedException, setSelectedException] = useState(null)

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
          fetchExceptions({ ...exceptionFilters, batchId: selectedBatchId }),
        ])
      )
    }

    setActivePage('reconciliation')

    return runReconciliation(result.batchId).then(async () => {
      setSelectedBatchId(result.batchId)
      await Promise.all([
        fetchBatches(),
        fetchMetrics(),
        fetchReconciliationResults(result.batchId),
        fetchExceptions({ ...exceptionFilters, batchId: result.batchId }),
      ])
      setExceptionFilters((currentFilters) => ({
        ...currentFilters,
        batchId: result.batchId,
      }))
      setActivePage('overview')
    })
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
    fetchExceptions({ ...exceptionFilters, batchId: selectedBatchId })
  }, [selectedBatchId, started])

  if (!started) {
    return (
      <IntroPage onStart={() => setStarted(true)} />
    )
  }

  return (
    <div className="h-[100dvh] overflow-hidden bg-[#f4f7f8] text-slate-900">
      <div className="mx-auto flex h-full min-h-0 max-w-[1600px] flex-col lg:flex-row">
        <aside className="min-h-0 border-b border-slate-200 bg-[#102a2e] text-white lg:h-[100dvh] lg:w-72 lg:flex-shrink-0 lg:overflow-y-auto lg:border-b-0 lg:border-r lg:border-slate-800">
          <div className="flex items-center justify-between px-6 py-6 lg:block">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-cyan-300">
                Finance control
              </p>
              <h1 className="mt-2 text-2xl font-semibold tracking-tight">
                Ledgerline
              </h1>
              <p className="mt-1 text-sm text-slate-300">
                AI reconciliation workspace
              </p>
            </div>
          </div>

          <nav className="flex gap-2 overflow-x-auto px-4 pb-4 lg:mt-8 lg:block lg:space-y-2 lg:px-4" aria-label="Main navigation">
            {[
              ['home', 'Home', 'Upload financial data'],
              ['reconciliation', 'Reconciliation', 'Review match outcomes'],
              ['exceptions', 'Exceptions', 'Investigate discrepancies'],
              ['operations', 'Inventory', 'Track ingested data'],
            ].map(([page, label, description]) => (
              <button
                key={page}
                onClick={() => {
                  setSelectedException(null)
                  setActivePage(page)
                }}
                className={`min-w-max rounded-lg px-4 py-3 text-left transition lg:block lg:w-full ${
                  activePage === page
                    ? 'bg-cyan-300 text-[#102a2e] shadow-lg shadow-cyan-950/20'
                    : 'text-slate-300 hover:bg-white/10 hover:text-white'
                }`}
              >
                <span className="block text-sm font-semibold">{label}</span>
                <span className={`hidden text-xs lg:block ${activePage === page ? 'text-[#31555a]' : 'text-slate-400'}`}>
                  {description}
                </span>
              </button>
            ))}
          </nav>

          <div className="hidden px-6 pb-6 lg:block lg:pt-20">
            <p className="text-xs leading-5 text-slate-400">
              Built for fast exception triage, evidence review, and accountable decisions.
            </p>
          </div>
        </aside>

        <main className="min-h-0 min-w-0 flex-1 overflow-y-auto overscroll-contain bg-[#f4f7f8] px-4 py-6 sm:px-6 lg:h-[100dvh] lg:px-10 lg:py-10">
          <header className="mb-8 flex flex-col gap-4 border-b border-slate-200 pb-6 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-sm font-medium text-cyan-700">Merchant operations</p>
              <h2 className="mt-1 text-3xl font-semibold tracking-tight text-slate-950">
                {activePage === 'home' && 'Upload and activate your workspace.'}
                {activePage === 'overview' && 'Good morning, here is the pulse.'}
                {activePage === 'reconciliation' && 'Settlement review'}
                {activePage === 'exceptions' && 'Exception queue'}
                {activePage === 'operations' && 'Data inventory'}
                {activePage === 'investigation' && 'Investigation detail'}
              </h2>
              <p className="mt-2 max-w-2xl text-sm text-slate-500">
                {activePage === 'home' && 'Start with a CSV. Your operational views will activate when data is available.'}
                {activePage === 'overview' && 'A concise view of financial records, risk, and the latest controller activity.'}
                {activePage === 'reconciliation' && 'Compare matched and exception outcomes across each payment batch.'}
                {activePage === 'exceptions' && 'Prioritize discrepancies by severity and move each case toward resolution.'}
                {activePage === 'operations' && 'Monitor ingestion health and understand what entered the controller.'}
                {activePage === 'investigation' && 'Trace one discrepancy from source evidence to an accountable decision.'}
              </p>
            </div>
          </header>

          {selectedException ? (
            <InvestigationDetail
              exceptionId={selectedException.id}
              paymentReference={selectedException.paymentReference}
              onBack={() => {
                setSelectedException(null)
                setActivePage('exceptions')
              }}
            />
          ) : (
            <>
              {activePage === 'home' && (
                <>
                  <OverviewMetrics metrics={metrics} loading={metricsLoading} error={metricsError} batches={batches} selectedBatchId={selectedBatchId} onBatchChange={setSelectedBatchId} />
                  <UploadPanel onUploaded={handleUploaded} />
                  <section className="mt-8">
                    <div className="mb-4">
                      <p className="text-sm font-medium text-cyan-700">Recent activity</p>
                      <h3 className="mt-1 text-xl font-semibold text-slate-950">Recent batches</h3>
                    </div>
                    <BatchRuns batches={batches} loading={batchLoading} error={batchError} />
                  </section>
                </>
              )}
              {activePage === 'reconciliation' && (
                <ReconciliationResults batches={batches} selectedBatchId={selectedBatchId} onBatchChange={setSelectedBatchId} results={reconciliationResults} loading={reconciliationLoading} error={reconciliationError} />
              )}
              {activePage === 'exceptions' && (
                <ExceptionQueue batches={batches} filters={exceptionFilters} onFilterChange={handleExceptionFilterChange} onClearFilters={clearExceptionFilters} exceptions={exceptions} loading={exceptionLoading} error={exceptionError} onInvestigate={handleInvestigate} />
              )}
              {activePage === 'operations' && (
                <BatchRuns batches={batches} loading={batchLoading} error={batchError} />
              )}
            </>
          )}
        </main>
      </div>
    </div>
  )
}



