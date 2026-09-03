// Thin wrapper around the Payments Service HTTP API using plain fetch().
const BASE_URL = import.meta.env.VITE_PAYMENTS_API_URL || 'http://localhost:8082'

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

export async function createPayment(payment) {
  const response = await fetch(`${BASE_URL}/api/payments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payment)
  })
  // Note: a business-level rejection/failure (status REJECTED/FAILED) still
  // comes back as a normal 201 response with details in the body - it is
  // only a network/HTTP-level problem that should throw here.
  return handleResponse(response)
}

export async function getPaymentsForAccount(accountId) {
  const response = await fetch(`${BASE_URL}/api/accounts/${accountId}/payments`)
  return handleResponse(response)
}
