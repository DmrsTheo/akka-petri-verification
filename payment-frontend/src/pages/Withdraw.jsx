import { useState } from 'react'
import { withdraw } from '../api'
import ResponseBox from '../components/ResponseBox'
import RouteTag from '../components/RouteTag'

export default function Withdraw() {
  const [form, setForm] = useState({ id: '', amount: '' })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const handleSubmit = async () => {
    const { id, amount } = form
    if (!id.trim() || amount === '') return

    setLoading(true)
    try {
      const res = await withdraw(id.trim(), parseFloat(amount))
      setResult(res)
    } catch (e) {
      setResult({ ok: false, data: { error: e.message } })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-xl font-semibold mb-1">Retrait</h1>
      <p className="text-sm text-stone-500 mb-6">Débite un montant d'un compte existant.</p>

      <div className="card">
        <RouteTag method="POST" path="/api/accounts/{id}/withdraw" />

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
            <label className="label">Montant (€)</label>
            <input
              className="input"
              name="amount"
              type="number"
              min="0.01"
              step="0.01"
              value={form.amount}
              onChange={handleChange}
              placeholder="ex: 200"
            />
          </div>
        </div>

        <button
          className="btn btn-primary mb-5"
          onClick={handleSubmit}
          disabled={loading}
        >
          {loading ? 'Envoi...' : 'Retirer'}
        </button>

        <ResponseBox result={result} />
      </div>
    </div>
  )
}
