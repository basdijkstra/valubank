// Thin wrapper around the Accounts Service HTTP API using plain fetch().
const BASE_URL = import.meta.env.VITE_ACCOUNTS_API_URL || 'http://localhost:8081'

async function handleResponse(response) {
  let body = null
  try {
    body = await response.json()
  } catch {
    // Some error responses may not have a JSON body; ignore parse failures.
  }

  if (!response.ok) {
    const message = (body && body.error) || `Request failed with status ${response.status}`
    throw new Error(message)
  }

  return body
}

export async function login(username, password) {
  const response = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })
  return handleResponse(response)
}

export async function getAccountsForCustomer(customerId) {
  const response = await fetch(`${BASE_URL}/api/customers/${customerId}/accounts`)
  return handleResponse(response)
}

export async function getAccount(accountId) {
  const response = await fetch(`${BASE_URL}/api/accounts/${accountId}`)
  return handleResponse(response)
}

export async function getInterestRate(accountId) {
  const response = await fetch(`${BASE_URL}/api/accounts/${accountId}/interest-rate`)
  return handleResponse(response)
}

// Admin-only in intent (not enforced server-side - this workshop's auth is deliberately simple).
export async function getAllAccounts() {
  const response = await fetch(`${BASE_URL}/api/accounts`)
  return handleResponse(response)
}

export async function applyInterest(accountId) {
  const response = await fetch(`${BASE_URL}/api/accounts/${accountId}/interest`, {
    method: 'PUT'
  })
  return handleResponse(response)
}
