import { createContext, useContext, useState, useCallback } from 'react'

const STORAGE_KEY = 'valubank.session'

const AuthContext = createContext(null)

function readStoredSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => readStoredSession())

  // NOTE: this is a workshop demo - the "session" is just the customer's
  // profile info kept in React context + localStorage so a page refresh
  // doesn't log the user out. There is no real authentication (no JWT /
  // session token / password hashing on the wire) here on purpose.
  const login = useCallback((profile) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(profile))
    setSession(profile)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY)
    setSession(null)
  }, [])

  return (
    <AuthContext.Provider value={{ session, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
