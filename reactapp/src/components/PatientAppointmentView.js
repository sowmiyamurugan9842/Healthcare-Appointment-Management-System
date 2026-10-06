import React, { useState, useEffect, useCallback } from 'react';
import { appointmentAPI, prescriptionAPI, waitlistAPI, patientAPI } from '../services/api';
import { Link } from 'react-router-dom';
import PrescriptionView from './PrescriptionView';

function PatientAppointmentView({ user }) {
  const [activeTab, setActiveTab] = useState('appointments'); // 'appointments' | 'waitlist'
  const [appointments, setAppointments] = useState([]);
  const [waitlistEntries, setWaitlistEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [waitlistLoading, setWaitlistLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [patientProfile, setPatientProfile] = useState(null);
  const [filterStatus, setFilterStatus] = useState('ALL');

  // Prescription modal state
  const [viewingPrescription, setViewingPrescription] = useState(null);
  const [loadingPrescriptionId, setLoadingPrescriptionId] = useState(null);

  const isPatient = user && user.role === 'PATIENT';

  const loadAppointments = useCallback(async () => {
    setLoading(true);
    setErrorMessage('');
    try {
      const data = await appointmentAPI.getMyPatientAppointments();
      setAppointments(data || []);
    } catch (err) {
      console.error('Failed to load patient appointments:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to retrieve appointments.');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadWaitlist = useCallback(async () => {
    setWaitlistLoading(true);
    try {
      const data = await waitlistAPI.getMyWaitlist();
      setWaitlistEntries(data || []);
    } catch (err) {
      console.warn('Failed to load waitlist entries:', err);
    } finally {
      setWaitlistLoading(false);
    }
  }, []);

  const refreshAll = useCallback(() => {
    loadAppointments();
    loadWaitlist();
  }, [loadAppointments, loadWaitlist]);

  useEffect(() => {
    if (isPatient) {
      patientAPI.getMe().then((profile) => {
        if (profile) setPatientProfile(profile);
      }).catch((err) => {
        console.debug('No patient profile found:', err);
      });
      loadAppointments();
      loadWaitlist();
    }
  }, [isPatient, loadAppointments, loadWaitlist]);

  const handleCancelAppointment = async (appointmentId) => {
    if (!window.confirm('Are you sure you want to cancel this scheduled appointment?')) return;

    setErrorMessage('');
    setSuccessMessage('');
    try {
      await appointmentAPI.updateStatus(appointmentId, 'CANCELLED');
      setSuccessMessage(`Appointment #${appointmentId} successfully cancelled.`);
      loadAppointments();
    } catch (err) {
      console.error('Cancellation error:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to cancel appointment';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error cancelling appointment.');
    }
  };

  const handleViewPrescription = async (appointmentId) => {
    setErrorMessage('');
    setLoadingPrescriptionId(appointmentId);
    try {
      const px = await prescriptionAPI.getByAppointment(appointmentId);
      setViewingPrescription(px);
    } catch (err) {
      console.warn('Prescription error:', err);
      if (err.response && err.response.status === 404) {
        setErrorMessage(`Prescription not yet published by the doctor for Appointment #${appointmentId}.`);
      } else {
        setErrorMessage(err.response?.data?.message || 'Unable to load prescription at this time.');
      }
    } finally {
      setLoadingPrescriptionId(null);
    }
  };

  const handleCancelWaitlist = async (entryId) => {
    if (!window.confirm('Are you sure you want to leave this waitlist?')) return;

    setErrorMessage('');
    setSuccessMessage('');
    try {
      await waitlistAPI.cancel(entryId);
      setSuccessMessage('Successfully removed from the waitlist.');
      loadWaitlist();
    } catch (err) {
      console.error('Waitlist cancel error:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to leave waitlist.';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error leaving waitlist.');
    }
  };

  const handleConfirmWaitlistOffer = async (entryId) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const confirmedAppt = await waitlistAPI.confirmSlot(entryId);
      setSuccessMessage(`🎉 Success! Your appointment #${confirmedAppt.id} with ${confirmedAppt.doctorName || 'your doctor'} has been confirmed.`);
      loadWaitlist();
      loadAppointments();
      setActiveTab('appointments');
    } catch (err) {
      console.error('Waitlist confirmation error:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to confirm waitlist appointment slot.';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error confirming slot.');
    }
  };

  const getStatusBadgeClass = (status) => {
    if (!status) return 'badge-pending';
    const s = status.toUpperCase();
    if (s === 'PENDING') return 'badge-pending';
    if (s === 'CONFIRMED' || s === 'APPROVED' || s === 'BOOKED') return 'badge-approved';
    if (s === 'CANCELLED' || s === 'REJECTED') return 'badge-rejected';
    if (s === 'COMPLETED') return 'badge-completed';
    if (s === 'EXPIRED') return 'badge-expired';
    if (s === 'NO_SHOW') return 'badge-no-show';
    if (s === 'NOTIFIED') return 'badge-notified';
    if (s === 'WAITING') return 'badge-waiting';
    return 'badge-pending';
  };

  const filteredAppointments = appointments.filter((a) => {
    if (filterStatus === 'ALL') return true;
    const s = a.status ? a.status.toUpperCase() : 'PENDING';
    if (filterStatus === 'UPCOMING') return s === 'PENDING' || s === 'APPROVED' || s === 'CONFIRMED';
    if (filterStatus === 'COMPLETED') return s === 'COMPLETED';
    if (filterStatus === 'NO_SHOW') return s === 'NO_SHOW';
    if (filterStatus === 'EXPIRED') return s === 'EXPIRED';
    if (filterStatus === 'CANCELLED') return s === 'CANCELLED' || s === 'REJECTED';
    return true;
  });

  const activeWaitlistCount = waitlistEntries.filter(
    (w) => w.status === 'WAITING' || w.status === 'NOTIFIED'
  ).length;

  if (!isPatient) {
    return (
      <div className="card" style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <h2>Access Denied</h2>
        <p style={{ marginTop: '1rem' }}>Only patient users can view personal appointment schedules.</p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem' }}>Go to Dashboard</Link>
      </div>
    );
  }

  return (
    <div>
      {/* PAGE HEADER */}
      <div className="page-header">
        <div className="page-title-group">
          <h1>My Medical Care & Schedule</h1>
          <p>
            {patientProfile?.firstName
              ? `Appointments and care plan for ${patientProfile.firstName} ${patientProfile.lastName || ''}`.trim()
              : (user?.firstName ? `Appointments and care plan for ${user.firstName} ${user.lastName || ''}`.trim() : 'Track scheduled consultations, waiting list positions, and digital medical records.')}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <button onClick={refreshAll} disabled={loading || waitlistLoading} className="btn btn-secondary">
            {loading ? 'Refreshing...' : '↻ Refresh Log'}
          </button>
          <Link to="/patient-prescriptions" className="btn btn-secondary">
            💊 My Prescriptions
          </Link>
          <Link to="/appointments/book" className="btn btn-primary">
            + Book New Visit
          </Link>
        </div>
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">{successMessage}</div>}

      {/* MAIN TABS & CONTENT */}
      <div>
        {/* TAB BAR */}
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.25rem', borderBottom: '2px solid #e2e8f0', paddingBottom: '0.5rem' }}>
          <button
            onClick={() => setActiveTab('appointments')}
            className={`btn ${activeTab === 'appointments' ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '0.5rem 1.25rem', borderRadius: '8px', fontWeight: 600 }}
          >
            📅 My Appointments ({appointments.length})
          </button>
          <button
            onClick={() => setActiveTab('waitlist')}
            className={`btn ${activeTab === 'waitlist' ? 'btn-primary' : 'btn-secondary'}`}
            style={{
              padding: '0.5rem 1.25rem',
              borderRadius: '8px',
              fontWeight: 600,
              position: 'relative'
            }}
          >
            📋 My Waitlist ({waitlistEntries.length})
            {activeWaitlistCount > 0 && (
              <span style={{
                marginLeft: '0.5rem',
                backgroundColor: '#f59e0b',
                color: '#fff',
                borderRadius: '12px',
                padding: '2px 7px',
                fontSize: '0.75rem'
              }}>
                {activeWaitlistCount} active
              </span>
            )}
          </button>
        </div>

        {/* TAB 1: APPOINTMENTS */}
        {activeTab === 'appointments' && (
          <div className="card">
            <div className="filter-bar" style={{ justifyContent: 'space-between' }}>
              <div className="filter-group">
                <label className="form-label" style={{ marginBottom: 0, marginRight: '0.35rem' }} htmlFor="filterTab">
                  Filter:
                </label>
                <select
                  id="filterTab"
                  className="form-control"
                  style={{ width: 'auto', padding: '0.45rem 0.85rem' }}
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                >
                  <option value="ALL">All Visits ({appointments.length})</option>
                  <option value="UPCOMING">Upcoming / Active</option>
                  <option value="COMPLETED">Completed Visits</option>
                  <option value="NO_SHOW">No-Show Records</option>
                  <option value="EXPIRED">Expired Visits</option>
                  <option value="CANCELLED">Cancelled / Closed</option>
                </select>
              </div>
            </div>

            {loading ? (
              <div className="empty-state-box">
                <div className="empty-state-icon">⏳</div>
                <div className="empty-state-title">Retrieving Medical Appointments...</div>
              </div>
            ) : filteredAppointments.length === 0 ? (
              <div className="empty-state-box">
                <div className="empty-state-icon">📅</div>
                <div className="empty-state-title">No Appointments Found</div>
                <div className="empty-state-desc">
                  {filterStatus !== 'ALL'
                    ? 'No records match the current filter selection.'
                    : 'No appointments are currently booked.'}
                </div>
                <Link to="/appointments/book" className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
                  Book Your First Visit
                </Link>
              </div>
              ) : (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Appt ID</th>
                        <th>Consulting Doctor</th>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Clinical Reason</th>
                        <th>Status</th>
                        <th>Fee</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredAppointments.map((appt) => {
                        const isCompleted = appt.status?.toUpperCase() === 'COMPLETED';
                        const isCancellable = appt.status === 'PENDING' || appt.status === 'CONFIRMED' || appt.status === 'APPROVED';

                        return (
                          <tr key={appt.id}>
                            <td><strong>#{appt.id}</strong></td>
                            <td>
                              <strong style={{ color: 'var(--primary-dark)' }}>
                                {appt.doctorName?.startsWith('Dr. ') ? appt.doctorName : `Dr. ${appt.doctorName}`}
                              </strong>
                            </td>
                            <td>{appt.appointmentDate}</td>
                            <td style={{ color: 'var(--primary-dark)', fontWeight: 600 }}>
                              {appt.appointmentTime?.substring(0, 5)}
                            </td>
                            <td>
                              <div style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={appt.reasonForVisit}>
                                {appt.reasonForVisit}
                              </div>
                              {appt.followUpDate && (
                                <div style={{ fontSize: '0.72rem', color: 'var(--primary-dark)', fontWeight: 600, marginTop: '0.2rem', display: 'flex', alignItems: 'center', gap: '0.2rem' }}>
                                  <span>🔄 Follow-up:</span> {appt.followUpDate} {appt.followUpTime ? `(${appt.followUpTime.substring(0, 5)})` : ''}
                                </div>
                              )}
                            </td>
                            <td>
                              <span className={`badge ${getStatusBadgeClass(appt.status)}`}>
                                {appt.status?.toUpperCase() === 'EXPIRED'
                                  ? '⏰ EXPIRED'
                                  : appt.status?.toUpperCase() === 'NO_SHOW'
                                  ? '⚠️ NO-SHOW'
                                  : appt.status}
                              </span>
                            </td>
                            <td style={{ fontWeight: 700, color: 'var(--primary-dark)' }}>
                              ${appt.consultationFee?.toFixed(2) || '0.00'}
                            </td>
                            <td>
                              <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                                {/* Cancel button if active */}
                                {isCancellable && (
                                  <button
                                    onClick={() => handleCancelAppointment(appt.id)}
                                    className="btn btn-danger"
                                    style={{ padding: '0.35rem 0.65rem', fontSize: '0.78rem' }}
                                  >
                                    Cancel
                                  </button>
                                )}

                                {/* View Prescription Button if Completed */}
                                {isCompleted && (
                                  <button
                                    onClick={() => handleViewPrescription(appt.id)}
                                    disabled={loadingPrescriptionId === appt.id}
                                    className="btn btn-accent"
                                    style={{
                                      padding: '0.35rem 0.65rem',
                                      fontSize: '0.78rem',
                                      display: 'flex',
                                      alignItems: 'center',
                                      gap: '0.25rem'
                                    }}
                                  >
                                    <span>{loadingPrescriptionId === appt.id ? '⏳' : '📄'}</span> View Rx
                                  </button>
                                )}

                                {!isCancellable && !isCompleted && (
                                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                                    {appt.status?.toUpperCase() === 'EXPIRED'
                                      ? 'Expired'
                                      : appt.status?.toUpperCase() === 'NO_SHOW'
                                      ? 'No-Show'
                                      : 'Closed'}
                                  </span>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* TAB 2: MY WAITLIST */}
          {activeTab === 'waitlist' && (
            <div className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.2rem', color: 'var(--text-main)' }}>📋 My Waitlist Entries</h3>
                  <p style={{ margin: '0.25rem 0 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    When fully-booked doctor schedules have cancellations or openings, offers appear here.
                  </p>
                </div>
                <button
                  onClick={loadWaitlist}
                  className="btn btn-secondary"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                  disabled={waitlistLoading}
                >
                  {waitlistLoading ? '⏳ Refreshing...' : '🔄 Refresh Waitlist'}
                </button>
              </div>

              {waitlistLoading && waitlistEntries.length === 0 ? (
                <div className="empty-state-box">
                  <div className="empty-state-icon">⏳</div>
                  <div className="empty-state-title">Checking Waitlist Status...</div>
                </div>
              ) : waitlistEntries.length === 0 ? (
                <div className="empty-state-box">
                  <div className="empty-state-icon">📋</div>
                  <div className="empty-state-title">No Waitlist Entries</div>
                  <div className="empty-state-desc">
                    You are not currently waiting for any doctor slots. When all slots are full for a doctor, you can join the queue.
                  </div>
                  <Link to="/appointments/book" className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
                    Browse Doctors & Book
                  </Link>
                </div>
              ) : (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Doctor</th>
                        <th>Date</th>
                        <th>Preferred Time</th>
                        <th>Queue Position</th>
                        <th>Status</th>
                        <th>Created Date</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {waitlistEntries.map((w) => {
                        const isNotified = w.status === 'NOTIFIED' || w.offerActive;
                        const isWaiting = w.status === 'WAITING';
                        const isBooked = w.status === 'BOOKED';

                        return (
                          <tr
                            key={w.id}
                            style={
                              isNotified
                                ? { backgroundColor: '#fef3c7', borderLeft: '4px solid #f59e0b' }
                                : {}
                            }
                          >
                            <td><strong>#{w.id}</strong></td>
                            <td>
                              <strong style={{ color: 'var(--primary-dark)' }}>
                                {w.doctorName?.startsWith('Dr. ') ? w.doctorName : `Dr. ${w.doctorName}`}
                              </strong>
                              {w.doctorSpecialization && (
                                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                  {w.doctorSpecialization}
                                </div>
                              )}
                            </td>
                            <td><strong>{w.appointmentDate}</strong></td>
                            <td>
                              {w.preferredTime ? (
                                <span style={{ fontWeight: 600, color: 'var(--primary-dark)' }}>
                                  {w.preferredTime.substring(0, 5)}
                                </span>
                              ) : (
                                <span style={{ fontStyle: 'italic', color: 'var(--text-muted)' }}>Any available time</span>
                              )}
                            </td>
                            <td>
                              {isWaiting ? (
                                <span
                                  style={{
                                    backgroundColor: '#e0f2fe',
                                    color: '#0369a1',
                                    padding: '0.2rem 0.6rem',
                                    borderRadius: '12px',
                                    fontWeight: 700,
                                    fontSize: '0.82rem'
                                  }}
                                >
                                  #{w.queuePosition || 1}
                                </span>
                              ) : isNotified ? (
                                <span
                                  style={{
                                    backgroundColor: '#dcfce7',
                                    color: '#15803d',
                                    padding: '0.2rem 0.6rem',
                                    borderRadius: '12px',
                                    fontWeight: 700,
                                    fontSize: '0.82rem'
                                  }}
                                >
                                  🎯 Next in Line
                                </span>
                              ) : (
                                <span style={{ color: 'var(--text-muted)' }}>-</span>
                              )}
                            </td>
                            <td>
                              <span className={`badge ${getStatusBadgeClass(w.status)}`}>
                                {w.status === 'NOTIFIED' ? '🔔 OFFER AVAILABLE' : w.status}
                              </span>
                              {isNotified && w.offeredTime && (
                                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#b45309', marginTop: '0.2rem' }}>
                                  Slot: {w.offeredTime.substring(0, 5)}
                                </div>
                              )}
                            </td>
                            <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                              {w.createdAt ? new Date(w.createdAt).toLocaleDateString() : '-'}
                            </td>
                            <td>
                              <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center', flexWrap: 'wrap' }}>
                                {/* IF NOTIFIED: Confirm Slot Button */}
                                {isNotified && (
                                  <button
                                    onClick={() => handleConfirmWaitlistOffer(w.id)}
                                    className="btn btn-primary"
                                    style={{
                                      padding: '0.35rem 0.75rem',
                                      fontSize: '0.8rem',
                                      backgroundColor: '#16a34a',
                                      borderColor: '#16a34a',
                                      fontWeight: 700
                                    }}
                                  >
                                    ✅ Confirm Slot
                                  </button>
                                )}

                                {/* Cancel / Leave Waitlist button for active/notified */}
                                {(isWaiting || isNotified) && (
                                  <button
                                    onClick={() => handleCancelWaitlist(w.id)}
                                    className="btn btn-danger"
                                    style={{ padding: '0.35rem 0.65rem', fontSize: '0.78rem' }}
                                  >
                                    Leave Waitlist
                                  </button>
                                )}

                                {isBooked && (
                                  <span style={{ fontSize: '0.8rem', color: '#16a34a', fontWeight: 600 }}>
                                    ✓ Booked
                                  </span>
                                )}

                                {!isWaiting && !isNotified && !isBooked && (
                                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                                    {w.status}
                                  </span>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>

      {/* PRESCRIPTION MODAL (USES REACT PORTAL) */}
      {viewingPrescription && (
        <PrescriptionView
          prescription={viewingPrescription}
          onClose={() => setViewingPrescription(null)}
        />
      )}
    </div>
  );
}

export default PatientAppointmentView;
