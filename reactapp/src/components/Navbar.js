import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { notificationAPI } from '../services/api';

function Navbar({ user, onLogout }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // In-app Notifications State
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [showNotifications, setShowNotifications] = useState(false);
  const dropdownRef = useRef(null);

  const isPatient = user && user.role === 'PATIENT';

  const loadNotifications = async () => {
    if (!user || !isPatient) return;
    try {
      const list = await notificationAPI.getMyNotifications();
      setNotifications(list || []);
      const unread = (list || []).filter((n) => !n.isRead).length;
      setUnreadCount(unread);
    } catch (err) {
      console.warn('Unable to load patient notifications:', err);
    }
  };

  useEffect(() => {
    if (isPatient) {
      loadNotifications();
      const interval = setInterval(loadNotifications, 15000); // poll every 15s
      return () => clearInterval(interval);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, isPatient]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setShowNotifications(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleMarkAsRead = async (id) => {
    try {
      await notificationAPI.markAsRead(id);
      loadNotifications();
    } catch (err) {
      console.error('Error marking notification as read:', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationAPI.markAllMyAsRead();
      loadNotifications();
    } catch (err) {
      console.error('Error marking all as read:', err);
    }
  };

  const handleLogout = () => {
    onLogout();
    navigate('/login');
  };

  const isActive = (path) => (location.pathname === path ? 'active' : '');

  const getUserInitials = () => {
    if (!user) return 'CP';
    if (user.firstName) {
      return (user.firstName[0] + (user.lastName ? user.lastName[0] : '')).toUpperCase();
    }
    if (user.email) {
      return user.email.substring(0, 2).toUpperCase();
    }
    return 'CP';
  };

  const getRoleDisplayName = (role) => {
    if (!role) return '';
    if (role === 'DOCTOR') return 'Physician';
    if (role === 'PATIENT') return 'Patient';
    if (role === 'ADMIN') return 'Administrator';
    return role;
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        {/* BRAND LOGO */}
        <Link to="/" className="navbar-brand" onClick={() => setMobileMenuOpen(false)}>
          <div className="navbar-brand-icon">
            <span>⚕</span>
          </div>
          <div>
            <span>Care<strong>Portal</strong></span>
          </div>
        </Link>

        {/* NAVIGATION LINKS */}
        <div className={`navbar-links ${mobileMenuOpen ? 'mobile-open' : ''}`}>
          <Link to="/" className={`navbar-link ${isActive('/')}`} onClick={() => setMobileMenuOpen(false)}>
            <span>🏠</span> Dashboard
          </Link>

          {!user ? (
            <>
              <Link to="/login" className={`navbar-link ${isActive('/login')}`} onClick={() => setMobileMenuOpen(false)}>
                Sign In
              </Link>
              <Link
                to="/register"
                className="btn btn-primary"
                style={{ padding: '0.45rem 1rem', fontSize: '0.85rem' }}
                onClick={() => setMobileMenuOpen(false)}
              >
                Create Account
              </Link>
            </>
          ) : (
            <>
              {/* ADMIN ROLE LINKS */}
              {user.role === 'ADMIN' && (
                <>
                  <Link
                    to="/departments"
                    className={`navbar-link ${isActive('/departments')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>🏢</span> Departments
                  </Link>
                  <Link
                    to="/doctors"
                    className={`navbar-link ${isActive('/doctors')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>🩺</span> Doctors
                  </Link>
                  <Link
                    to="/patients"
                    className={`navbar-link ${isActive('/patients')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>👥</span> Patients
                  </Link>
                  <Link
                    to="/appointments"
                    className={`navbar-link ${isActive('/appointments')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>📅</span> All Appointments
                  </Link>
                  <Link
                    to="/patient-prescriptions"
                    className={`navbar-link ${isActive('/patient-prescriptions')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>💊</span> Prescriptions
                  </Link>
                </>
              )}

              {/* PATIENT ROLE LINKS */}
              {user.role === 'PATIENT' && (
                <>
                  <Link
                    to="/doctors"
                    className={`navbar-link ${isActive('/doctors')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>🩺</span> Find Doctors
                  </Link>
                  <Link
                    to="/appointments/book"
                    className={`navbar-link ${isActive('/appointments/book')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>➕</span> Book Visit
                  </Link>
                  <Link
                    to="/patient-appointments"
                    className={`navbar-link ${isActive('/patient-appointments')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>📅</span> My Appointments
                  </Link>
                  <Link
                    to="/patient-prescriptions"
                    className={`navbar-link ${isActive('/patient-prescriptions')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>💊</span> My Prescriptions
                  </Link>
                </>
              )}

              {/* DOCTOR ROLE LINKS */}
              {user.role === 'DOCTOR' && (
                <>
                  <Link
                    to="/appointments"
                    className={`navbar-link ${isActive('/appointments')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>📅</span> My Appointments
                  </Link>
                  <Link
                    to="/doctor-prescriptions"
                    className={`navbar-link ${isActive('/doctor-prescriptions')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>💊</span> Issue Rx
                  </Link>
                  <Link
                    to="/doctors"
                    className={`navbar-link ${isActive('/doctors')}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <span>👥</span> Colleagues
                  </Link>
                </>
              )}

              {/* NOTIFICATION BELL FOR PATIENTS */}
              {isPatient && (
                <div className="notification-bell-wrapper" ref={dropdownRef}>
                  <button
                    type="button"
                    className="notification-bell-btn"
                    onClick={() => setShowNotifications(!showNotifications)}
                    title="Appointment Reminders & Alerts"
                    aria-label="View notifications"
                  >
                    <span>🔔</span>
                    {unreadCount > 0 && <span className="notification-badge">{unreadCount}</span>}
                  </button>

                  {showNotifications && (
                    <div className="notification-dropdown">
                      <div className="notification-dropdown-header">
                        <div>
                          <strong>Notifications</strong>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginLeft: '0.4rem' }}>
                            ({notifications.length})
                          </span>
                        </div>
                        {unreadCount > 0 && (
                          <button
                            type="button"
                            onClick={handleMarkAllAsRead}
                            className="notification-mark-all-btn"
                          >
                            Mark all read
                          </button>
                        )}
                      </div>

                      <div className="notification-list">
                        {notifications.length === 0 ? (
                          <div className="notification-empty-state">
                            <span style={{ fontSize: '1.6rem' }}>🔕</span>
                            <div style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                              No notifications yet
                            </div>
                            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                              You will receive in-app reminders 1 hour before scheduled consultations.
                            </span>
                          </div>
                        ) : (
                          notifications.map((n) => (
                            <div
                              key={n.id}
                              className={`notification-item ${n.isRead ? 'read' : 'unread'}`}
                              onClick={() => !n.isRead && handleMarkAsRead(n.id)}
                            >
                              <div style={{ display: 'flex', gap: '0.65rem', alignItems: 'flex-start' }}>
                                <span style={{ fontSize: '1.25rem', lineHeight: 1 }}>🔔</span>
                                <div style={{ flex: 1 }}>
                                  <div className="notification-item-title">
                                    {n.title?.replace(/^🔔\s*/, '') || 'Appointment Reminder'}
                                    {!n.isRead && <span className="notification-unread-dot" />}
                                  </div>
                                  <div className="notification-item-msg">{n.message}</div>
                                  <div className="notification-item-time">
                                    {n.createdAt
                                      ? new Date(n.createdAt).toLocaleTimeString([], {
                                          hour: '2-digit',
                                          minute: '2-digit'
                                        })
                                      : 'Recently'}
                                  </div>
                                </div>
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* USER PROFILE & LOGOUT SECTION */}
              <div className="navbar-user-section">
                <div className="user-profile-chip">
                  <div className="user-avatar">{getUserInitials()}</div>
                  <div style={{ display: 'flex', flexDirection: 'column' }}>
                    <span className="user-name-text">{user.firstName || user.email.split('@')[0]}</span>
                    <span className="user-role-badge">{getRoleDisplayName(user.role)}</span>
                  </div>
                </div>

                <button
                  onClick={handleLogout}
                  className="btn btn-secondary"
                  style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
                  title="Sign out of CarePortal"
                >
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
