import { useCallback, useEffect, useState } from 'react'
import { applyInterest, getAllAccounts } from '../api/accounts'
import { formatBalance } from '../utils/format'

export default function AdminDashboard() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [selectedIds, setSelectedIds] = useState(new Set())
  const [applying, setApplying] = useState(false)
  const [results, setResults] = useState(null)

  const loadAccounts = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await getAllAccounts()
      setAccounts(data)
      // Drop any selection for accounts that no longer exist after a reload.
      setSelectedIds((prev) => new Set([...prev].filter((id) => data.some((a) => a.id === id))))
    } catch (err) {
      setError(err.message || 'Failed to load accounts.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadAccounts()
  }, [loadAccounts])

  function toggleOne(accountId) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(accountId)) {
        next.delete(accountId)
      } else {
        next.add(accountId)
      }
      return next
    })
  }

  function toggleAll() {
    setSelectedIds((prev) =>
      prev.size === accounts.length ? new Set() : new Set(accounts.map((a) => a.id))
    )
  }

  async function handleAddInterest() {
    setApplying(true)
    setResults(null)

    const targets = accounts.filter((a) => selectedIds.has(a.id))
    const outcomes = await Promise.allSettled(targets.map((a) => applyInterest(a.id)))

    const summary = targets.map((account, index) => {
      const outcome = outcomes[index]
      return outcome.status === 'fulfilled'
        ? { account, ok: true, data: outcome.value }
        : { account, ok: false, message: outcome.reason?.message || 'Failed to add interest.' }
    })

    setResults(summary)
    setSelectedIds(new Set())
    setApplying(false)
    await loadAccounts()
  }

  const allSelected = accounts.length > 0 && selectedIds.size === accounts.length

  return (
    <div className="page">
      <h1>Admin - all accounts</h1>

      {loading && <p className="status-text">Loading accounts...</p>}
      {error && <div className="banner banner-error">{error}</div>}

      {results && (
        <div className="card admin-results">
          {results.map(({ account, ok, data, message }) => (
            <div
              key={account.id}
              className={`banner ${ok ? 'banner-success' : 'banner-error'} admin-result-row`}
            >
              <strong>{account.iban}</strong>
              {ok
                ? ` - added ${formatBalance(data.interestAmount, data.currency)} interest (new balance ${formatBalance(data.newBalance, data.currency)})`
                : ` - ${message}`}
            </div>
          ))}
        </div>
      )}

      {!loading && !error && accounts.length === 0 && (
        <p className="status-text">No accounts found.</p>
      )}

      {!loading && !error && accounts.length > 0 && (
        <>
          <div className="admin-toolbar">
            <button
              type="button"
              className="btn btn-primary"
              disabled={selectedIds.size === 0 || applying}
              onClick={handleAddInterest}
            >
              {applying ? 'Adding interest...' : `Add interest to selected (${selectedIds.size})`}
            </button>
          </div>

          <div className="table-wrapper">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>
                    <input
                      type="checkbox"
                      checked={allSelected}
                      onChange={toggleAll}
                      aria-label="Select all accounts"
                    />
                  </th>
                  <th>Owner</th>
                  <th>IBAN</th>
                  <th>Type</th>
                  <th>Balance</th>
                </tr>
              </thead>
              <tbody>
                {accounts.map((account) => (
                  <tr key={account.id}>
                    <td>
                      <input
                        type="checkbox"
                        checked={selectedIds.has(account.id)}
                        onChange={() => toggleOne(account.id)}
                        aria-label={`Select account ${account.iban}`}
                      />
                    </td>
                    <td>
                      {account.ownerFullName}{' '}
                      <span className="status-text">({account.ownerUsername})</span>
                    </td>
                    <td className="account-card-iban">{account.iban}</td>
                    <td>{account.accountType}</td>
                    <td className="account-summary-balance">
                      {formatBalance(account.balance, account.currency)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}
