import { useRef, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { sendChatMessage } from '../api/assistant'

export default function Assistant() {
  const { session } = useAuth()
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const messagesEndRef = useRef(null)

  async function handleSubmit(event) {
    event.preventDefault()
    const text = input.trim()
    if (!text || sending) return

    const history = messages.map(({ role, content }) => ({ role, content }))
    const nextMessages = [...messages, { role: 'user', content: text }]
    setMessages(nextMessages)
    setInput('')
    setError('')
    setSending(true)

    try {
      const response = await sendChatMessage(session.customerId, text, history)
      setMessages([...nextMessages, { role: 'assistant', content: response.reply }])
    } catch (err) {
      setError(err.message || 'Failed to reach the assistant.')
    } finally {
      setSending(false)
      setTimeout(() => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 0)
    }
  }

  return (
    <div className="page">
      <h1>Assistant</h1>
      <p className="status-text">Ask about your accounts and payments.</p>

      <div className="card chat-card">
        <div className="chat-messages">
          {messages.length === 0 && (
            <p className="status-text">
              Try asking things like "What's my balance?" or "Show my recent payments."
            </p>
          )}
          {messages.map((msg, i) => (
            <div key={i} className={`chat-message chat-message-${msg.role}`}>
              {msg.content}
            </div>
          ))}
          {sending && <div className="chat-message chat-message-assistant status-text">Thinking...</div>}
          <div ref={messagesEndRef} />
        </div>

        {error && <div className="banner banner-error">{error}</div>}

        <form className="chat-input-row" onSubmit={handleSubmit}>
          <input
            type="text"
            className="chat-input"
            placeholder="Ask a question..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={sending}
          />
          <button type="submit" className="btn btn-primary" disabled={sending || !input.trim()}>
            Send
          </button>
        </form>
      </div>
    </div>
  )
}
