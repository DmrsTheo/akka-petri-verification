import { useState } from 'react'
import { transfer } from '../api'
import ResponseBox from '../components/ResponseBox'
import RouteTag from '../components/RouteTag'

export default function Transfer() {
  const [form, setForm] = useState({ from: '', to: '', amount: '' })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const handleSubmit = async () => {
    const { from, to, amount } = form
    if (!from.trim() || !to.trim() || amount === '') return

    setLoading(true)
    try {
      const res = await transfer(from.trim(), to.trim(), parseFloat(amount))
      setResult(res)
    } catch (e) {
      setResult({ ok: false, data: { error: e.message } })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-xl font-semibold mb-1">Transfert</h1>
      <p className="text-sm text-stone-500 mb-6">Transfère un montant d'un compte vers un autre.</p>

      <div className="card">
        <RouteTag method="POST" path="/api/transfer" />

        <div className="space-y-4 mb-5">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Compte source</label>
              <input
                className="input"
                name="from"
                value={form.from}
                onChange={handleChange}
                placeholder="ex: ACC001"
              />
            </div>
            <div>
              <label className="label">Compte destinataire</label>
              <input
                className="input"
                name="to"
                value={form.to}
                onChange={handleChange}
                placeholder="ex: ACC002"
              />
            </div>
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
              placeholder="ex: 100"
            />
          </div>
        </div>

        <button
          className="btn btn-primary mb-5"
          onClick={handleSubmit}
          disabled={loading}
        >
          {loading ? 'Transfert en cours...' : 'Transférer →'}
        </button>

        <ResponseBox result={result} />
      </div>
    </div>
  )
}
