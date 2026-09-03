import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getAccount, getInterestRate } from '../api/accounts'
import { createPayment, getPaymentsForAccount } from '../api/payments'

function formatBalance(balance, currency) {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(balance)
  } catch {
    return `${balance} ${currency}`
  }
}

function formatTimestamp(timestamp) {
  if (!timestamp) return ''
  try {
    return new Date(timestamp).toLocaleString()
  } catch {
    return timestamp
  }
}

const emptyForm = {
  toAccountIban: '',
  toAccountName: '',
  amount: '',
  currency: '',
  description: ''
}

export default function AccountDetail() {
  const { accountId } = useParams()

  const [account, setAccount] = useState(null)
  const [interestRate, setInterestRate] = useState(null)
  const [payments, setPayments] = useState([])

  const [loadingAccount, setLoadingAccount] = useState(true)
  const [loadingRate, setLoadingRate] = useState(true)
  const [loadingPayments, setLoadingPayments] = useState(true)

  const [accountError, setAccountError] = useState('')
  const [rateError, setRateError] = useState('')
  const [paymentsError, setPaymentsError] = useState('')

  const [form, setForm] = useState(emptyForm)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [lastPaymentResult, setLastPaymentResult] = useState(null)

  const loadAccount = useCallback(async () => {
    setLoadingAccount(true)
    setAccountError('')
    try {
      const data = await getAccount(accountId)
      setAccount(data)
      // Default the payment form currency to the account's currency once
      // we know it, but don't clobber anything the user already typed.
      setForm((prev) => (prev.currency ? prev : { ...prev, currency: data.currency }))
    } catch (err) {
      setAccountError(err.message || 'Failed to load account.')
    } finally {
      setLoadingAccount(false)
    }
  }, [accountId])

  const loadInterestRate = useCallback(async () => {
    setLoadingRate(true)
    setRateError('')
    try {
      const data = await getInterestRate(accountId)
      setInterestRate(data)
    } catch (err) {
      setRateError(err.message || 'Failed to load interest rate.')
    } finally {
      setLoadingRate(false)
    }
  }, [accountId])

  const loadPayments = useCallback(async () => {
    setLoadingPayments(true)
    setPaymentsError('')
    try {
      const data = await getPaymentsForAccount(accountId)
      // Show most recent payments first.
      const sorted = [...data].sort(
        (a, b) => new Date(b.timestamp) - new Date(a.timestamp)
      )
      setPayments(sorted)
    } catch (err) {
      setPaymentsError(err.message || 'Failed to load payment history.')
    } finally {
      setLoadingPayments(false)
    }
  }, [accountId])

  useEffect(() => {
    loadAccount()
    loadInterestRate()
    loadPayments()
  }, [loadAccount, loadInterestRate, loadPayments])

  function handleFormChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmitPayment(event) {
    event.preventDefault()
    setSubmitError('')
    setLastPaymentResult(null)
    setSubmitting(true)

    try {
      const result = await createPayment({
        fromAccountId: account.id,
        toAccountIban: form.toAccountIban,
        toAccountName: form.toAccountName,
        amount: Number(form.amount),
        currency: form.currency,
        description: form.description
      })

      setLastPaymentResult(result)
      setForm({ ...emptyForm, currency: account.currency })

      // The account balance shown to the user is only current as of the
      // last GET, so re-fetch the account (and payment history) after
      // submitting a payment to reflect any change.
      await Promise.all([loadAccount(), loadPayments()])
    } catch (err) {
      setSubmitError(err.message || 'Failed to submit payment.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page">
      <Link to="/" className="back-link">
        &larr; Back to accounts
      </Link>

      <h1>Account detail</h1>

      {loadingAccount && <p className="status-text">Loading account...</p>}
      {accountError && <div className="banner banner-error">{accountError}</div>}

      {account && (
        <div className="card account-summary">
          <div className="account-summary-row">
            <span className="label">IBAN</span>
            <span>{account.iban}</span>
          </div>
          <div className="account-summary-row">
            <span className="label">Type</span>
            <span>{account.accountType}</span>
          </div>
          <div className="account-summary-row">
            <span className="label">Balance</span>
            <span className="account-summary-balance">
              {formatBalance(account.balance, account.currency)}
            </span>
          </div>
          <div className="account-summary-row">
            <span className="label">Currency</span>
            <span>{account.currency}</span>
          </div>
          <div className="account-summary-row">
            <span className="label">Interest rate</span>
            <span>
              {loadingRate && 'Loading...'}
              {rateError && <span className="text-error">{rateError}</span>}
              {interestRate && !loadingRate && !rateError && `${interestRate.ratePercentage}%`}
            </span>
          </div>
        </div>
      )}

      <div className="detail-columns">
        <section className="card">
          <h2>Make a payment</h2>

          {lastPaymentResult && (
            <div
              className={`banner banner-${lastPaymentResult.status === 'COMPLETED' ? 'success' : 'error'}`}
            >
              <strong>{lastPaymentResult.status}</strong>
              {lastPaymentResult.reason && <span> - {lastPaymentResult.reason}</span>}
            </div>
          )}

          {submitError && <div className="banner banner-error">{submitError}</div>}

          <form onSubmit={handleSubmitPayment} className="form">
            <label className="form-field">
              <span>Destination IBAN</span>
              <input
                type="text"
                value={form.toAccountIban}
                onChange={(e) => handleFormChange('toAccountIban', e.target.value)}
                required
              />
            </label>

            <label className="form-field">
              <span>Destination name</span>
              <input
                type="text"
                value={form.toAccountName}
                onChange={(e) => handleFormChange('toAccountName', e.target.value)}
                required
              />
            </label>

            <label className="form-field">
              <span>Amount</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.amount}
                onChange={(e) => handleFormChange('amount', e.target.value)}
                required
              />
            </label>

            <label className="form-field">
              <span>Currency</span>
              <input
                type="text"
                value={form.currency}
                onChange={(e) => handleFormChange('currency', e.target.value)}
                required
              />
            </label>

            <label className="form-field">
              <span>Description</span>
              <input
                type="text"
                value={form.description}
                onChange={(e) => handleFormChange('description', e.target.value)}
              />
            </label>

            <button type="submit" className="btn btn-primary" disabled={submitting || !account}>
              {submitting ? 'Submitting...' : 'Send payment'}
            </button>
          </form>
        </section>

        <section className="card">
          <h2>Payment history</h2>

          {loadingPayments && <p className="status-text">Loading payment history...</p>}
          {paymentsError && <div className="banner banner-error">{paymentsError}</div>}

          {!loadingPayments && !paymentsError && payments.length === 0 && (
            <p className="status-text">No payments yet.</p>
          )}

          {!loadingPayments && !paymentsError && payments.length > 0 && (
            <ul className="payment-list">
              {payments.map((payment) => (
                <li key={payment.id} className="payment-list-item">
                  <div className="payment-list-row">
                    <span className="payment-to">{payment.toAccountName}</span>
                    <span className={`payment-status payment-status-${payment.status.toLowerCase()}`}>
                      {payment.status}
                    </span>
                  </div>
                  <div className="payment-list-row">
                    <span>{payment.toAccountIban}</span>
                    <span>
                      {formatBalance(payment.amount, payment.currency)}
                    </span>
                  </div>
                  {payment.description && (
                    <div className="payment-description">{payment.description}</div>
                  )}
                  {payment.reason && (
                    <div className="payment-reason">{payment.reason}</div>
                  )}
                  <div className="payment-timestamp">{formatTimestamp(payment.timestamp)}</div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
