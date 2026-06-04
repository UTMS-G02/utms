import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/auth/Login'
import ActivatePage from './pages/auth/ActivatePage';
import KvkkPage from './pages/auth/KvkkPage';
import StudentDashboard from './pages/student/Dashboard';
import ApplicationList from './pages/student/ApplicationList';
import ApplicationDetail from './pages/student/ApplicationDetail';
import ApplicationForm from './pages/student/ApplicationForm';
import Profile from './pages/student/Profile';
import OidbDashboard from './pages/oidb/Dashboard';
import YdyoDashboard from './pages/ydyo/YdyoDashboard';
import YdyoApplicationDetail from './pages/ydyo/YdyoApplicationDetail';
import DekanlikOfisi from './pages/dean/DekanlikOfisi';
import FakulteKurulu from './pages/dean/FakulteKurulu';
import YGK from './pages/dean/YGK';
import AppLayout from './components/Layout/AppLayout';
import ProtectedRoute from './components/ProtectedRoute';
import { ROLES } from './contexts/AuthContext';


export default function App() {
  return (
    <Routes>
      {/* Herkese Açık Rotalar */}
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Login initialModal="register" />} />
      <Route path="/forgot-password" element={<Login initialModal="forgot" />} />
      <Route path="/reset-password" element={<Login initialModal="reset" />} />
      <Route path="/activate" element={<ActivatePage />} />
      <Route path="/kvkk" element={<KvkkPage />} />

      {/* Öğrenci Portalı */}
      <Route
        path="/student"
        element={
          <ProtectedRoute allowedRoles={[ROLES.STUDENT]}>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<StudentDashboard />} />
        <Route path="dashboard" element={<StudentDashboard />} />
        <Route path="applications" element={<ApplicationList />} />
        <Route path="applications/new" element={<ApplicationForm />} />
        <Route path="applications/:id" element={<ApplicationDetail />} />
        <Route path="profile" element={<Profile />} />
      </Route>

      {/* OIDB Portalı */}
      <Route
        path="/oidb"
        element={
          <ProtectedRoute allowedRoles={[ROLES.OIDB]}>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<OidbDashboard />} />
        <Route path="dashboard" element={<OidbDashboard />} />
        <Route path="pending" element={<OidbDashboard />} />
      </Route>

      {/* YDYO (Yabancı Diller) Portalı */}
      <Route
        path="/ydyo"
        element={
          <ProtectedRoute allowedRoles={[ROLES.YDYO]}>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<YdyoDashboard />} />
        <Route path="dashboard" element={<YdyoDashboard />} />
        <Route path="applications/:id" element={<YdyoApplicationDetail />} />
      </Route>

      {/* Dekanlık Portalı */}
      <Route
        path="/dean"
        element={
          <ProtectedRoute allowedRoles={[ROLES.DEAN_OFFICE]}>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="dashboard" element={<div style={{ padding: 24 }}><h2>Dean Dashboard</h2></div>} />
        <Route path="dekanlık-ofisi" element={<DekanlikOfisi />} />
        <Route path="faculty-board" element={<FakulteKurulu />} />
        <Route path="ygk" element={<YGK />} />
      </Route>

      {/* Yanlış URL girilirse Login'e at */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}