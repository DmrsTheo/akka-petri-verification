// Toutes les fonctions d'appel à l'API Akka
// Le proxy Vite redirige /api → http://localhost:8080/api en dev

const BASE = '/api'

async function request(path, method = 'GET', body = null) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body) opts.body = JSON.stringify(body)

  const res = await fetch(BASE + path, opts)
  const data = await res.json()
  return { ok: res.ok, status: res.status, data }
}

// GET /api/health
export const checkHealth = () => request('/health')

// GET /api/accounts/
export const listAccounts = () => request('/accounts/')

// GET /api/accounts/:id
export const getAccount = (id) => request(`/accounts/${id}`)

// POST /api/accounts
export const createAccount = (id, ownerName, initialBalance) =>
  request('/accounts', 'POST', { id, ownerName, initialBalance })

// POST /api/accounts/:id/deposit
export const deposit = (id, amount) =>
  request(`/accounts/${id}/deposit`, 'POST', { amount })

// POST /api/accounts/:id/withdraw
export const withdraw = (id, amount) =>
  request(`/accounts/${id}/withdraw`, 'POST', { amount })

// POST /api/transfer
export const transfer = (fromAccountId, toAccountId, amount) =>
  request('/transfer', 'POST', { fromAccountId, toAccountId, amount })
