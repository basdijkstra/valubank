import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function RequireAuth({ children, adminOnly = false }) {
  const { session } = useAuth()

  if (!session) {
    return <Navigate to="/login" replace />
  }

  if (adminOnly && !session.admin) {
    return <Navigate to="/" replace />
  }

  return children
}
