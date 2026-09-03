import { Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import RequireAuth from './components/RequireAuth'
import Header from './components/Header'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import AccountDetail from './pages/AccountDetail'
import AdminDashboard from './pages/AdminDashboard'

export default function App() {
  return (
    <AuthProvider>
      <Header />
      <main className="app-main">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <RequireAuth>
                <Dashboard />
              </RequireAuth>
            }
          />
          <Route
            path="/accounts/:accountId"
            element={
              <RequireAuth>
                <AccountDetail />
              </RequireAuth>
            }
          />
          <Route
            path="/admin"
            element={
              <RequireAuth adminOnly>
                <AdminDashboard />
              </RequireAuth>
            }
          />
        </Routes>
      </main>
    </AuthProvider>
  )
}
