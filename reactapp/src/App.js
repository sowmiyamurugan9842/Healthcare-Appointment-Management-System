import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar';
import Home from './components/Home';
import Login from './components/Login';
import Register from './components/Register';
import DepartmentManager from './components/DepartmentManager';
import PatientRegister from './components/PatientRegister';
import DoctorRegister from './components/DoctorRegister';
import PatientList from './components/PatientList';
import DoctorList from './components/DoctorList';
import BookAppointment from './components/BookAppointment';
import AppointmentList from './components/AppointmentList';
import PatientAppointmentView from './components/PatientAppointmentView';

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Load auth state on app start
  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      try {
        setUser(JSON.parse(savedUser));
      } catch (e) {
        console.error('Error parsing stored user data:', e);
        localStorage.removeItem('user');
        localStorage.removeItem('token');
      }
    }
    setLoading(false);
  }, []);

  const handleLoginSuccess = (userData) => {
    setUser(userData);
  };

  const handleLogout = () => {
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    setUser(null);
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', color: '#ffffff' }}>
        <h3>Loading CarePortal...</h3>
      </div>
    );
  }

  // Helper route guards
  const AuthenticatedRoute = ({ children }) => {
    return user ? children : <Navigate to="/login" replace />;
  };

  const RoleRoute = ({ children, allowedRoles }) => {
    if (!user) return <Navigate to="/login" replace />;
    if (!allowedRoles.includes(user.role)) {
      return <Navigate to="/" replace />;
    }
    return children;
  };

  return (
    <Router>
      <div className="app-container">
        <Navbar user={user} onLogout={handleLogout} />
        
        <main className="content-wrapper">
          <Routes>
            {/* Public Routes */}
            <Route path="/" element={<Home user={user} />} />
            <Route 
              path="/login" 
              element={!user ? <Login onLoginSuccess={handleLoginSuccess} /> : <Navigate to="/" replace />} 
            />
            <Route 
              path="/register" 
              element={!user ? <Register /> : <Navigate to="/" replace />} 
            />

            {/* Authenticated Routes */}
            <Route 
              path="/doctors" 
              element={
                <AuthenticatedRoute>
                  <DoctorList user={user} />
                </AuthenticatedRoute>
              } 
            />

            {/* Admin-Only Routes */}
            <Route 
              path="/departments" 
              element={
                <RoleRoute allowedRoles={['ADMIN']}>
                  <DepartmentManager user={user} />
                </RoleRoute>
              } 
            />
            <Route 
              path="/patients/register" 
              element={
                <RoleRoute allowedRoles={['ADMIN']}>
                  <PatientRegister />
                </RoleRoute>
              } 
            />
            <Route 
              path="/doctors/register" 
              element={
                <RoleRoute allowedRoles={['ADMIN']}>
                  <DoctorRegister />
                </RoleRoute>
              } 
            />
            <Route 
              path="/patients" 
              element={
                <RoleRoute allowedRoles={['ADMIN']}>
                  <PatientList user={user} />
                </RoleRoute>
              } 
            />

            {/* Patient & Admin Booking */}
            <Route 
              path="/appointments/book" 
              element={
                <RoleRoute allowedRoles={['ADMIN', 'PATIENT']}>
                  <BookAppointment user={user} />
                </RoleRoute>
              } 
            />

            {/* Doctor & Admin Logs */}
            <Route 
              path="/appointments" 
              element={
                <RoleRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                  <AppointmentList user={user} />
                </RoleRoute>
              } 
            />

            {/* Patient-Only Log */}
            <Route 
              path="/patient-appointments" 
              element={
                <RoleRoute allowedRoles={['PATIENT']}>
                  <PatientAppointmentView user={user} />
                </RoleRoute>
              } 
            />

            {/* Catch all */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
