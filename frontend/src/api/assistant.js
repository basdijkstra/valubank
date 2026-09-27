// Thin wrapper around the Assistant Service HTTP API using plain fetch().
const BASE_URL = import.meta.env.VITE_ASSISTANT_API_URL || 'http://localhost:8086'

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

export async function sendChatMessage(customerId, message, history) {
  const response = await fetch(`${BASE_URL}/api/assistant/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ customerId, message, history })
  })
  return handleResponse(response)
}
