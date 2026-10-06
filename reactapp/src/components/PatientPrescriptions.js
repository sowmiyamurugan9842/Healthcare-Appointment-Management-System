import React, { useState, useEffect, useCallback } from 'react';
import { prescriptionAPI, patientAPI } from '../services/api';
import PrescriptionView from './PrescriptionView';
import { Link } from 'react-router-dom';

function PatientPrescriptions({ user }) {
  const [prescriptions, setPrescriptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [selectedPrescription, setSelectedPrescription] = useState(null);
  const [patientProfile, setPatientProfile] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  const isPatient = user && user.role === 'PATIENT';
  const isAdmin = user && user.role === 'ADMIN';

  const loadPrescriptions = useCallback(async () => {
    setLoading(true);
    setErrorMessage('');
    try {
      let data = [];
      if (isAdmin) {
        data = await prescriptionAPI.getAll();
      } else if (isPatient) {
        data = await prescriptionAPI.getMyPatientPrescriptions();
      }
      setPrescriptions(data || []);
    } catch (err) {
      console.error('Failed to load prescriptions:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to retrieve prescriptions.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin, isPatient]);

  useEffect(() => {
    if (isPatient) {
      patientAPI.getMe().then((profile) => {
        if (profile) setPatientProfile(profile);
      }).catch((err) => {
        console.debug('No patient profile found on prescriptions load:', err);
      });
    }
    if (isAdmin || isPatient) {
      loadPrescriptions();
    }
  }, [isAdmin, isPatient, loadPrescriptions]);

  const filteredPrescriptions = prescriptions.filter((px) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const docName = (px.doctorName || '').toLowerCase();
    const diag = (px.diagnosis || '').toLowerCase();
    const idStr = String(px.id);
    return docName.includes(q) || diag.includes(q) || idStr.includes(q);
  });

  if (!isPatient && !isAdmin) {
    return (
      <div className="card" style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <h2>Access Denied</h2>
        <p style={{ marginTop: '1rem' }}>Only patients and medical administrators can access personal prescription logs.</p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem' }}>Go to Dashboard</Link>
      </div>
    );
  }

  return (
    <div>
      {/* PAGE HEADER */}
      <div className="page-header">
        <div className="page-title-group">
          <h1>Digital Medical Prescriptions</h1>
          <p>
            {isAdmin
              ? 'Administrator View: Complete repository of issued clinical prescriptions.'
              : `Patient View: Digital prescriptions issued for ${patientProfile?.firstName ? `${patientProfile.firstName} ${patientProfile.lastName || ''}`.trim() : (user?.firstName || 'your care plan')}`}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to="/patient-appointments" className="btn btn-secondary">
            📅 My Appointments
          </Link>
          <button onClick={loadPrescriptions} disabled={loading} className="btn btn-primary">
            {loading ? 'Refreshing...' : '↻ Refresh Rx Log'}
          </button>
        </div>
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}

      {/* PRESCRIPTIONS LOG TABLE */}
      <div className="card">
        <div className="filter-bar" style={{ justifyContent: 'space-between' }}>
          <div className="filter-group" style={{ flex: '1 1 300px' }}>
            <input
              type="text"
              className="form-control"
              placeholder="Search prescription by doctor, diagnosis, or Rx ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        {loading ? (
            <div className="empty-state-box">
              <div className="empty-state-icon">⏳</div>
              <div className="empty-state-title">Retrieving Digital Prescriptions...</div>
            </div>
          ) : filteredPrescriptions.length === 0 ? (
            <div className="empty-state-box">
              <div className="empty-state-icon">💊</div>
              <div className="empty-state-title">No Prescriptions Found</div>
              <div className="empty-state-desc">
                Prescriptions will be published here automatically once consulting physicians issue them after completed appointments.
              </div>
              <Link to="/patient-appointments" className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
                View My Appointments
              </Link>
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Rx Reference</th>
                    <th>Appt Ref</th>
                    <th>Consulting Doctor</th>
                    {isAdmin && <th>Patient</th>}
                    <th>Issue Date</th>
                    <th>Clinical Diagnosis</th>
                    <th>WhatsApp Delivery</th>
                    <th>Medications Count</th>
                    <th>Follow-Up Date</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredPrescriptions.map((px) => {
                    const waStatus = (px.whatsappStatus || 'PENDING').toUpperCase();
                    let waBadgeClass = 'badge-pending';
                    let waText = '⏳ Pending';
                    if (waStatus === 'SENT') {
                      waBadgeClass = 'badge-approved';
                      waText = '✓ Sent';
                    } else if (waStatus === 'FAILED') {
                      waBadgeClass = 'badge-rejected';
                      waText = '⚠️ Failed';
                    } else if (waStatus === 'NOT_CONFIGURED') {
                      waBadgeClass = 'badge-expired';
                      waText = 'ℹ️ Available';
                    }

                    return (
                      <tr key={px.id}>
                        <td>
                          <span className="user-tag" style={{ fontWeight: 800 }}>
                            #PX-{px.id}
                          </span>
                        </td>
                        <td>
                          <strong style={{ color: 'var(--text-secondary)' }}>
                            Appt #{px.appointmentId || 'N/A'}
                          </strong>
                        </td>
                        <td>
                          <strong style={{ color: 'var(--primary-dark)' }}>
                            {px.doctorName?.startsWith('Dr. ') ? px.doctorName : `Dr. ${px.doctorName || 'Attending'}`}
                          </strong>
                        </td>
                        {isAdmin && (
                          <td>
                            {px.patientName} (ID: #{px.patientId})
                          </td>
                        )}
                        <td>{px.appointmentDate || 'N/A'}</td>
                        <td>
                          <div
                            style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                            title={px.diagnosis}
                          >
                            {px.diagnosis}
                          </div>
                        </td>
                        <td>
                          <span className={`badge ${waBadgeClass}`} style={{ fontSize: '0.75rem' }} title={px.whatsappError || ''}>
                            {waText}
                          </span>
                        </td>
                        <td>
                          <span className="badge badge-completed" style={{ fontSize: '0.75rem' }}>
                            {px.medicines ? px.medicines.length : 1} Medication(s)
                          </span>
                        </td>
                        <td style={{ color: 'var(--primary-dark)', fontWeight: 600, fontSize: '0.85rem' }}>
                          {px.followUpDate || px.nextVisitDate ? (
                            <div>
                              <div>📅 {px.followUpDate || px.nextVisitDate}</div>
                              {px.followUpTime && (
                                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 'normal' }}>
                                  ⏰ {px.followUpTime.substring(0, 5)}
                                </div>
                              )}
                            </div>
                          ) : (
                            <span style={{ color: 'var(--text-muted)', fontWeight: 'normal', fontStyle: 'italic' }}>
                              No follow-up
                            </span>
                          )}
                        </td>
                        <td>
                          <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                            <button
                              onClick={() => setSelectedPrescription(px)}
                              className="btn btn-accent"
                              style={{
                                padding: '0.35rem 0.65rem',
                                fontSize: '0.78rem',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.25rem'
                              }}
                            >
                              <span>📄</span> View
                            </button>

                            <button
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
                              title="Download PDF"
                            >
                              <span>📥</span> PDF
                            </button>
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

      {/* PRESCRIPTION DETAIL MODAL (USES REACT PORTAL) */}
      {selectedPrescription && (
        <PrescriptionView
          prescription={selectedPrescription}
          onClose={() => setSelectedPrescription(null)}
          user={user}
        />
      )}
    </div>
  );
}

export default PatientPrescriptions;
