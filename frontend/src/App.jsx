import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/auth/Login';
import AppLayout from './components/Layout/AppLayout'; 
import ProtectedRoute from './components/ProtectedRoute';
import { ROLES } from './contexts/AuthContext';

const Dashboard = () => (
  <div style={{ padding: 24 }}>
    <h2>Dashboard</h2>
    <p>Sisteme başarıyla giriş yaptınız. İçerik buraya gelecek.</p>
  </div>
);

export default function App() {
  return (
    <Routes>
      {/* Herkese Açık Rotalar */}
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Login initialModal="register" />} />
      <Route path="/forgot-password" element={<Login initialModal="forgot" />} />

      {/* Öğrenci Portalı */}
      <Route
        path="/student"
        element={
          <ProtectedRoute allowedRoles={[ROLES.STUDENT]}>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="profile" element={<Dashboard />} />
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
        <Route path="dashboard" element={<Dashboard />} />
      </Route>

      {/* Yanlış URL girilirse Login'e at */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}