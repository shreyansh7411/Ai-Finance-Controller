import { useState, useEffect } from 'react'
import { getIngestionBatches } from './api/ingestionBatchApi.js'
import InvestigationDetail from './components/InvestigationDetail.jsx'
import { getDashboardMetrics } from './api/dashboardApi.js'
import {
  getReconciliationResults,
  getReconciliationExceptions,
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
  onRefresh,
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

          <button
            onClick={onRefresh}
            className="mt-3 rounded-lg border border-red-300 bg-white px-3 py-2 text-sm font-medium text-red-700 hover:bg-red-50"
          >
            Retry
          </button>
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

        <button
          onClick={onRefresh}
          className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Refresh
        </button>
      </div>

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
  onRefresh,
}) {
  const visibleResults = results.slice(0, MAX_RENDERED_ROWS)

  return (
    <section className="mb-8">
      <div className="mb-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">
            Reconciliation Results
          </h2>

          <p className="text-sm text-gray-500">
            Review reconciliation outcomes for an ingestion batch.
          </p>
        </div>

        <div className="flex gap-2">
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
                key={batch.batchId}
                value={batch.batchId}
              >
                {batch.filename || batch.batchId}
              </option>
            ))}
          </select>

          <button
            onClick={onRefresh}
            disabled={!selectedBatchId || loading}
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Refresh
          </button>
        </div>
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
        results.length === 0 && (
          <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
            No reconciliation results found for this batch.
          </div>
        )}

      {!loading &&
        !error &&
        results.length > 0 && (
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
                    <tr
                      key={result.id}
                      className="hover:bg-gray-50"
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
                  ))}
                </tbody>
              </table>
            </div>

            <div className="border-t border-gray-200 px-4 py-3 text-xs text-gray-500">
              Showing {visibleResults.length} of {results.length}{' '}
              reconciliation result{results.length === 1 ? '' : 's'}.
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
  onRefresh,
  onInvestigate,
}) {
  const visibleExceptions = exceptions.slice(0, MAX_RENDERED_ROWS)

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
                key={batch.batchId}
                value={batch.batchId}
              >
                {batch.filename || batch.batchId}
              </option>
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

          <button
            onClick={onRefresh}
            className="mt-3 rounded-lg border border-red-300 bg-white px-3 py-2 text-sm font-medium text-red-700 hover:bg-red-50"
          >
            Retry
          </button>
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
                    <tr
                      key={exception.id}
                      className="hover:bg-gray-50"
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
                          onClick={() =>
                            onInvestigate(exception)
                          }
                          className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="border-t border-gray-200 px-4 py-3 text-xs text-gray-500">
              Showing {visibleExceptions.length} of {exceptions.length}{' '}
              exception{exceptions.length === 1 ? '' : 's'}.
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
  onRefresh,
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

        <button
          onClick={onRefresh}
          className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Refresh
        </button>
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

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Entity
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Status
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Total
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Imported
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Skipped
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Failed
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Started
                    </th>

                    <th className="px-4 py-3 text-left font-medium text-gray-600">
                      Completed
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-gray-100">
                  {visibleBatches.map((batch) => (
                    <tr
                      key={batch.batchId}
                      className="hover:bg-gray-50"
                    >
                      <td className="whitespace-nowrap px-4 py-3 font-medium text-gray-900">
                        {batch.filename || batch.batchId}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {batch.entityType || '�'}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3">
                        <StatusBadge
                          status={batch.status}
                          type="batch"
                        />
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {batch.totalRows}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {batch.importedRows}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {batch.skippedRows}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {batch.failedRows}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {formatDateTime(batch.startedAt)}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                        {formatDateTime(batch.completedAt)}
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

export default function App() {
  const [health, setHealth] = useState(null)
  const [healthError, setHealthError] = useState(null)
  const [healthLoading, setHealthLoading] = useState(true)

  const [batches, setBatches] = useState([])
  const [batchError, setBatchError] = useState(null)
  const [batchLoading, setBatchLoading] = useState(true)

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

  const fetchHealth = async () => {
    setHealthLoading(true)
    setHealthError(null)

    try {
      const response = await fetch('/actuator/health')

      if (!response.ok) {
        throw new Error(
          `Health check failed: ${response.status}`
        )
      }

      const data = await response.json()
      setHealth(data)
    } catch (err) {
      setHealthError(err.message)
      setHealth(null)
    } finally {
      setHealthLoading(false)
    }
  }

  const fetchBatches = async () => {
    setBatchLoading(true)
    setBatchError(null)

    try {
      const data = await getIngestionBatches()
      setBatches(Array.isArray(data) ? data : [])
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
  }

  useEffect(() => {
    fetchHealth()
    fetchBatches()
    fetchMetrics()

    const interval = setInterval(() => {
      fetchHealth()
      fetchBatches()
      fetchMetrics()
    }, 30000)

    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    if (batches.length === 0) {
      setSelectedBatchId('')
      setReconciliationResults([])
      return
    }

    setSelectedBatchId((currentBatchId) => {
      if (
        currentBatchId &&
        batches.some(
          (batch) => batch.batchId === currentBatchId
        )
      ) {
        return currentBatchId
      }

      return batches[0].batchId
    })
  }, [batches])

  useEffect(() => {
    fetchReconciliationResults(selectedBatchId)
  }, [selectedBatchId])

  useEffect(() => {
    fetchExceptions(exceptionFilters)
  }, [])

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">
            AI Finance Controller
          </h1>

          <p className="mt-1 text-sm text-gray-500">
            Controller operations dashboard
          </p>
        </header>

        {selectedException ? (
          <InvestigationDetail
            exceptionId={selectedException.id}
            paymentReference={selectedException.paymentReference}
            onBack={() => setSelectedException(null)}
          />
        ) : (
          <>
            <OverviewMetrics
              metrics={metrics}
              loading={metricsLoading}
              error={metricsError}
              onRefresh={fetchMetrics}
            />

            <section className="mb-8">
              <div className="mb-4">
                <h2 className="text-xl font-semibold text-gray-900">
                  System Health
                </h2>

                <p className="text-sm text-gray-500">
                  Current backend service availability.
                </p>
              </div>

              <div className="rounded-xl border border-gray-200 bg-white p-5">
                {healthLoading && (
                  <p className="text-sm text-gray-500">
                    Checking system health...
                  </p>
                )}

                {!healthLoading && healthError && (
                  <div>
                    <p className="text-sm text-red-700">
                      {healthError}
                    </p>

                    <button
                      onClick={fetchHealth}
                      className="mt-3 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                    >
                      Retry
                    </button>
                  </div>
                )}

                {!healthLoading &&
                  !healthError &&
                  health && (
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium text-gray-900">
                          Backend
                        </p>

                        <p className="text-sm text-gray-500">
                          Spring Boot application
                        </p>
                      </div>

                      <StatusBadge status={health.status} />
                    </div>
                  )}
              </div>
            </section>

            <ReconciliationResults
              batches={batches}
              selectedBatchId={selectedBatchId}
              onBatchChange={setSelectedBatchId}
              results={reconciliationResults}
              loading={reconciliationLoading}
              error={reconciliationError}
              onRefresh={() =>
                fetchReconciliationResults(selectedBatchId)
              }
            />

            <ExceptionQueue
              batches={batches}
              filters={exceptionFilters}
              onFilterChange={handleExceptionFilterChange}
              onClearFilters={clearExceptionFilters}
              exceptions={exceptions}
              loading={exceptionLoading}
              error={exceptionError}
              onRefresh={() =>
                fetchExceptions(exceptionFilters)
              }
              onInvestigate={handleInvestigate}
            />

            <BatchRuns
              batches={batches}
              loading={batchLoading}
              error={batchError}
              onRefresh={fetchBatches}
            />
          </>
        )}
      </div>
    </div>
  )
}



