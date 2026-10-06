import React, { useState, useEffect, useCallback } from 'react';
import { appointmentAPI, prescriptionAPI, doctorAPI } from '../services/api';
import { Link } from 'react-router-dom';
import PrescriptionForm from './PrescriptionForm';
import PrescriptionView from './PrescriptionView';
import AppointmentDetailsModal from './AppointmentDetailsModal';

function AppointmentList({ user }) {
  const [appointments, setAppointments] = useState([]);
  const [doctorProfile, setDoctorProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [selectedStatusFilter, setSelectedStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Appointment Details Modal state
  const [selectedAppointmentDetails, setSelectedAppointmentDetails] = useState(null);

  // Prescription Modals state
  const [prescriptionFormAppt, setPrescriptionFormAppt] = useState(null);
  const [viewingPrescription, setViewingPrescription] = useState(null);
  const [loadingPrescriptionId, setLoadingPrescriptionId] = useState(null);

  const isAdmin = user && user.role === 'ADMIN';
  const isDoctor = user && user.role === 'DOCTOR';

  const loadAppointments = useCallback(async () => {
    setLoading(true);
    setErrorMessage('');
    try {
      let data = [];
      if (isAdmin) {
        data = await appointmentAPI.getAll();
      } else if (isDoctor) {
        const [profile, appts] = await Promise.all([
          doctorAPI.getMe().catch((err) => {
            console.warn('Could not fetch doctor profile details:', err);
            return null;
          }),
          appointmentAPI.getMyDoctorAppointments()
        ]);
        if (profile) {
          setDoctorProfile(profile);
        }
        data = appts;
      }
      setAppointments(data || []);
    } catch (err) {
      console.error('Failed to load appointments:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to retrieve appointments. Check authentication.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin, isDoctor]);

  useEffect(() => {
    if (isAdmin || isDoctor) {
      loadAppointments();
    }
  }, [isAdmin, isDoctor, loadAppointments]);

  const handleStatusChange = async (appointmentId, newStatus) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      await appointmentAPI.updateStatus(appointmentId, newStatus);
      setSuccessMessage(`Appointment #${appointmentId} status successfully updated to ${newStatus}!`);
      loadAppointments();
    } catch (err) {
      console.error('Status change error:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to update status';
      setErrorMessage(typeof msg === 'string' ? msg : `Error updating appointment status.`);
    }
  };

  const handleMarkNoShow = async (appointmentId) => {
    const confirmed = window.confirm('Are you sure this patient did not attend the scheduled appointment?');
    if (!confirmed) return;

    setErrorMessage('');
    setSuccessMessage('');
    try {
      await appointmentAPI.markNoShow(appointmentId, 'Patient did not attend scheduled consultation');
      setSuccessMessage(`Appointment #${appointmentId} has been successfully marked as NO-SHOW.`);
      loadAppointments();
    } catch (err) {
      console.error('Mark no-show error:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to mark appointment as no-show';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error marking appointment as no-show.');
    }
  };

  const pendingAppointments = appointments.filter((a) => a.status?.toUpperCase() === 'PENDING');
  const pendingCount = pendingAppointments.length;

  const handleConfirmAll = async () => {
    if (pendingCount === 0) {
      setErrorMessage('No pending appointments to confirm.');
      return;
    }

    const confirmed = window.confirm(
      `Are you sure you want to confirm all ${pendingCount} pending appointment(s)?`
    );
    if (!confirmed) return;

    setLoading(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      try {
        await appointmentAPI.confirmAllMy();
      } catch (apiErr) {
        console.warn('Batch confirm-all endpoint fallback to individual calls:', apiErr);
        await Promise.all(
          pendingAppointments.map((appt) => appointmentAPI.updateStatus(appt.id, 'APPROVED'))
        );
      }
      setSuccessMessage('All pending appointments have been confirmed.');
      await loadAppointments();
    } catch (err) {
      console.error('Error confirming all appointments:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to confirm all pending appointments.';
      setErrorMessage(typeof msg === 'string' ? msg : 'Failed to confirm all pending appointments.');
    } finally {
      setLoading(false);
    }
  };

  const handleViewPrescription = async (appt) => {
    setErrorMessage('');
    setLoadingPrescriptionId(appt.id);
    try {
      const px = await prescriptionAPI.getByAppointment(appt.id);
      setViewingPrescription(px);
    } catch (err) {
      console.warn('Prescription not found for appointment:', appt.id);
      if (err.response && err.response.status === 404) {
        if (isDoctor || isAdmin) {
          setPrescriptionFormAppt(appt);
        } else {
          setErrorMessage(`No prescription has been issued yet for Appointment #${appt.id}.`);
        }
      } else {
        setErrorMessage(err.response?.data?.message || 'Unable to load prescription.');
      }
    } finally {
      setLoadingPrescriptionId(null);
    }
  };

  const handlePrescriptionSaved = (savedPx) => {
    setPrescriptionFormAppt(null);
    setSuccessMessage(`Prescription #PX-${savedPx.id} successfully created and saved for Appointment #${savedPx.appointmentId}!`);
    setViewingPrescription(savedPx);
  };

  const getStatusBadgeClass = (status) => {
    if (!status) return 'badge-pending';
    const s = status.toUpperCase();
    if (s === 'PENDING') return 'badge-pending';
    if (s === 'CONFIRMED' || s === 'APPROVED') return 'badge-approved';
    if (s === 'CANCELLED' || s === 'REJECTED') return 'badge-rejected';
    if (s === 'COMPLETED') return 'badge-completed';
    if (s === 'EXPIRED') return 'badge-expired';
    if (s === 'NO_SHOW') return 'badge-no-show';
    return 'badge-pending';
  };

  // Filter appointments
  const filteredAppointments = appointments.filter((a) => {
    const s = a.status ? a.status.toUpperCase() : 'PENDING';
    const matchesStatus =
      selectedStatusFilter === 'ALL'
        ? true
        : selectedStatusFilter === 'APPROVED'
        ? s === 'APPROVED' || s === 'CONFIRMED'
        : selectedStatusFilter === 'REJECTED'
        ? s === 'REJECTED' || s === 'CANCELLED'
        : selectedStatusFilter === 'NO_SHOW'
        ? s === 'NO_SHOW'
        : s === selectedStatusFilter;

    if (!matchesStatus) return false;

    if (searchQuery.trim() !== '') {
      const q = searchQuery.toLowerCase();
      const pName = (a.patientName || '').toLowerCase();
      const dName = (a.doctorName || '').toLowerCase();
      const reason = (a.reasonForVisit || '').toLowerCase();
      const idStr = String(a.id);
      return pName.includes(q) || dName.includes(q) || reason.includes(q) || idStr.includes(q);
    }

    return true;
  });

  if (!isAdmin && !isDoctor) {
    return (
      <div className="card" style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <h2>Access Denied</h2>
        <p style={{ marginTop: '1rem' }}>Only administrators and medical doctors can view the full appointment logs.</p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem' }}>Go to Dashboard</Link>
      </div>
    );
  }

  return (
    <div>
      {/* PAGE HEADER */}
      <div className="page-header">
        <div className="page-title-group">
          <h1>Clinical Appointments Log</h1>
          <p>
            {isAdmin 
              ? 'Administrator View: Comprehensive clinical schedule and consultation management.'
              : `Appointments scheduled for Dr. ${
                  doctorProfile?.fullName
                    ? doctorProfile.fullName.replace(/^Dr\.\s*/i, '')
                    : (doctorProfile?.user?.firstName
                        ? `${doctorProfile.user.firstName} ${doctorProfile.user.lastName}`
                        : (user?.firstName ? `${user.firstName} ${user.lastName || ''}`.trim() : (user?.email?.split('@')[0] || 'Doctor')))
                }`}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button onClick={loadAppointments} disabled={loading} className="btn btn-secondary">
            {loading ? 'Refreshing...' : '↻ Refresh Log'}
          </button>
        </div>
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      {/* APPOINTMENTS TABLE & FILTERS */}
      <div className="card">
        {/* TOOLBAR */}
        <div className="filter-bar" style={{ justifyContent: 'space-between' }}>
          <div className="filter-group" style={{ flex: '1 1 280px' }}>
            <input
              type="text"
              className="form-control"
              placeholder="Search patient, doctor, reason, or Appt ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <div className="filter-group">
              <label className="form-label" style={{ marginBottom: 0, marginRight: '0.25rem' }} htmlFor="statusFilter">
                Status:
              </label>
              <select
                id="statusFilter"
                className="form-control"
                style={{ width: 'auto', padding: '0.5rem 0.85rem' }}
                value={selectedStatusFilter}
                onChange={(e) => setSelectedStatusFilter(e.target.value)}
              >
                <option value="ALL">All Statuses ({appointments.length})</option>
                <option value="PENDING">Pending</option>
                <option value="APPROVED">Approved / Confirmed</option>
                <option value="COMPLETED">Completed</option>
                <option value="NO_SHOW">No-Show</option>
                <option value="EXPIRED">Expired</option>
                <option value="REJECTED">Cancelled / Rejected</option>
              </select>
            </div>

            {isDoctor && (
              <button
                type="button"
                onClick={handleConfirmAll}
                disabled={loading || pendingCount === 0}
                className="btn btn-success"
                style={{
                  padding: '0.45rem 0.85rem',
                  fontSize: '0.82rem',
                  fontWeight: 600,
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  opacity: pendingCount === 0 ? 0.6 : 1,
                  cursor: pendingCount === 0 ? 'not-allowed' : 'pointer'
                }}
                title={pendingCount === 0 ? 'No pending appointments to confirm.' : `Confirm all ${pendingCount} pending appointment(s)`}
              >
                <span>✅</span> Confirm All {pendingCount > 0 ? `(${pendingCount})` : ''}
              </button>
            )}
          </div>
        </div>

          {/* TABLE CONTENT */}
          {loading ? (
            <div className="empty-state-box">
              <div className="empty-state-icon">⏳</div>
              <div className="empty-state-title">Loading Appointments Log...</div>
            </div>
          ) : filteredAppointments.length === 0 ? (
            <div className="empty-state-box">
              <div className="empty-state-icon">📅</div>
              <div className="empty-state-title">No Appointments Found</div>
              <div className="empty-state-desc">
                {searchQuery || selectedStatusFilter !== 'ALL'
                  ? 'No records match the selected filter criteria.'
                  : 'There are currently no appointments registered in the system.'}
              </div>
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Appt ID</th>
                    <th>Patient Name</th>
                    <th>Consulting Doctor</th>
                    <th>Date</th>
                    <th>Time</th>
                    <th>Clinical Reason</th>
                    <th>Status</th>
                    <th>Fee</th>
                    <th>Actions & Prescription</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAppointments.map((appt) => {
                    const isCompleted = appt.status?.toUpperCase() === 'COMPLETED';
                    const isPending = appt.status?.toUpperCase() === 'PENDING';
                    const isConfirmed = appt.status?.toUpperCase() === 'CONFIRMED' || appt.status?.toUpperCase() === 'APPROVED';

                    return (
                      <tr key={appt.id}>
                        <td>
                          {isCompleted ? (
                            <button
                              type="button"
                              onClick={() => setSelectedAppointmentDetails(appt)}
                              style={{
                                background: 'none',
                                border: 'none',
                                padding: 0,
                                color: 'var(--primary-dark)',
                                fontWeight: '700',
                                cursor: 'pointer',
                                textDecoration: 'underline',
                                fontFamily: 'inherit',
                                fontSize: 'inherit'
                              }}
                              title="Click to view appointment details"
                            >
                              #{appt.id}
                            </button>
                          ) : (
                            <strong>#{appt.id}</strong>
                          )}
                        </td>
                        <td>
                          <div style={{ fontWeight: '700', color: 'var(--text-primary)' }}>
                            {appt.patientName}
                          </div>
                          {isAdmin && appt.patientId && (
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                              Patient ID: #{appt.patientId}
                            </span>
                          )}
                        </td>
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
                          <div
                            style={{ maxWidth: '220px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                            title={appt.reasonForVisit}
                          >
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
                          <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap', alignItems: 'center' }}>
                            {/* APPROVE BUTTON */}
                            {isPending && (
                              <button
                                onClick={() => handleStatusChange(appt.id, 'APPROVED')}
                                className="btn btn-success"
                                style={{ padding: '0.3rem 0.55rem', fontSize: '0.78rem' }}
                                title="Approve & Confirm Appointment"
                              >
                                Approve
                              </button>
                            )}

                            {/* COMPLETE BUTTON */}
                            {(isConfirmed || isPending) && (
                              <button
                                onClick={() => handleStatusChange(appt.id, 'COMPLETED')}
                                className="btn btn-primary"
                                style={{ padding: '0.3rem 0.55rem', fontSize: '0.78rem' }}
                                title="Mark Consultation as Completed"
                              >
                                Complete
                              </button>
                            )}

                            {/* NO-SHOW BUTTON */}
                            {isConfirmed && (
                              <button
                                onClick={() => handleMarkNoShow(appt.id)}
                                className="btn btn-warning"
                                style={{
                                  padding: '0.3rem 0.55rem',
                                  fontSize: '0.78rem',
                                  backgroundColor: '#f59e0b',
                                  borderColor: '#f59e0b',
                                  color: '#ffffff',
                                  fontWeight: 600
                                }}
                                title="Mark Patient as No-Show (Did Not Attend)"
                              >
                                No-Show
                              </button>
                            )}

                            {/* REJECT BUTTON */}
                            {(isPending || isConfirmed) && (
                              <button
                                onClick={() => handleStatusChange(appt.id, 'REJECTED')}
                                className="btn btn-danger"
                                style={{ padding: '0.3rem 0.55rem', fontSize: '0.78rem' }}
                                title="Reject or Cancel Appointment"
                              >
                                Reject
                              </button>
                            )}

                            {/* ACTIONS FOR COMPLETED APPOINTMENTS */}
                            {isCompleted && (
                              <>
                                <button
                                  type="button"
                                  onClick={() => setSelectedAppointmentDetails(appt)}
                                  className="btn btn-secondary"
                                  style={{
                                    padding: '0.3rem 0.65rem',
                                    fontSize: '0.78rem',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '0.25rem'
                                  }}
                                  title="View Consultation & Appointment Details"
                                >
                                  <span>📋</span> Details
                                </button>

                                <button
                                  onClick={() => setPrescriptionFormAppt(appt)}
                                  className="btn btn-accent"
                                  style={{
                                    padding: '0.3rem 0.65rem',
                                    fontSize: '0.78rem',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '0.25rem'
                                  }}
                                  title="Create or Issue Online Prescription"
                                >
                                  <span>📝</span> Create Rx
                                </button>

                                <button
                                  onClick={() => handleViewPrescription(appt)}
                                  disabled={loadingPrescriptionId === appt.id}
                                  className="btn btn-secondary"
                                  style={{
                                    padding: '0.3rem 0.65rem',
                                    fontSize: '0.78rem',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '0.25rem'
                                  }}
                                  title="View Published Prescription"
                                >
                                  <span>{loadingPrescriptionId === appt.id ? '⏳' : '📄'}</span> View Rx
                                </button>
                              </>
                            )}

                            {appt.status?.toUpperCase() === 'EXPIRED' && (
                              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                                Expired
                              </span>
                            )}

                            {appt.status?.toUpperCase() === 'NO_SHOW' && (
                              <span style={{ fontSize: '0.8rem', color: '#b45309', fontWeight: 600 }}>
                                ⚠️ No-Show
                              </span>
                            )}

                            {(appt.status === 'CANCELLED' || appt.status === 'REJECTED') && (
                              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                                Closed
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

      {/* APPOINTMENT DETAILS MODAL (USES REACT PORTAL) */}
      {selectedAppointmentDetails && (
        <AppointmentDetailsModal
          appointment={selectedAppointmentDetails}
          onClose={() => setSelectedAppointmentDetails(null)}
          onCreateRx={(appt) => {
            setSelectedAppointmentDetails(null);
            setPrescriptionFormAppt(appt);
          }}
          onViewRx={(appt) => {
            setSelectedAppointmentDetails(null);
            handleViewPrescription(appt);
          }}
        />
      )}

      {/* PRESCRIPTION FORM MODAL (USES REACT PORTAL) */}
      {prescriptionFormAppt && (
        <PrescriptionForm
          appointment={prescriptionFormAppt}
          onSaved={handlePrescriptionSaved}
          onCancel={() => setPrescriptionFormAppt(null)}
        />
      )}

      {/* PRESCRIPTION VIEW MODAL (USES REACT PORTAL) */}
      {viewingPrescription && (
        <PrescriptionView
          prescription={viewingPrescription}
          onClose={() => setViewingPrescription(null)}
        />
      )}
    </div>
  );
}

export default AppointmentList;
