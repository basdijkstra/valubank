import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { login as loginRequest } from '../api/accounts'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const profile = await loginRequest(username, password)
      login(profile)
      const redirectTo = profile.admin ? '/admin' : location.state?.from?.pathname || '/'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(err.message || 'Unable to log in. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="centered-page">
      <div className="card login-card">
        <h1 className="login-title">ValuBank</h1>
        <p className="login-subtitle">Log in to manage your accounts</p>

        <form onSubmit={handleSubmit} className="form">
          <label className="form-field">
            <span>Username</span>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </label>

          <label className="form-field">
            <span>Password</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </label>

          {error && <div className="banner banner-error">{error}</div>}

          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Logging in...' : 'Log in'}
          </button>
        </form>

        <p className="login-hint">
          Demo users: <code>alice</code> or <code>bob</code>, password{' '}
          <code>password123</code>
          <br />
          Admin: <code>admin</code>, password <code>admin123</code>
        </p>
      </div>
    </div>
  )
}
