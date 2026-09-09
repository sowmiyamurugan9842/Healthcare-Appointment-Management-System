import React, { useState, useEffect, useCallback } from 'react';
import { appointmentAPI } from '../services/api';
import { Link } from 'react-router-dom';

function PatientAppointmentView({ user }) {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [patientId, setPatientId] = useState('');
  const [isPatientLinked, setIsPatientLinked] = useState(false);

  const isPatient = user && user.role === 'PATIENT';

  // Load linked patient ID from localStorage if exists
  useEffect(() => {
    if (isPatient) {
      const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
      if (cachedUser.patientId) {
        setPatientId(cachedUser.patientId);
        setIsPatientLinked(true);
      }
    }
  }, [isPatient]);

  const loadAppointments = useCallback(async () => {
    if (!patientId) return;
    setLoading(true);
    setErrorMessage('');
    try {
      const data = await appointmentAPI.getByPatient(patientId);
      setAppointments(data || []);
    } catch (err) {
      console.error('Failed to load patient appointments:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to retrieve appointments. Verify your Patient ID.');
    } finally {
      setLoading(false);
    }
  }, [patientId]);

  useEffect(() => {
    if (isPatientLinked && patientId) {
      loadAppointments();
    }
  }, [isPatientLinked, patientId, loadAppointments]);

  const handleLinkPatient = (e) => {
    e.preventDefault();
    if (!patientId) return;
    
    // Save to localStorage
    const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
    cachedUser.patientId = patientId;
    localStorage.setItem('user', JSON.stringify(cachedUser));
    
    setIsPatientLinked(true);
  };

  const handleUnlinkPatient = () => {
    const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
    delete cachedUser.patientId;
    localStorage.setItem('user', JSON.stringify(cachedUser));
    
    setIsPatientLinked(false);
    setPatientId('');
    setAppointments([]);
  };

  const handleCancelAppointment = async (appointmentId) => {
    if (!window.confirm('Are you sure you want to cancel this appointment?')) return;
    
    setErrorMessage('');
    setSuccessMessage('');
    try {
      // In backend cancel maps to CANCELLED status (accessible by PATIENT)
      await appointmentAPI.updateStatus(appointmentId, 'CANCELLED');
      setSuccessMessage(`Appointment #${appointmentId} successfully cancelled.`);
      loadAppointments();
    } catch (err) {
      console.error('Cancellation error:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to cancel appointment';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error cancelling appointment.');
    }
  };

  const getStatusBadgeClass = (status) => {
    if (!status) return 'badge-pending';
    const s = status.toUpperCase();
    if (s === 'PENDING') return 'badge-pending';
    if (s === 'CONFIRMED' || s === 'APPROVED') return 'badge-approved';
    if (s === 'CANCELLED' || s === 'REJECTED') return 'badge-rejected';
    if (s === 'COMPLETED') return 'badge-completed';
    return 'badge-pending';
  };

  if (!isPatient) {
    return (
      <div className="card" style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <h2>Access Denied</h2>
        <p style={{ marginTop: '1rem' }}>Only patient users can view personal appointment schedules.</p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem' }}>Go to Home</Link>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="flex-between" style={{ marginBottom: '2rem' }}>
        <div>
          <h1>My Medical Appointments</h1>
          <p>View and manage your upcoming and historical appointments.</p>
        </div>
        {isPatientLinked && (
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <Link to="/appointments/book" className="btn btn-accent">
              Book Appointment
            </Link>
            <button onClick={loadAppointments} disabled={loading} className="btn btn-secondary">
              {loading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
        )}
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      {/* UNLINKED PATIENT PROFILE ID PROMPT */}
      {!isPatientLinked && (
        <div style={{ maxWidth: '480px', margin: '2rem auto', textAlign: 'center' }} className="card">
          <h3>Link Patient Profile ID</h3>
          <p style={{ margin: '0.75rem 0 1.5rem', fontSize: '0.9rem' }}>
            To protect your patient record privacy, please link your Patient Profile ID (returned when your medical file was created by an Admin).
          </p>
          <form onSubmit={handleLinkPatient}>
            <div className="form-group">
              <label className="form-label" htmlFor="patientIdInput">Patient Profile ID</label>
              <input
                id="patientIdInput"
                type="number"
                required
                className="form-control"
                placeholder="e.g. 2"
                value={patientId}
                onChange={(e) => setPatientId(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
              Load Appointments
            </button>
          </form>
        </div>
      )}

      {/* APPOINTMENT GRID */}
      {isPatientLinked && (
        <>
          <div className="filter-bar" style={{ justifyContent: 'flex-end' }}>
            <button onClick={handleUnlinkPatient} className="btn btn-secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}>
              Unlink Patient ID (#{patientId})
            </button>
          </div>

          {loading ? (
            <p style={{ textAlign: 'center', padding: '2rem' }}>Retrieving your clinical schedules...</p>
          ) : appointments.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '3rem' }}>
              <p>No appointments booked under Patient ID #{patientId}.</p>
              <Link to="/appointments/book" className="btn btn-primary" style={{ marginTop: '1rem' }}>
                Book Your First Appointment
              </Link>
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Appt ID</th>
                    <th>Doctor Name</th>
                    <th>Date</th>
                    <th>Time</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th>Fee</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {appointments.map((appt) => (
                    <tr key={appt.id}>
                      <td><strong>#{appt.id}</strong></td>
                      <td style={{ fontWeight: '600', color: '#ffffff' }}>Dr. {appt.doctorName}</td>
                      <td>{appt.appointmentDate}</td>
                      <td style={{ color: 'var(--warning)' }}>{appt.appointmentTime?.substring(0, 5)}</td>
                      <td>{appt.reasonForVisit}</td>
                      <td>
                        <span className={`badge ${getStatusBadgeClass(appt.status)}`}>
                          {appt.status}
                        </span>
                      </td>
                      <td>${appt.consultationFee?.toFixed(2) || '0.00'}</td>
                      <td>
                        {/* Only allow cancellation if status is pending or confirmed/approved */}
                        {(appt.status === 'PENDING' || appt.status === 'CONFIRMED' || appt.status === 'APPROVED') ? (
                          <button
                            onClick={() => handleCancelAppointment(appt.id)}
                            className="btn btn-danger"
                            style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                          >
                            Cancel
                          </button>
                        ) : (
                          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>None</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default PatientAppointmentView;
