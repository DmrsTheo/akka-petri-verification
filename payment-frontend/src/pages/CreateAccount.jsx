import { useState } from 'react'
import { createAccount } from '../api'
import ResponseBox from '../components/ResponseBox'
import RouteTag from '../components/RouteTag'

export default function CreateAccount() {
  const [form, setForm] = useState({ id: '', ownerName: '', initialBalance: '' })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const handleSubmit = async () => {
    const { id, ownerName, initialBalance } = form
    if (!id.trim() || !ownerName.trim() || initialBalance === '') return

    setLoading(true)
    try {
      const res = await createAccount(id.trim(), ownerName.trim(), parseFloat(initialBalance))
      setResult(res)
      if (res.ok) setForm({ id: '', ownerName: '', initialBalance: '' })
    } catch (e) {
      setResult({ ok: false, data: { error: e.message } })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-xl font-semibold mb-1">Créer un compte</h1>
      <p className="text-sm text-stone-500 mb-6">Ouvre un nouveau compte avec un solde initial.</p>

      <div className="card">
        <RouteTag method="POST" path="/api/accounts" />

        <div className="space-y-4 mb-5">
          <div>
            <label className="label">ID du compte</label>
            <input
              className="input"
              name="id"
              value={form.id}
              onChange={handleChange}
              placeholder="ex: ACC001"
            />
          </div>
          <div>
            <label className="label">Nom du titulaire</label>
            <input
              className="input"
              name="ownerName"
              value={form.ownerName}
              onChange={handleChange}
              placeholder="ex: Alice Martin"
            />
          </div>
          <div>
            <label className="label">Solde initial (€)</label>
            <input
              className="input"
              name="initialBalance"
              type="number"
              min="0"
              step="0.01"
              value={form.initialBalance}
              onChange={handleChange}
              placeholder="ex: 1000"
            />
          </div>
        </div>

        <button
          className="btn btn-primary mb-5"
          onClick={handleSubmit}
          disabled={loading}
        >
          {loading ? 'Création...' : 'Créer le compte'}
        </button>

        <ResponseBox result={result} />
      </div>
    </div>
  )
}
