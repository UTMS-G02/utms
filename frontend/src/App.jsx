import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/auth/Login';
import StudentDashboard from './pages/student/Dashboard';
import ApplicationList from './pages/student/ApplicationList';
import ApplicationDetail from './pages/student/ApplicationDetail';
import ApplicationForm from './pages/student/ApplicationForm';
import Profile from './pages/student/Profile';
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
      </Route>

      {/* Yanlış URL girilirse Login'e at */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}