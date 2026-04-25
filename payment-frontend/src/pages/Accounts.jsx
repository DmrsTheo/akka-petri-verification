import { useState } from 'react'
import { listAccounts } from '../api'
import RouteTag from '../components/RouteTag'

export default function Accounts() {
  const [accounts, setAccounts] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleList = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await listAccounts()
      if (res.ok) {
        setAccounts(res.data.accounts || [])
      } else {
        setError(res.data)
        setAccounts(null)
      }
    } catch (e) {
      setError({ error: e.message })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-xl font-semibold mb-1">Liste des comptes</h1>
      <p className="text-sm text-stone-500 mb-6">Tous les comptes enregistrés dans le système.</p>

      <div className="card">
        <RouteTag method="GET" path="/api/accounts/" />

        <div className="flex justify-end mb-4">
          <button className="btn btn-primary" onClick={handleList} disabled={loading}>
            {loading ? 'Chargement...' : 'Actualiser'}
          </button>
        </div>

        {error && (
          <div className="resp-err mb-4">{JSON.stringify(error, null, 2)}</div>
        )}

        {accounts === null && !error && (
          <p className="text-sm text-stone-400">Cliquez sur Actualiser pour charger les comptes.</p>
        )}

        {accounts && accounts.length === 0 && (
          <p className="text-sm text-stone-400">Aucun compte trouvé.</p>
        )}

        {accounts && accounts.length > 0 && (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-stone-100">
                <th className="text-left pb-2 text-xs text-stone-400 font-medium">ID</th>
                <th className="text-left pb-2 text-xs text-stone-400 font-medium">Titulaire</th>
                <th className="text-right pb-2 text-xs text-stone-400 font-medium">Solde</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((acc) => (
                <tr key={acc.id} className="border-b border-stone-50 hover:bg-stone-50 transition-colors">
                  <td className="py-2.5 font-mono text-xs text-stone-600">{acc.id}</td>
                  <td className="py-2.5 text-stone-700">{acc.ownerName || '—'}</td>
                  <td className="py-2.5 text-right font-medium tabular-nums">
                    {parseFloat(acc.balance).toFixed(2)} €
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
