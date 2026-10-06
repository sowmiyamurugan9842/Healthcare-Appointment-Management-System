import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { departmentAPI, patientAPI, doctorAPI, appointmentAPI, prescriptionAPI, notificationAPI } from '../services/api';

function Home({ user }) {
  const [adminStats, setAdminStats] = useState({
    departments: 0,
    patients: 0,
    doctors: 0,
    appointments: 0
  });

  const [doctorStats, setDoctorStats] = useState({
    total: 0,
    pending: 0,
    completed: 0,
    prescriptions: 0
  });

  const [patientStats, setPatientStats] = useState({
    total: 0,
    upcoming: 0,
    completed: 0,
    prescriptions: 0
  });

  const [patientReminders, setPatientReminders] = useState([]);
  const [loading, setLoading] = useState(false);

  // Load stats depending on the role
  useEffect(() => {
    if (!user) return;

    const loadRoleData = async () => {
      setLoading(true);
      try {
        if (user.role === 'ADMIN') {
          const [depts, pats, docs, appts] = await Promise.all([
            departmentAPI.getAll().catch(() => []),
            patientAPI.getAll().catch(() => []),
            doctorAPI.getAll().catch(() => []),
            appointmentAPI.getAll().catch(() => [])
          ]);

          setAdminStats({
            departments: depts?.length || 0,
            patients: pats?.length || 0,
            doctors: docs?.length || 0,
            appointments: appts?.length || 0
          });
        } else if (user.role === 'DOCTOR') {
          const appts = await appointmentAPI.getMyDoctorAppointments().catch(() => []);
          const total = appts?.length || 0;
          const pending = appts?.filter((a) => a.status === 'PENDING').length || 0;
          const completed = appts?.filter((a) => a.status === 'COMPLETED').length || 0;
          setDoctorStats({
            total,
            pending,
            completed,
            prescriptions: completed // doctor can issue rx for completed
          });
        } else if (user.role === 'PATIENT') {
          const [appts, rxs, notifs] = await Promise.all([
            appointmentAPI.getMyPatientAppointments().catch(() => []),
            prescriptionAPI.getMyPatientPrescriptions().catch(() => []),
            notificationAPI.getMyNotifications().catch(() => [])
          ]);
          const total = appts?.length || 0;
          const upcoming = appts?.filter((a) => a.status === 'CONFIRMED' || a.status === 'APPROVED' || a.status === 'PENDING').length || 0;
          const completed = appts?.filter((a) => a.status === 'COMPLETED').length || 0;
          setPatientStats({
            total,
            upcoming,
            completed,
            prescriptions: rxs?.length || 0
          });
          const unread = (notifs || []).filter((n) => !n.isRead);
          setPatientReminders(unread);
        }
      } catch (err) {
        console.error('Error fetching dashboard statistics:', err);
      } finally {
        setLoading(false);
      }
    };

    loadRoleData();
  }, [user]);

  const handleDismissReminder = async (id) => {
    try {
      await notificationAPI.markAsRead(id);
      setPatientReminders((prev) => prev.filter((r) => r.id !== id));
    } catch (err) {
      console.error('Error dismissing reminder:', err);
    }
  };

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  };

  const todayFormatted = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });

  // 1. PUBLIC LANDING PAGE (LOGGED OUT)
  if (!user) {
    return (
      <div>
        {/* HERO SECTION */}
        <div
          style={{
            background: 'linear-gradient(135deg, var(--primary-dark) 0%, #0A6973 60%, var(--primary) 100%)',
            borderRadius: 'var(--radius-xl)',
            padding: '4rem 3rem',
            color: '#FFFFFF',
            textAlign: 'center',
            boxShadow: 'var(--shadow-lg)',
            marginBottom: '3.5rem'
          }}
        >
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              backgroundColor: 'rgba(255, 255, 255, 0.15)',
              padding: '0.35rem 1rem',
              borderRadius: 'var(--radius-full)',
              fontSize: '0.85rem',
              fontWeight: 600,
              marginBottom: '1.5rem',
              border: '1px solid rgba(255, 255, 255, 0.2)'
            }}
          >
            <span>⚕</span> Intelligent Clinical Management & Telehealth
          </div>

          <h1 style={{ fontSize: '2.75rem', fontWeight: 800, color: '#FFFFFF', maxWidth: '850px', margin: '0 auto 1.25rem', lineHeight: 1.2 }}>
            Connected Healthcare for Doctors, Patients & Clinics
          </h1>

          <p style={{ fontSize: '1.15rem', color: 'rgba(255, 255, 255, 0.9)', maxWidth: '680px', margin: '0 auto 2.5rem', lineHeight: 1.6 }}>
            Streamline patient appointments, consult with verified medical specialists, and manage digital online prescriptions in one unified platform.
          </p>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link
              to="/login"
              className="btn btn-primary"
              style={{
                backgroundColor: '#FFFFFF',
                color: 'var(--primary-dark)',
                padding: '0.85rem 2rem',
                fontSize: '1rem',
                fontWeight: 700,
                boxShadow: '0 4px 14px rgba(0,0,0,0.15)'
              }}
            >
              Sign In to Portal
            </Link>
            <Link
              to="/register"
              className="btn btn-secondary"
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                color: '#FFFFFF',
                borderColor: 'rgba(255, 255, 255, 0.3)',
                padding: '0.85rem 2rem',
                fontSize: '1rem',
                fontWeight: 600
              }}
            >
              Create Free Account
            </Link>
          </div>
        </div>

        {/* 3 CORE PILLARS GRID */}
        <div className="section-header" style={{ textAlign: 'center', display: 'block', marginBottom: '2.5rem' }}>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '0.5rem' }}>Comprehensive Medical Capabilities</h2>
          <p>Designed to deliver a seamless healthcare experience.</p>
        </div>

        <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem' }}>
          <div className="card" style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div className="stat-icon-wrapper" style={{ width: '56px', height: '56px', fontSize: '1.6rem' }}>
              <span>📅</span>
            </div>
            <h3>Smart Scheduling</h3>
            <p>
              Book consultations with doctors, specify symptoms, and receive instant status updates from clinical staff.
            </p>
          </div>

          <div className="card" style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div className="stat-icon-wrapper" style={{ width: '56px', height: '56px', fontSize: '1.6rem' }}>
              <span>💊</span>
            </div>
            <h3>Digital Prescriptions</h3>
            <p>
              Physicians issue structured digital prescriptions with dosages and follow-up care plans available to patients in real-time.
            </p>
          </div>

          <div className="card" style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div className="stat-icon-wrapper" style={{ width: '56px', height: '56px', fontSize: '1.6rem' }}>
              <span>🩺</span>
            </div>
            <h3>Specialist Directory</h3>
            <p>
              Explore qualified physicians filtered by medical specialization, consultation fees, and working clinic hours.
            </p>
          </div>
        </div>
      </div>
    );
  }

  // 2. DOCTOR DASHBOARD
  if (user.role === 'DOCTOR') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        {/* WELCOME BANNER */}
        <div className="dashboard-welcome-banner">
          <div>
            <h1>{getGreeting()}, Dr. {user.firstName || user.email.split('@')[0]}</h1>
            <p>📅 {todayFormatted} • Ready for clinical consultations</p>
          </div>
          <Link to="/appointments" className="btn btn-primary" style={{ backgroundColor: '#FFFFFF', color: 'var(--primary-dark)', fontWeight: 700 }}>
            View Today's Schedule →
          </Link>
        </div>

        {/* STATS GRID */}
        <div>
          <div className="section-header">
            <h2>Clinical Consultation Summary</h2>
          </div>

          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon-wrapper">📅</div>
              <div>
                <div className="stat-number">{doctorStats.total}</div>
                <div className="stat-title">Total Appointments</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--warning-bg)', borderColor: 'var(--warning-border)', color: 'var(--warning-text)' }}>
                ⏳
              </div>
              <div>
                <div className="stat-number">{doctorStats.pending}</div>
                <div className="stat-title">Pending Confirmation</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--success-bg)', borderColor: 'var(--success-border)', color: 'var(--success-text)' }}>
                ✓
              </div>
              <div>
                <div className="stat-number">{doctorStats.completed}</div>
                <div className="stat-title">Completed Visits</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper">💊</div>
              <div>
                <div className="stat-number">{doctorStats.prescriptions}</div>
                <div className="stat-title">Eligible for Prescription</div>
              </div>
            </div>
          </div>
        </div>

        {/* QUICK ACTIONS */}
        <div className="card">
          <div className="section-header">
            <h2>Quick Actions</h2>
          </div>

          <div className="quick-actions-grid">
            <Link to="/appointments" className="quick-action-card quick-action-appointments">
              <div className="quick-action-icon-wrapper appt-icon">
                <span className="quick-action-icon">📋</span>
              </div>
              <span className="quick-action-title">My Appointments</span>
              <span className="quick-action-desc">Review schedules, appointment status & visits</span>
            </Link>

            <Link to="/doctor-prescriptions" className="quick-action-card quick-action-prescriptions">
              <div className="quick-action-icon-wrapper rx-icon">
                <span className="quick-action-icon">💊</span>
              </div>
              <span className="quick-action-title">Issue Online Rx</span>
              <span className="quick-action-desc">Create prescriptions for completed consultations</span>
            </Link>

            <Link to="/doctors" className="quick-action-card quick-action-directory">
              <div className="quick-action-icon-wrapper dir-icon">
                <span className="quick-action-icon">🩺</span>
              </div>
              <span className="quick-action-title">Colleague Directory</span>
              <span className="quick-action-desc">Explore medical staff profiles</span>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // 3. PATIENT DASHBOARD
  if (user.role === 'PATIENT') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        {/* WELCOME BANNER */}
        <div className="dashboard-welcome-banner">
          <div>
            <h1>{getGreeting()}, {user.firstName || user.email.split('@')[0]} 👋</h1>
            <p>📅 {todayFormatted} • Welcome to your personal health portal</p>
          </div>
          <Link to="/appointments/book" className="btn btn-primary" style={{ backgroundColor: '#FFFFFF', color: 'var(--primary-dark)', fontWeight: 700 }}>
            + Book Appointment
          </Link>
        </div>

        {/* APPOINTMENT REMINDER BANNER */}
        {patientReminders && patientReminders.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
            {patientReminders.map((reminder) => (
              <div key={reminder.id} className="reminder-banner-card">
                <div className="reminder-banner-left">
                  <div className="reminder-banner-icon">🔔</div>
                  <div>
                    <div className="reminder-banner-title">
                      {reminder.title?.startsWith('🔔') ? reminder.title : `🔔 ${reminder.title || 'Appointment Reminder'}`}
                    </div>
                    <div className="reminder-banner-msg">
                      {reminder.message}
                    </div>
                    <div className="reminder-banner-submsg">
                      Please be available on time for your scheduled consultation.
                    </div>
                  </div>
                </div>

                <div className="reminder-banner-actions">
                  <Link
                    to="/patient-appointments"
                    className="btn btn-primary"
                    style={{ padding: '0.4rem 0.85rem', fontSize: '0.82rem' }}
                  >
                    View Appointment
                  </Link>
                  <button
                    type="button"
                    onClick={() => handleDismissReminder(reminder.id)}
                    className="btn btn-secondary"
                    style={{ padding: '0.4rem 0.75rem', fontSize: '0.82rem' }}
                    title="Dismiss reminder"
                  >
                    Dismiss
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* STATS GRID */}
        <div>
          <div className="section-header">
            <h2>Your Health Dashboard</h2>
          </div>

          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon-wrapper">📅</div>
              <div>
                <div className="stat-number">{patientStats.upcoming}</div>
                <div className="stat-title">Upcoming Visits</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--success-bg)', borderColor: 'var(--success-border)', color: 'var(--success-text)' }}>
                ✓
              </div>
              <div>
                <div className="stat-number">{patientStats.completed}</div>
                <div className="stat-title">Completed Visits</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper">💊</div>
              <div>
                <div className="stat-number">{patientStats.prescriptions}</div>
                <div className="stat-title">Digital Prescriptions</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper">🩺</div>
              <div>
                <div className="stat-number">{patientStats.total}</div>
                <div className="stat-title">Total Consultations</div>
              </div>
            </div>
          </div>
        </div>

        {/* QUICK ACTIONS */}
        <div className="card">
          <div className="section-header">
            <h2>Patient Services</h2>
          </div>

          <div className="quick-actions-grid">
            <Link to="/doctors" className="quick-action-card">
              <div className="quick-action-icon-wrapper dir-icon">
                <span className="quick-action-icon">🩺</span>
              </div>
              <span className="quick-action-title">Find a Specialist</span>
              <span className="quick-action-desc">Search doctors by department</span>
            </Link>

            <Link to="/appointments/book" className="quick-action-card">
              <div className="quick-action-icon-wrapper appt-icon">
                <span className="quick-action-icon">➕</span>
              </div>
              <span className="quick-action-title">Schedule Visit</span>
              <span className="quick-action-desc">Book your next consultation</span>
            </Link>

            <Link to="/patient-appointments" className="quick-action-card">
              <div className="quick-action-icon-wrapper appt-icon">
                <span className="quick-action-icon">📋</span>
              </div>
              <span className="quick-action-title">My Appointments</span>
              <span className="quick-action-desc">View dates, times and status</span>
            </Link>

            <Link to="/patient-prescriptions" className="quick-action-card">
              <div className="quick-action-icon-wrapper rx-icon">
                <span className="quick-action-icon">💊</span>
              </div>
              <span className="quick-action-title">My Prescriptions</span>
              <span className="quick-action-desc">Access & print digital Rx sheets</span>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // 4. ADMIN DASHBOARD
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* WELCOME BANNER */}
      <div className="dashboard-welcome-banner">
        <div>
          <h1>Hospital Administration Portal</h1>
          <p>📅 {todayFormatted} • Operational overview & registry controls</p>
        </div>
        <Link to="/appointments" className="btn btn-primary" style={{ backgroundColor: '#FFFFFF', color: 'var(--primary-dark)', fontWeight: 700 }}>
          View All Appointments →
        </Link>
      </div>

      {/* STATS GRID */}
      <div>
        <div className="section-header">
          <h2>Hospital At a Glance</h2>
        </div>

        {loading ? (
          <p>Updating hospital statistics...</p>
        ) : (
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon-wrapper">🏢</div>
              <div>
                <div className="stat-number">{adminStats.departments}</div>
                <div className="stat-title">Medical Departments</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper">👥</div>
              <div>
                <div className="stat-number">{adminStats.patients}</div>
                <div className="stat-title">Registered Patients</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper">🩺</div>
              <div>
                <div className="stat-number">{adminStats.doctors}</div>
                <div className="stat-title">Certified Doctors</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper">📅</div>
              <div>
                <div className="stat-number">{adminStats.appointments}</div>
                <div className="stat-title">Total Appointments</div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* QUICK ACTIONS */}
      <div className="card">
        <div className="section-header">
          <h2>Administrative Controls</h2>
        </div>

        <div className="quick-actions-grid">
          <Link to="/departments" className="quick-action-card">
            <div className="quick-action-icon-wrapper dir-icon">
              <span className="quick-action-icon">🏢</span>
            </div>
            <span className="quick-action-title">Departments</span>
            <span className="quick-action-desc">Create & manage clinical units</span>
          </Link>

          <Link to="/doctors/register" className="quick-action-card">
            <div className="quick-action-icon-wrapper dir-icon">
              <span className="quick-action-icon">🩺</span>
            </div>
            <span className="quick-action-title">Register Doctor</span>
            <span className="quick-action-desc">Create professional doctor profile</span>
          </Link>

          <Link to="/patients/register" className="quick-action-card">
            <div className="quick-action-icon-wrapper appt-icon">
              <span className="quick-action-icon">👤</span>
            </div>
            <span className="quick-action-title">Register Patient</span>
            <span className="quick-action-desc">Create patient medical record</span>
          </Link>

          <Link to="/patients" className="quick-action-card">
            <div className="quick-action-icon-wrapper appt-icon">
              <span className="quick-action-icon">👥</span>
            </div>
            <span className="quick-action-title">Patient Directory</span>
            <span className="quick-action-desc">View registered patient files</span>
          </Link>

          <Link to="/appointments" className="quick-action-card">
            <div className="quick-action-icon-wrapper appt-icon">
              <span className="quick-action-icon">📅</span>
            </div>
            <span className="quick-action-title">Clinical Logbook</span>
            <span className="quick-action-desc">Monitor all schedules & statuses</span>
          </Link>

          <Link to="/patient-prescriptions" className="quick-action-card">
            <div className="quick-action-icon-wrapper rx-icon">
              <span className="quick-action-icon">💊</span>
            </div>
            <span className="quick-action-title">All Prescriptions</span>
            <span className="quick-action-desc">Audit published medical Rx records</span>
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Home;
