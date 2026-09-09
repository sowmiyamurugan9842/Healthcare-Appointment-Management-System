import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';

function Navbar({ user, onLogout }) {
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    onLogout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path ? 'active' : '';

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-brand">
          ⚕️ <span>CarePortal</span>
        </Link>
        <div className="navbar-links">
          <Link to="/" className={`navbar-link ${isActive('/')}`}>
            Home
          </Link>
          
          {!user ? (
            <>
              <Link to="/login" className={`navbar-link ${isActive('/login')}`}>
                Login
              </Link>
              <Link to="/register" className={`navbar-link ${isActive('/register')}`}>
                Register
              </Link>
            </>
          ) : (
            <>
              {/* ADMIN LINKS */}
              {user.role === 'ADMIN' && (
                <>
                  <Link to="/departments" className={`navbar-link ${isActive('/departments')}`}>
                    Departments
                  </Link>
                  <Link to="/patients" className={`navbar-link ${isActive('/patients')}`}>
                    Patients
                  </Link>
                  <Link to="/doctors" className={`navbar-link ${isActive('/doctors')}`}>
                    Doctors
                  </Link>
                  <Link to="/appointments" className={`navbar-link ${isActive('/appointments')}`}>
                    All Appointments
                  </Link>
                </>
              )}

              {/* PATIENT LINKS */}
              {user.role === 'PATIENT' && (
                <>
                  <Link to="/doctors" className={`navbar-link ${isActive('/doctors')}`}>
                    View Doctors
                  </Link>
                  <Link to="/appointments/book" className={`navbar-link ${isActive('/appointments/book')}`}>
                    Book Appointment
                  </Link>
                  <Link to="/patient-appointments" className={`navbar-link ${isActive('/patient-appointments')}`}>
                    My Appointments
                  </Link>
                </>
              )}

              {/* DOCTOR LINKS */}
              {user.role === 'DOCTOR' && (
                <>
                  <Link to="/appointments" className={`navbar-link ${isActive('/appointments')}`}>
                    My Appointments
                  </Link>
                </>
              )}

              <div className="navbar-user">
                <span>{user.firstName || user.email}</span>
                <span className="user-tag">{user.role}</span>
                <button onClick={handleLogout} className="btn btn-secondary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.85rem' }}>
                  Logout
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
