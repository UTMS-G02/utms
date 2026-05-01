import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';

const Dashboard = () => (
	<div style={{ padding: 24 }}>
		<h2>Dashboard (geçici)</h2>
		<p>Bu sayfa daha sonra role-dayalı içerikle doldurulacak.</p>
	</div>
);

export default function App() {
	return (
		<BrowserRouter>
			<Routes>
				<Route path="/" element={<Navigate to="/login" replace />} />
				<Route path="/login" element={<Login />} />
				<Route path="/register" element={<Register />} />
				<Route path="/dashboard" element={<Dashboard />} />
			</Routes>
		</BrowserRouter>
	);
}
