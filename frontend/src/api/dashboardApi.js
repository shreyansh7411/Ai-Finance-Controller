export async function getDashboardMetrics() {
  const response = await fetch('/api/v1/dashboard/metrics')

  if (!response.ok) {
    throw new Error(`Failed to load dashboard metrics: ${response.status}`)
  }

  return response.json()
}
