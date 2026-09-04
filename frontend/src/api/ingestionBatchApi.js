export async function getIngestionBatches() {
  const response = await fetch('/api/v1/ingestion/batches')

  if (!response.ok) {
    throw new Error(`Failed to load ingestion batches: ${response.status}`)
  }

  return response.json()
}

export async function getIngestionBatch(batchId) {
  const response = await fetch(
    `/api/v1/ingestion/batches/${encodeURIComponent(batchId)}`
  )

  if (!response.ok) {
    if (response.status === 404) {
      return null
    }

    throw new Error(`Failed to load ingestion batch: ${response.status}`)
  }

  return response.json()
}

export async function uploadCsv(file, entityType) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await fetch(
    `/api/v1/ingestion/${entityType.toLowerCase()}s`,
    {
      method: 'POST',
      body: formData,
    }
  )

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Upload failed: ${response.status}`)
  }

  return response.json()
}
