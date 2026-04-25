import { useState } from 'react'
import Health from './pages/Health'
import Accounts from './pages/Accounts'
import CreateAccount from './pages/CreateAccount'
import Deposit from './pages/Deposit'
import Withdraw from './pages/Withdraw'
import Transfer from './pages/Transfer'

const PAGES = [
  { id: 'health',   label: 'Santé',         icon: '◎', component: Health },
  { id: 'accounts', label: 'Comptes',        icon: '≡', component: Accounts },
  { id: 'create',   label: 'Créer compte',   icon: '+', component: CreateAccount },
  { id: 'deposit',  label: 'Dépôt',          icon: '↓', component: Deposit },
  { id: 'withdraw', label: 'Retrait',         icon: '↑', component: Withdraw },
  { id: 'transfer', label: 'Transfert',       icon: '⇄', component: Transfer },
]

export default function App() {
  const [activePage, setActivePage] = useState('health')

  const CurrentPage = PAGES.find((p) => p.id === activePage)?.component ?? Health

  return (
    <div className="flex h-screen bg-stone-50">

      {/* Sidebar */}
      <aside className="w-52 border-r border-stone-200 bg-white flex flex-col py-5 gap-1 shrink-0">
        <div className="px-4 mb-3">
          <span className="text-xs font-semibold tracking-widest text-stone-400 uppercase">
            Payment API
          </span>
        </div>

        {PAGES.map((page) => (
          <button
            key={page.id}
            className={`nav-item ${activePage === page.id ? 'active' : ''}`}
            onClick={() => setActivePage(page.id)}
          >
            <span className="text-base w-5 text-center">{page.icon}</span>
            {page.label}
          </button>
        ))}

        <div className="mt-auto px-4 pt-4 border-t border-stone-100">
          <p className="text-xs text-stone-400">Akka HTTP · port 8080</p>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto p-8 max-w-2xl">
        <CurrentPage />
      </main>
    </div>
  )
}
