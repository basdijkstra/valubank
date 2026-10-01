import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getAccountsForCustomer } from '../api/accounts'
import {
  cancelScheduledPayment,
  getScheduledPaymentsForCustomer,
  schedulePayment
} from '../api/payments'
import { formatBalance } from '../utils/format'
import { isValidIban } from '../utils/iban'

// Local-date YYYY-MM-DD for "tomorrow" - the earliest allowed execution date.
function tomorrowIsoDate() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function formatDate(isoDate) {
  if (!isoDate) return ''
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString()
}

const emptyForm = {
  fromAccountId: '',
  toAccountIban: '',
  toAccountName: '',
  amount: '',
  executionDate: '',
  description: ''
}

export default function ScheduledPayments() {
  const { session } = useAuth()
  const [searchParams] = useSearchParams()

  const [accounts, setAccounts] = useState([])
  const [scheduledPayments, setScheduledPayments] = useState([])
  const [loadingAccounts, setLoadingAccounts] = useState(true)
  const [loadingScheduled, setLoadingScheduled] = useState(true)
  const [accountsError, setAccountsError] = useState('')
  const [scheduledError, setScheduledError] = useState('')

  const [form, setForm] = useState(() => ({
    ...emptyForm,
    fromAccountId: searchParams.get('accountId') || ''
  }))
  const [formError, setFormError] = useState('')
  const [confirming, setConfirming] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [lastScheduled, setLastScheduled] = useState(null)

  const [cancellingId, setCancellingId] = useState(null)
  const [cancelError, setCancelError] = useState('')

  const loadAccounts = useCallback(async () => {
    setLoadingAccounts(true)
    setAccountsError('')
    try {
      const data = await getAccountsForCustomer(session.customerId)
      setAccounts(data)
      // Default to the first account, unless one was preselected (e.g. from account detail).
      setForm((prev) => {
        const preselectedIsOwn = data.some((a) => String(a.id) === String(prev.fromAccountId))
        if (preselectedIsOwn || data.length === 0) return prev
        return { ...prev, fromAccountId: String(data[0].id) }
      })
    } catch (err) {
      setAccountsError(err.message || 'Failed to load accounts.')
    } finally {
      setLoadingAccounts(false)
    }
  }, [session.customerId])

  const loadScheduledPayments = useCallback(async () => {
    setLoadingScheduled(true)
    setScheduledError('')
    try {
      setScheduledPayments(await getScheduledPaymentsForCustomer(session.customerId))
    } catch (err) {
      setScheduledError(err.message || 'Failed to load scheduled payments.')
    } finally {
      setLoadingScheduled(false)
    }
  }, [session.customerId])

  useEffect(() => {
    loadAccounts()
    loadScheduledPayments()
  }, [loadAccounts, loadScheduledPayments])

  const accountsById = Object.fromEntries(accounts.map((a) => [String(a.id), a]))
  const selectedAccount = accountsById[String(form.fromAccountId)]

  function handleFormChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
    setFormError('')
  }

  function validate() {
    if (!selectedAccount) return 'Please select a source account.'
    if (!isValidIban(form.toAccountIban)) return 'Please enter a valid IBAN (e.g. NL91ABNA0417164300).'
    if (!(Number(form.amount) > 0)) return 'Amount must be greater than zero.'
    if (!form.executionDate || form.executionDate < tomorrowIsoDate()) {
      return 'Execution date must be in the future.'
    }
    return ''
  }

  function handleReview(event) {
    event.preventDefault()
    setSubmitError('')
    setLastScheduled(null)
    const error = validate()
    if (error) {
      setFormError(error)
      return
    }
    setConfirming(true)
  }

  async function handleConfirm() {
    setSubmitting(true)
    setSubmitError('')
    try {
      const result = await schedulePayment({
        customerId: session.customerId,
        fromAccountId: selectedAccount.id,
        toAccountIban: form.toAccountIban,
        toAccountName: form.toAccountName,
        amount: Number(form.amount),
        executionDate: form.executionDate,
        description: form.description
      })
      setLastScheduled(result)
      setForm({ ...emptyForm, fromAccountId: form.fromAccountId })
      setConfirming(false)
      await loadScheduledPayments()
    } catch (err) {
      setSubmitError(err.message || 'Failed to schedule payment.')
      setConfirming(false)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCancel(scheduledPaymentId) {
    setCancellingId(scheduledPaymentId)
    setCancelError('')
    try {
      await cancelScheduledPayment(scheduledPaymentId, session.customerId)
    } catch (err) {
      setCancelError(err.message || 'Failed to cancel scheduled payment.')
    } finally {
      setCancellingId(null)
      // Reload either way: on failure the payment may have been executed meanwhile.
      await loadScheduledPayments()
    }
  }

  return (
    <div className="page">
      <Link to="/" className="back-link">
        &larr; Back to accounts
      </Link>

      <h1>Scheduled payments</h1>

      <div className="detail-columns">
        <section className="card">
          <h2>Schedule a payment</h2>

          {loadingAccounts && <p className="status-text">Loading accounts...</p>}
          {accountsError && <div className="banner banner-error">{accountsError}</div>}

          {lastScheduled && (
            <div className="banner banner-success">
              <strong>{lastScheduled.status}</strong>
              <span> - payment scheduled for {formatDate(lastScheduled.executionDate)}</span>
            </div>
          )}
          {submitError && <div className="banner banner-error">{submitError}</div>}

          {!confirming && (
            <form onSubmit={handleReview} className="form" noValidate>
              <div className="form-field">
                <label htmlFor="scheduled-from-account">From account</label>
                <select
                  id="scheduled-from-account"
                  value={form.fromAccountId}
                  onChange={(e) => handleFormChange('fromAccountId', e.target.value)}
                  required
                >
                  {accounts.map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.iban} ({account.accountType}, {formatBalance(account.balance, account.currency)})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-field">
                <label htmlFor="scheduled-to-iban">Beneficiary IBAN</label>
                <input
                  id="scheduled-to-iban"
                  type="text"
                  value={form.toAccountIban}
                  onChange={(e) => handleFormChange('toAccountIban', e.target.value)}
                  required
                />
              </div>

              <div className="form-field">
                <label htmlFor="scheduled-to-name">Beneficiary name</label>
                <input
                  id="scheduled-to-name"
                  type="text"
                  value={form.toAccountName}
                  onChange={(e) => handleFormChange('toAccountName', e.target.value)}
                  required
                />
              </div>

              <div className="form-field">
                <label htmlFor="scheduled-amount">
                  Amount{selectedAccount ? ` (${selectedAccount.currency})` : ''}
                </label>
                <input
                  id="scheduled-amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={form.amount}
                  onChange={(e) => handleFormChange('amount', e.target.value)}
                  required
                />
              </div>

              <div className="form-field">
                <label htmlFor="scheduled-execution-date">Execution date</label>
                <input
                  id="scheduled-execution-date"
                  type="date"
                  min={tomorrowIsoDate()}
                  value={form.executionDate}
                  onChange={(e) => handleFormChange('executionDate', e.target.value)}
                  required
                />
              </div>

              <div className="form-field">
                <label htmlFor="scheduled-description">Description</label>
                <input
                  id="scheduled-description"
                  type="text"
                  value={form.description}
                  onChange={(e) => handleFormChange('description', e.target.value)}
                />
              </div>

              {formError && <div className="banner banner-error form-error">{formError}</div>}

              <button type="submit" className="btn btn-primary" disabled={accounts.length === 0}>
                Review payment
              </button>
            </form>
          )}

          {confirming && selectedAccount && (
            <div className="scheduled-confirm">
              <p>Please confirm this scheduled payment:</p>
              <div className="account-summary">
                <div className="account-summary-row">
                  <span className="label">From</span>
                  <span>{selectedAccount.iban}</span>
                </div>
                <div className="account-summary-row">
                  <span className="label">To</span>
                  <span>
                    {form.toAccountName} ({form.toAccountIban})
                  </span>
                </div>
                <div className="account-summary-row">
                  <span className="label">Amount</span>
                  <span>{formatBalance(Number(form.amount), selectedAccount.currency)}</span>
                </div>
                <div className="account-summary-row">
                  <span className="label">Execution date</span>
                  <span>{formatDate(form.executionDate)}</span>
                </div>
                {form.description && (
                  <div className="account-summary-row">
                    <span className="label">Description</span>
                    <span>{form.description}</span>
                  </div>
                )}
              </div>
              <div className="button-row">
                <button type="button" className="btn btn-primary" onClick={handleConfirm} disabled={submitting}>
                  {submitting ? 'Scheduling...' : 'Confirm'}
                </button>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setConfirming(false)}
                  disabled={submitting}
                >
                  Back
                </button>
              </div>
            </div>
          )}
        </section>

        <section className="card">
          <h2>Your scheduled payments</h2>

          {loadingScheduled && <p className="status-text">Loading scheduled payments...</p>}
          {scheduledError && <div className="banner banner-error">{scheduledError}</div>}
          {cancelError && <div className="banner banner-error">{cancelError}</div>}

          {!loadingScheduled && !scheduledError && scheduledPayments.length === 0 && (
            <p className="status-text">No scheduled payments yet.</p>
          )}

          {!loadingScheduled && !scheduledError && scheduledPayments.length > 0 && (
            <ul className="payment-list">
              {scheduledPayments.map((sp) => (
                <li key={sp.id} className="payment-list-item" data-testid={`scheduled-payment-${sp.id}`}>
                  <div className="payment-list-row">
                    <span className="payment-to">{sp.toAccountName}</span>
                    <span className={`payment-status payment-status-${sp.status.toLowerCase()}`}>
                      {sp.status}
                    </span>
                  </div>
                  <div className="payment-list-row">
                    <span>{sp.toAccountIban}</span>
                    <span>{formatBalance(sp.amount, sp.currency)}</span>
                  </div>
                  <div className="payment-list-row payment-timestamp">
                    <span>From {accountsById[String(sp.fromAccountId)]?.iban || `account ${sp.fromAccountId}`}</span>
                    <span>Execution date: {formatDate(sp.executionDate)}</span>
                  </div>
                  {sp.description && <div className="payment-description">{sp.description}</div>}
                  {sp.reason && <div className="payment-reason">{sp.reason}</div>}
                  {sp.status === 'SCHEDULED' && (
                    <div>
                      <button
                        type="button"
                        className="btn btn-outline btn-small"
                        onClick={() => handleCancel(sp.id)}
                        disabled={cancellingId === sp.id}
                      >
                        {cancellingId === sp.id ? 'Cancelling...' : 'Cancel'}
                      </button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
