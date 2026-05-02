import { Navigate, useLocation } from 'react-router-dom'
import { Spin } from 'antd'
import { useAuth, ROLE_HOME } from '../contexts/AuthContext'

/**
 * ProtectedRoute
 *
 * Props:
 *   allowedRoles  string[]   - list of ROLES allowed to access this route
 *   children      ReactNode  - the protected page component
 *
 * Behaviour:
 *   • Loading  → full-page spinner (avoids flash of redirect)
 *   • Not auth → /login (preserves attempted URL via state.from)
 *   • Wrong role → redirect to user's own home dashboard
 *   • OK → render children
 */
export default function ProtectedRoute({ allowedRoles = [], children }) {
  const { isAuthenticated, user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100vh',
        }}
      >
        <Spin size="large" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (allowedRoles.length > 0 && !allowedRoles.includes(user?.role)) {
    // User is authenticated but wrong role → send to their own dashboard
    const home = ROLE_HOME[user?.role] ?? '/login'
    return <Navigate to={home} replace />
  }

  return children
}