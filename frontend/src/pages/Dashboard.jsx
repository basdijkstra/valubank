import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getAccountsForCustomer } from '../api/accounts'
import { formatBalance } from '../utils/format'

export default function Dashboard() {
  const { session } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function loadAccounts() {
      setLoading(true)
      setError('')
      try {
        const data = await getAccountsForCustomer(session.customerId)
        if (!cancelled) {
          setAccounts(data)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Failed to load accounts.')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadAccounts()

    return () => {
      cancelled = true
    }
  }, [session.customerId])

  return (
    <div className="page">
      <h1>Your accounts</h1>

      {loading && <p className="status-text">Loading accounts...</p>}
      {error && <div className="banner banner-error">{error}</div>}

      {!loading && !error && accounts.length === 0 && (
        <p className="status-text">No accounts found for this customer.</p>
      )}

      {!loading && !error && accounts.length > 0 && (
        <div className="account-grid">
          {accounts.map((account) => (
            <Link
              to={`/accounts/${account.id}`}
              key={account.id}
              className="card account-card"
            >
              <div className="account-card-type">{account.accountType}</div>
              <div className="account-card-iban">{account.iban}</div>
              <div className="account-card-balance">
                {formatBalance(account.balance, account.currency)}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
