import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { departmentAPI, patientAPI, doctorAPI, appointmentAPI } from '../services/api';

function Home({ user }) {
  const [stats, setStats] = useState({
    departments: 0,
    patients: 0,
    doctors: 0,
    appointments: 0
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Only administrators have permissions to query all metadata for statistics
    if (user && user.role === 'ADMIN') {
      const loadStats = async () => {
        setLoading(true);
        try {
          const [depts, pats, docs, appts] = await Promise.all([
            departmentAPI.getAll().catch(() => []),
            patientAPI.getAll().catch(() => []),
            doctorAPI.getAll().catch(() => []),
            appointmentAPI.getAll().catch(() => [])
          ]);

          setStats({
            departments: depts?.length || 0,
            patients: pats?.length || 0,
            doctors: docs?.length || 0,
            appointments: appts?.length || 0
          });
        } catch (error) {
          console.error('Failed to load dashboard statistics:', error);
        } finally {
          setLoading(false);
        }
      };

      loadStats();
    }
  }, [user]);

  if (!user) {
    return (
      <div style={{ textAlign: 'center', padding: '3rem 1rem' }}>
        <h1 style={{ fontSize: '3rem', marginBottom: '1rem' }}>
          Welcome to <span>CarePortal</span>
        </h1>
        <p style={{ fontSize: '1.25rem', maxWidth: '700px', margin: '0 auto 2.5rem', color: 'var(--text-secondary)' }}>
          Manage clinical registries, doctor specialization searches, hospital department listings, and appointment schedules effortlessly.
        </p>

        <div style={{ display: 'flex', gap: '1.5rem', justifyContent: 'center' }}>
          <Link to="/login" className="btn btn-primary" style={{ padding: '0.85rem 2rem', fontSize: '1.05rem' }}>
            Sign In to Account
          </Link>
          <Link to="/register" className="btn btn-secondary" style={{ padding: '0.85rem 2rem', fontSize: '1.05rem' }}>
            Register New User
          </Link>
        </div>

        <div className="dashboard-grid" style={{ marginTop: '5rem', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))' }}>
          <div className="card" style={{ padding: '1.5rem', textAlign: 'left' }}>
            <span style={{ fontSize: '2.5rem' }}>📅</span>
            <h3 style={{ margin: '0.75rem 0 0.5rem', color: '#ffffff' }}>Schedule Appointments</h3>
            <p style={{ fontSize: '0.9rem' }}>Patients can book visits with doctors for future times and manage active cancellations.</p>
          </div>
          <div className="card" style={{ padding: '1.5rem', textAlign: 'left' }}>
            <span style={{ fontSize: '2.5rem' }}>🩺</span>
            <h3 style={{ margin: '0.75rem 0 0.5rem', color: '#ffffff' }}>Specialist Directory</h3>
            <p style={{ fontSize: '0.9rem' }}>Search for certified doctors filtered by department, qualifications and clinical specialization.</p>
          </div>
          <div className="card" style={{ padding: '1.5rem', textAlign: 'left' }}>
            <span style={{ fontSize: '2.5rem' }}>🔒</span>
            <h3 style={{ margin: '0.75rem 0 0.5rem', color: '#ffffff' }}>Role-Based Access</h3>
            <p style={{ fontSize: '0.9rem' }}>Secure profiles protected by JWT backend filters for Patients, Doctors, and Hospital Admins.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      <div>
        <h1>Dashboard</h1>
        <p style={{ fontSize: '1.1rem' }}>
          Welcome back, <strong style={{ color: '#ffffff' }}>{user.firstName || user.email}</strong>. Active Role: <span className="user-tag">{user.role}</span>
        </p>
      </div>

      {/* ADMIN STATS */}
      {user.role === 'ADMIN' && (
        <div>
          <h2 style={{ fontSize: '1.35rem', marginBottom: '1rem' }}>Hospital At a Glance</h2>
          {loading ? (
            <p>Loading analytics...</p>
          ) : (
            <div className="dashboard-grid">
              <div className="stat-card">
                <div className="stat-icon">🏥</div>
                <div>
                  <div className="stat-value">{stats.departments}</div>
                  <div className="stat-label">Departments</div>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon">👥</div>
                <div>
                  <div className="stat-value">{stats.patients}</div>
                  <div className="stat-label">Patients</div>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon">🩺</div>
                <div>
                  <div className="stat-value">{stats.doctors}</div>
                  <div className="stat-label">Doctors</div>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon">📅</div>
                <div>
                  <div className="stat-value">{stats.appointments}</div>
                  <div className="stat-label">Appointments</div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* QUICK ACTIONS CONTAINER */}
      <div className="card">
        <h2 style={{ fontSize: '1.35rem', marginBottom: '1.5rem' }}>Quick Actions</h2>
        
        <div className="dashboard-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', margin: 0 }}>
          {user.role === 'ADMIN' && (
            <>
              <Link to="/departments" className="card" style={{ padding: '1.25rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderStyle: 'dashed', textDecoration: 'none' }}>
                <span style={{ fontSize: '2rem' }}>🏢</span>
                <h4 style={{ color: '#ffffff', marginTop: '0.5rem' }}>Manage Departments</h4>
              </Link>
              <Link to="/patients/register" className="card" style={{ padding: '1.25rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderStyle: 'dashed', textDecoration: 'none' }}>
                <span style={{ fontSize: '2rem' }}>👤</span>
                <h4 style={{ color: '#ffffff', marginTop: '0.5rem' }}>Register Patient</h4>
              </Link>
              <Link to="/doctors/register" className="card" style={{ padding: '1.25rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderStyle: 'dashed', textDecoration: 'none' }}>
                <span style={{ fontSize: '2rem' }}>🩺</span>
                <h4 style={{ color: '#ffffff', marginTop: '0.5rem' }}>Register Doctor</h4>
              </Link>
              <Link to="/appointments" className="card" style={{ padding: '1.25rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderStyle: 'dashed', textDecoration: 'none' }}>
                <span style={{ fontSize: '2rem' }}>📅</span>
                <h4 style={{ color: '#ffffff', marginTop: '0.5rem' }}>Clinical Logbook</h4>
              </Link>
            </>
          )}

          {user.role === 'PATIENT' && (
            <>
              <Link to="/doctors" className="card" style={{ padding: '1.25rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderStyle: 'dashed', textDecoration: 'none' }}>
                <span style={{ fontSize: '2rem' }}>🩺</span>
                <h4 style={{ color: '#ffffff', marginTop: '0.5rem' }}>Doctor Directory</h4>
              </Link>
              <Link to="/appointments/book" className="card" style={{ padding: '1.25rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderStyle: 'dashed', textDecoration: 'none' }}>
                <span style={{ fontSize: '2rem' }}>📅</span>
                <h4 style={{ color: '#ffffff', marginTop: '0.5rem' }}>Schedule Appointment</h4>
              </Link>
              <Link to="/patient-appointments" className="card" style={{ padding: '1.25rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderStyle: 'dashed', textDecoration: 'none' }}>
                <span style={{ fontSize: '2rem' }}>📋</span>
                <h4 style={{ color: '#ffffff', marginTop: '0.5rem' }}>My Appointments</h4>
              </Link>
            </>
          )}

          {user.role === 'DOCTOR' && (
            <>
              <Link to="/appointments" className="card" style={{ padding: '1.25rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderStyle: 'dashed', textDecoration: 'none' }}>
                <span style={{ fontSize: '2rem' }}>📋</span>
                <h4 style={{ color: '#ffffff', marginTop: '0.5rem' }}>My Shift Appointments</h4>
              </Link>
              <Link to="/doctors" className="card" style={{ padding: '1.25rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderStyle: 'dashed', textDecoration: 'none' }}>
                <span style={{ fontSize: '2rem' }}>👥</span>
                <h4 style={{ color: '#ffffff', marginTop: '0.5rem' }}>Doctor Directory</h4>
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default Home;
