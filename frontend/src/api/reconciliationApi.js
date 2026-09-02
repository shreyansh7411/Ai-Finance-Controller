export async function getReconciliationResults(batchId) {
  const response = await fetch(
    `/api/v1/reconciliation/results?batchId=${encodeURIComponent(batchId)}`
  )

  if (!response.ok) {
    throw new Error(
      `Failed to load reconciliation results: ${response.status}`
    )
  }

  return response.json()
}

export async function getReconciliationExceptions(filters = {}) {
  const params = new URLSearchParams()

  if (filters.batchId) {
    params.set('batchId', filters.batchId)
  }

  if (filters.matchType) {
    params.set('matchType', filters.matchType)
  }

  if (filters.status) {
    params.set('status', filters.status)
  }

  if (filters.category) {
    params.set('category', filters.category)
  }

  if (filters.severity) {
    params.set('severity', filters.severity)
  }

  const queryString = params.toString()

  const response = await fetch(
    `/api/v1/reconciliation/exceptions${
      queryString ? `?${queryString}` : ''
    }`
  )

  if (!response.ok) {
    throw new Error(
      `Failed to load reconciliation exceptions: ${response.status}`
    )
  }

  return response.json()
}
