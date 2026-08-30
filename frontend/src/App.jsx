import { useState, useEffect } from 'react'

const STATUS_COLORS = {
  UP: 'bg-green-100 text-green-800 border-green-300',
  DOWN: 'bg-red-100 text-red-800 border-red-300',
  DEGRADED: 'bg-yellow-100 text-yellow-800 border-yellow-300',
  LOADING: 'bg-gray-100 text-gray-600 border-gray-300',
}

function StatusBadge({ status }) {
  const color = STATUS_COLORS[status] ?? STATUS_COLORS.LOADING
  return (
    <span
      className={`inline-block px-3 py-1 rounded-full border text-sm font-semibold ${color}`}
    >
      {status}
    </span>
  )
}

export default function App() {
  const [health, setHealth] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [lastChecked, setLastChecked] = useState(null)

  const fetchHealth = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch('/api/health')
      const data = await res.json()
      setHealth(data)
      setLastChecked(new Date().toLocaleTimeString())
    } catch (err) {
      setError('Could not reach backend. Is it running on port 8080?')
      setHealth(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchHealth()
    const interval = setInterval(fetchHealth, 30000)
    return () => clearInterval(interval)
  }, [])

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-lg">
        {/* Header */}
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-gray-900">AI Finance Controller</h1>
          <p className="mt-2 text-gray-500">System Health — Phase 01 Foundation</p>
        </div>

        {/* Status card */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          {loading && !health && !error && (
            <p className="text-gray-500 text-center animate-pulse">Checking backend…</p>
          )}

          {error && (
            <div className="rounded-lg bg-red-50 border border-red-200 p-4">
              <p className="text-red-700 font-medium">Backend Unreachable</p>
              <p className="text-red-600 text-sm mt-1">{error}</p>
            </div>
          )}

          {health && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-gray-600 font-medium">Backend</span>
                <StatusBadge status={health.status} />
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600 font-medium">Database</span>
                <StatusBadge status={health.database} />
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600 font-medium">Service</span>
                <span className="text-gray-700 text-sm">{health.service}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600 font-medium">Version</span>
                <span className="text-gray-700 text-sm">{health.version}</span>
              </div>
              {health.timestamp && (
                <div className="pt-2 border-t border-gray-100">
                  <p className="text-xs text-gray-400">
                    Server time: {health.timestamp}
                  </p>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="mt-4 flex items-center justify-between text-sm text-gray-400">
          <span>{lastChecked ? `Last checked: ${lastChecked}` : ''}</span>
          <button
            onClick={fetchHealth}
            disabled={loading}
            className="text-blue-500 hover:text-blue-700 disabled:opacity-40 transition-colors"
          >
            {loading ? 'Checking…' : 'Refresh'}
          </button>
        </div>
      </div>
    </div>
  )
}
