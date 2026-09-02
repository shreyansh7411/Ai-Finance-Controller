export async function getException(exceptionId) {
  const response = await fetch(
    `/api/v1/reconciliation/exceptions/${encodeURIComponent(exceptionId)}`
  )

  if (!response.ok) {
    throw new Error(`Failed to load exception: ${response.status}`)
  }

  return response.json()
}

export async function investigateException(exceptionId) {
  const response = await fetch(
    `/api/v1/reconciliation/exceptions/${encodeURIComponent(exceptionId)}/investigate`,
    {
      method: 'POST',
    }
  )

  if (!response.ok) {
    throw new Error(`Failed to investigate exception: ${response.status}`)
  }

  return response.json()
}

export async function getExceptionDecision(exceptionId) {
  const response = await fetch(
    `/api/v1/reconciliation/exceptions/${encodeURIComponent(exceptionId)}/decision`
  )

  if (response.status === 404) {
    return null
  }

  if (!response.ok) {
    throw new Error(`Failed to load decision: ${response.status}`)
  }

  return response.json()
}

export async function getExceptionAudit(exceptionId) {
  const response = await fetch(
    `/api/v1/reconciliation/exceptions/${encodeURIComponent(exceptionId)}/audit`
  )

  if (!response.ok) {
    throw new Error(`Failed to load audit history: ${response.status}`)
  }

  return response.json()
}

export async function updateExceptionStatus(exceptionId, status, actor = 'MERCHANT_UI', decision = '', resolution = '') {
  const response = await fetch(
    `/api/v1/reconciliation/exceptions/${encodeURIComponent(exceptionId)}/status`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        status,
        actor,
        decision,
        resolution,
      }),
    }
  )

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Failed to update exception status: ${response.status}`)
  }

  return response.json()
}
