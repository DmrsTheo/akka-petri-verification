import { useState } from 'react'
import { checkHealth } from '../api'
import ResponseBox from '../components/ResponseBox'
import RouteTag from '../components/RouteTag'

export default function Health() {
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleCheck = async () => {
    setLoading(true)
    try {
      const res = await checkHealth()
      setResult(res)
    } catch (e) {
      setResult({ ok: false, data: { error: e.message } })
    } finally {
      setLoading(false)
    }
  }

  // Détermine la couleur du badge de statut
  const statusColor = result === null
    ? 'bg-stone-200 text-stone-500'
    : result.ok
      ? 'bg-emerald-100 text-emerald-700'
      : 'bg-red-100 text-red-700'

  const statusLabel = result === null
    ? 'Non vérifié'
    : result.ok ? 'Opérationnelle' : 'Injoignable'

  return (
    <div>
      <h1 className="text-xl font-semibold mb-1">Santé de l'API</h1>
      <p className="text-sm text-stone-500 mb-6">Vérifie que le serveur Akka répond correctement.</p>

      <div className="card">
        <RouteTag method="GET" path="/api/health" />

        <div className="flex items-center justify-between mb-5">
          <span className={`text-xs font-medium px-3 py-1 rounded-full ${statusColor}`}>
            {statusLabel}
          </span>
          <button
            className="btn btn-primary"
            onClick={handleCheck}
            disabled={loading}
          >
            {loading ? 'Vérification...' : 'Vérifier'}
          </button>
        </div>

        <ResponseBox result={result} />
      </div>
    </div>
  )
}
