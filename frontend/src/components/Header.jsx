import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Header() {
  const { session, logout } = useAuth()
  const navigate = useNavigate()

  if (!session) {
    return null
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="app-header">
      <div className="app-header-inner">
        <Link to={session.admin ? '/admin' : '/'} className="app-brand">
          ValuBank
        </Link>
        <nav className="app-nav">
          <span className="app-nav-greeting">Hi, {session.fullName}</span>
          <button type="button" className="btn btn-secondary" onClick={handleLogout}>
            Log out
          </button>
        </nav>
      </div>
    </header>
  )
}
