import React, { useState, useEffect, useCallback } from 'react';
import { appointmentAPI, prescriptionAPI } from '../services/api';
import { Link } from 'react-router-dom';
import PrescriptionForm from './PrescriptionForm';
import PrescriptionView from './PrescriptionView';

function DoctorPrescriptions({ user }) {
  const [completedAppointments, setCompletedAppointments] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState('ALL'); // ALL, PENDING_RX, ISSUED_RX

  // Prescription Modals state
  const [prescriptionFormAppt, setPrescriptionFormAppt] = useState(null);
  const [viewingPrescription, setViewingPrescription] = useState(null);
  const [loadingPrescriptionApptId, setLoadingPrescriptionApptId] = useState(null);

  // Doctor Linking State
  const [doctorId, setDoctorId] = useState('');
  const [isDoctorLinked, setIsDoctorLinked] = useState(false);

  const isDoctor = user && user.role === 'DOCTOR';
  const isAdmin = user && user.role === 'ADMIN';

  useEffect(() => {
    if (isDoctor) {
      const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
      if (cachedUser.doctorId) {
        setDoctorId(cachedUser.doctorId);
        setIsDoctorLinked(true);
      }
    }
  }, [isDoctor]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setErrorMessage('');
    try {
      let appts = [];
      let rxs = [];

      if (isAdmin) {
        const [allAppts, allRxs] = await Promise.all([
          appointmentAPI.getAll().catch(() => []),
          prescriptionAPI.getAll().catch(() => [])
        ]);
        appts = (allAppts || []).filter((a) => a.status?.toUpperCase() === 'COMPLETED');
        rxs = allRxs || [];
      } else if (isDoctor && isDoctorLinked && doctorId) {
        const [docAppts, docRxs] = await Promise.all([
          appointmentAPI.getByDoctor(doctorId).catch(() => []),
          prescriptionAPI.getByDoctor(doctorId).catch(() => [])
        ]);
        appts = (docAppts || []).filter((a) => a.status?.toUpperCase() === 'COMPLETED');
        rxs = docRxs || [];
      }

      setCompletedAppointments(appts);
      setPrescriptions(rxs);
    } catch (err) {
      console.error('Failed to load prescription management data:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to retrieve completed consultations.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin, isDoctor, isDoctorLinked, doctorId]);

  useEffect(() => {
    if (isAdmin || (isDoctor && isDoctorLinked)) {
      loadData();
    }
  }, [isAdmin, isDoctor, isDoctorLinked, loadData]);

  const handleLinkDoctor = (e) => {
    e.preventDefault();
    if (!doctorId) return;

    const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
    cachedUser.doctorId = doctorId;
    localStorage.setItem('user', JSON.stringify(cachedUser));

    setIsDoctorLinked(true);
  };

  const handleUnlinkDoctor = () => {
    const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
    delete cachedUser.doctorId;
    localStorage.setItem('user', JSON.stringify(cachedUser));

    setIsDoctorLinked(false);
    setDoctorId('');
    setCompletedAppointments([]);
    setPrescriptions([]);
  };

  const handleViewPrescription = async (appt) => {
    setErrorMessage('');
    setLoadingPrescriptionApptId(appt.id);
    try {
      // 1. Check if prescription is already in loaded list
      const existingPx = prescriptions.find((p) => p.appointmentId === appt.id);
      if (existingPx) {
        setViewingPrescription(existingPx);
        return;
      }

      // 2. Otherwise fetch by appointment ID from API
      const px = await prescriptionAPI.getByAppointment(appt.id);
      setViewingPrescription(px);
    } catch (err) {
      console.warn('Prescription not found for appointment:', appt.id);
      if (err.response && err.response.status === 404) {
        setPrescriptionFormAppt(appt);
      } else {
        setErrorMessage(err.response?.data?.message || 'Unable to load prescription.');
      }
    } finally {
      setLoadingPrescriptionApptId(null);
    }
  };

  const handlePrescriptionSaved = (savedPx) => {
    setPrescriptionFormAppt(null);
    setSuccessMessage(`Prescription #PX-${savedPx.id} successfully issued for Appointment #${savedPx.appointmentId}!`);
    loadData();
    setViewingPrescription(savedPx);
  };

  // Helper to find prescription by appointment ID
  const getPrescriptionForAppt = (apptId) => {
    return prescriptions.find((p) => p.appointmentId === apptId);
  };

  // Filter consultations
  const filteredList = completedAppointments.filter((appt) => {
    const px = getPrescriptionForAppt(appt.id);

    if (filterType === 'PENDING_RX' && px) return false;
    if (filterType === 'ISSUED_RX' && !px) return false;

    if (searchQuery.trim() !== '') {
      const q = searchQuery.toLowerCase();
      const pName = (appt.patientName || '').toLowerCase();
      const reason = (appt.reasonForVisit || '').toLowerCase();
      const idStr = String(appt.id);
      const pxDiag = px ? (px.diagnosis || '').toLowerCase() : '';
      const pxIdStr = px ? String(px.id) : '';

      return pName.includes(q) || reason.includes(q) || idStr.includes(q) || pxDiag.includes(q) || pxIdStr.includes(q);
    }

    return true;
  });

  if (!isAdmin && !isDoctor) {
    return (
      <div className="card" style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <h2>Access Denied</h2>
        <p style={{ marginTop: '1rem' }}>Only physicians and medical administrators can access prescription console.</p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem' }}>Go to Dashboard</Link>
      </div>
    );
  }

  return (
    <div>
      {/* PAGE HEADER */}
      <div className="page-header">
        <div className="page-title-group">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <span style={{ fontSize: '1.75rem' }}>💊</span>
            <h1>Clinical Prescription Console</h1>
          </div>
          <p>
            {isAdmin
              ? 'Administrator View: Comprehensive clinical prescriptions and completed visit records.'
              : `Doctor View: Issue & audit digital prescriptions for completed visits (Doctor ID #${doctorId}).`}
          </p>
        </div>

        {(isAdmin || (isDoctor && isDoctorLinked)) && (
          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            <Link to="/appointments" className="btn btn-secondary">
              📅 My Appointments
            </Link>
            <button onClick={loadData} disabled={loading} className="btn btn-primary">
              {loading ? 'Refreshing...' : '↻ Refresh Rx Console'}
            </button>
          </div>
        )}
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      {/* DOCTOR UNLINKED PROMPT */}
      {isDoctor && !isDoctorLinked && (
        <div style={{ maxWidth: '480px', margin: '2rem auto', textAlign: 'center' }} className="card">
          <div className="empty-state-icon">🩺</div>
          <h3>Link Doctor Profile ID</h3>
          <p style={{ margin: '0.75rem 0 1.5rem', fontSize: '0.9rem' }}>
            Please enter your Doctor Profile ID to manage prescriptions for your completed consultations.
          </p>
          <form onSubmit={handleLinkDoctor}>
            <div className="form-group">
              <label className="form-label" htmlFor="docIdInput">Doctor Profile ID</label>
              <input
                id="docIdInput"
                type="number"
                required
                className="form-control"
                placeholder="e.g. 4"
                value={doctorId}
                onChange={(e) => setDoctorId(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
              Load Prescription Console
            </button>
          </form>
        </div>
      )}

      {/* COMPLETED CONSULTATIONS & PRESCRIPTIONS TABLE */}
      {(isAdmin || (isDoctor && isDoctorLinked)) && (
        <div className="card">
          {/* TOOLBAR */}
          <div className="filter-bar" style={{ justifyContent: 'space-between' }}>
            <div className="filter-group" style={{ flex: '1 1 280px' }}>
              <input
                type="text"
                className="form-control"
                placeholder="Search patient, reason, Appt ID, or Rx..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <div className="filter-group">
                <label className="form-label" style={{ marginBottom: 0, marginRight: '0.25rem' }} htmlFor="rxFilter">
                  Filter:
                </label>
                <select
                  id="rxFilter"
                  className="form-control"
                  style={{ width: 'auto', padding: '0.5rem 0.85rem' }}
                  value={filterType}
                  onChange={(e) => setFilterType(e.target.value)}
                >
                  <option value="ALL">All Completed Visits ({completedAppointments.length})</option>
                  <option value="PENDING_RX">Needs Prescription ({completedAppointments.filter(a => !getPrescriptionForAppt(a.id)).length})</option>
                  <option value="ISSUED_RX">Prescriptions Issued ({completedAppointments.filter(a => getPrescriptionForAppt(a.id)).length})</option>
                </select>
              </div>

              {isDoctor && (
                <button
                  onClick={handleUnlinkDoctor}
                  className="btn btn-secondary"
                  style={{ padding: '0.45rem 0.75rem', fontSize: '0.8rem' }}
                  title="Unlink current doctor profile ID"
                >
                  Unlink ID (#{doctorId})
                </button>
              )}
            </div>
          </div>

          {/* TABLE CONTENT */}
          {loading ? (
            <div className="empty-state-box">
              <div className="empty-state-icon">⏳</div>
              <div className="empty-state-title">Loading Prescription Console...</div>
            </div>
          ) : filteredList.length === 0 ? (
            <div className="empty-state-box">
              <div className="empty-state-icon">💊</div>
              <div className="empty-state-title">No Completed Visits Found</div>
              <div className="empty-state-desc">
                {searchQuery || filterType !== 'ALL'
                  ? 'No completed visits match your search and filter criteria.'
                  : 'There are currently no completed consultations eligible for prescription issuance.'}
              </div>
              <Link to="/appointments" className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
                Go to My Appointments
              </Link>
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Appt ID</th>
                    <th>Patient Name</th>
                    <th>Consultation Date</th>
                    <th>Time</th>
                    <th>Clinical Reason</th>
                    <th>Prescription Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredList.map((appt) => {
                    const px = getPrescriptionForAppt(appt.id);
                    const hasPrescription = Boolean(px);

                    return (
                      <tr key={appt.id}>
                        <td>
                          <strong>#{appt.id}</strong>
                        </td>
                        <td>
                          <div style={{ fontWeight: '700', color: 'var(--text-primary)' }}>
                            {appt.patientName}
                          </div>
                          {appt.patientId && (
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                              Patient ID: #{appt.patientId}
                            </span>
                          )}
                        </td>
                        <td>{appt.appointmentDate}</td>
                        <td style={{ color: 'var(--primary-dark)', fontWeight: 600 }}>
                          {appt.appointmentTime?.substring(0, 5)}
                        </td>
                        <td>
                          <div
                            style={{ maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                            title={appt.reasonForVisit}
                          >
                            {appt.reasonForVisit}
                          </div>
                        </td>
                        <td>
                          {hasPrescription ? (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', alignItems: 'flex-start' }}>
                              <span className="badge badge-approved" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                                <span>✓</span> Rx Issued (#{px.id})
                              </span>
                              {px.whatsappStatus === 'SENT' && (
                                <span className="badge badge-approved" style={{ fontSize: '0.7rem' }}>
                                  ✓ WhatsApp: Sent
                                </span>
                              )}
                              {px.whatsappStatus === 'FAILED' && (
                                <span className="badge badge-rejected" style={{ fontSize: '0.7rem' }} title={px.whatsappError || ''}>
                                  ⚠️ WhatsApp: Failed
                                </span>
                              )}
                              {px.whatsappStatus === 'NOT_CONFIGURED' && (
                                <span className="badge badge-expired" style={{ fontSize: '0.7rem' }}>
                                  ℹ️ WhatsApp: Unconfigured
                                </span>
                              )}
                              {(!px.whatsappStatus || px.whatsappStatus === 'PENDING') && (
                                <span className="badge badge-pending" style={{ fontSize: '0.7rem' }}>
                                  ⏳ WhatsApp: Pending
                                </span>
                              )}
                              {(px.followUpDate || px.nextVisitDate) && (
                                <span style={{ fontSize: '0.72rem', color: 'var(--primary-dark)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.2rem', marginTop: '0.1rem' }}>
                                  <span>🔄</span> Follow-up: {px.followUpDate || px.nextVisitDate} {px.followUpTime ? `(${px.followUpTime.substring(0, 5)})` : ''}
                                </span>
                              )}
                            </div>
                          ) : (
                            <span className="badge badge-pending" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                              <span>⚠️</span> Needs Rx
                            </span>
                          )}
                        </td>
                        <td>
                          <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center', flexWrap: 'wrap' }}>
                            {hasPrescription ? (
                              <>
                                <button
                                  type="button"
                                  onClick={() => handleViewPrescription(appt)}
                                  disabled={loadingPrescriptionApptId === appt.id}
                                  className="btn btn-secondary"
                                  style={{ padding: '0.35rem 0.65rem', fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                                  title="View Digital Prescription"
                                >
                                  <span>{loadingPrescriptionApptId === appt.id ? '⏳' : '📄'}</span> View
                                </button>
                                <button
                                  type="button"
                                  onClick={async () => {
                                    try {
                                      await prescriptionAPI.downloadPdf(px.id);
                                    } catch (err) {
                                      alert(err.response?.data?.message || 'Failed to download PDF.');
                                    }
                                  }}
                                  className="btn btn-primary"
                                  style={{
                                    padding: '0.35rem 0.65rem',
                                    fontSize: '0.78rem',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '0.25rem',
                                    backgroundColor: '#075E66',
                                    borderColor: '#075E66'
                                  }}
                                  title="Download Signed PDF"
                                >
                                  <span>📥</span> PDF
                                </button>
                                <button
                                  type="button"
                                  onClick={async () => {
                                    try {
                                      const res = await prescriptionAPI.sendWhatsApp(px.id);
                                      alert(res.message || 'WhatsApp message dispatched.');
                                      loadData();
                                    } catch (err) {
                                      alert(err.response?.data?.message || 'WhatsApp delivery failed.');
                                    }
                                  }}
                                  className="btn btn-secondary"
                                  style={{
                                    padding: '0.35rem 0.65rem',
                                    fontSize: '0.78rem',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '0.25rem',
                                    borderColor: '#10b981',
                                    color: '#047857'
                                  }}
                                  title="Dispatch or Retry WhatsApp Delivery"
                                >
                                  <span>📱</span> {px.whatsappStatus === 'FAILED' ? 'Retry WA' : 'Send WA'}
                                </button>
                                <button
                                  type="button"
                                  onClick={() => setPrescriptionFormAppt(appt)}
                                  className="btn btn-secondary"
                                  style={{ padding: '0.35rem 0.55rem', fontSize: '0.78rem' }}
                                  title="Re-issue or Update Prescription"
                                >
                                  <span>✏️</span> Edit
                                </button>
                              </>
                            ) : (
                              <button
                                type="button"
                                onClick={() => setPrescriptionFormAppt(appt)}
                                className="btn btn-accent"
                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                                title="Create and Issue Digital Prescription"
                              >
                                <span>📝</span> Create Rx
                              </button>
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
          user={user}
        />
      )}
    </div>
  );
}

export default DoctorPrescriptions;
