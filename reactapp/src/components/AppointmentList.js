import React, { useState, useEffect, useCallback } from 'react';
import { appointmentAPI } from '../services/api';
import { Link } from 'react-router-dom';

function AppointmentList({ user }) {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [selectedStatusFilter, setSelectedStatusFilter] = useState('ALL');

  // For DOCTOR role: doctorId linking
  const [doctorId, setDoctorId] = useState('');
  const [isDoctorLinked, setIsDoctorLinked] = useState(false);

  const isAdmin = user && user.role === 'ADMIN';
  const isDoctor = user && user.role === 'DOCTOR';

  // Load linked doctor ID from localStorage if exists
  useEffect(() => {
    if (isDoctor) {
      const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
      if (cachedUser.doctorId) {
        setDoctorId(cachedUser.doctorId);
        setIsDoctorLinked(true);
      }
    }
  }, [isDoctor]);

  const loadAppointments = useCallback(async () => {
    setLoading(true);
    setErrorMessage('');
    try {
      let data = [];
      if (isAdmin) {
        data = await appointmentAPI.getAll();
      } else if (isDoctor && isDoctorLinked && doctorId) {
        data = await appointmentAPI.getByDoctor(doctorId);
      }
      setAppointments(data || []);
    } catch (err) {
      console.error('Failed to load appointments:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to retrieve appointments. Check authentication.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin, isDoctor, isDoctorLinked, doctorId]);

  useEffect(() => {
    if (isAdmin || (isDoctor && isDoctorLinked)) {
      loadAppointments();
    }
  }, [isAdmin, isDoctor, isDoctorLinked, loadAppointments]);

  const handleLinkDoctor = (e) => {
    e.preventDefault();
    if (!doctorId) return;
    
    // Save to localStorage
    const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
    cachedUser.doctorId = doctorId;
    localStorage.setItem('user', JSON.stringify(cachedUser));
    
    setIsDoctorLinked(true);
    loadAppointments();
  };

  const handleUnlinkDoctor = () => {
    const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
    delete cachedUser.doctorId;
    localStorage.setItem('user', JSON.stringify(cachedUser));
    
    setIsDoctorLinked(false);
    setDoctorId('');
    setAppointments([]);
  };

  const handleStatusChange = async (appointmentId, newStatus) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      await appointmentAPI.updateStatus(appointmentId, newStatus);
      setSuccessMessage(`Appointment #${appointmentId} successfully updated to ${newStatus}!`);
      loadAppointments();
    } catch (err) {
      console.error('Status change error:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to update status';
      setErrorMessage(typeof msg === 'string' ? msg : `Error updating appointment status.`);
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

  // Filter appointments client-side by status
  const filteredAppointments = selectedStatusFilter === 'ALL'
    ? appointments
    : appointments.filter((a) => {
        const s = a.status ? a.status.toUpperCase() : 'PENDING';
        if (selectedStatusFilter === 'APPROVED' && (s === 'APPROVED' || s === 'CONFIRMED')) return true;
        if (selectedStatusFilter === 'REJECTED' && (s === 'REJECTED' || s === 'CANCELLED')) return true;
        return s === selectedStatusFilter;
      });

  if (!isAdmin && !isDoctor) {
    return (
      <div className="card" style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <h2>Access Denied</h2>
        <p style={{ marginTop: '1rem' }}>Only administrators and medical doctors can view the full appointment logs.</p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem' }}>Go to Home</Link>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="flex-between" style={{ marginBottom: '2rem' }}>
        <div>
          <h1>Appointment Log</h1>
          <p>
            {isAdmin 
              ? 'Administrator View: Accessing all clinical schedules.'
              : `Doctor View: Accessing appointments for Doctor ID #${doctorId}`}
          </p>
        </div>
        
        {(isAdmin || (isDoctor && isDoctorLinked)) && (
          <button onClick={loadAppointments} disabled={loading} className="btn btn-secondary">
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        )}
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      {/* DOCTOR ID UNLINKED VIEW */}
      {isDoctor && !isDoctorLinked && (
        <div style={{ maxWidth: '450px', margin: '2rem auto', textAlign: 'center' }} className="card">
          <h3>Link Doctor Profile ID</h3>
          <p style={{ margin: '0.75rem 0 1.5rem', fontSize: '0.9rem' }}>
            Please enter your Doctor ID (primary key of your Doctor medical profile created by Admin) to view your schedule.
          </p>
          <form onSubmit={handleLinkDoctor}>
            <div className="form-group">
              <label className="form-label" htmlFor="docIdInput">Doctor ID</label>
              <input
                id="docIdInput"
                type="number"
                required
                className="form-control"
                placeholder="e.g. 3"
                value={doctorId}
                onChange={(e) => setDoctorId(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
              Load Schedule
            </button>
          </form>
        </div>
      )}

      {/* APPOINTMENT LOG GRID & TABLE */}
      {(isAdmin || (isDoctor && isDoctorLinked)) && (
        <>
          {/* Status Filter Bar */}
          <div className="filter-bar">
            <div className="filter-group">
              <label className="form-label" style={{ marginBottom: 0, marginRight: '0.5rem' }} htmlFor="statusFilter">Filter Status:</label>
              <select
                id="statusFilter"
                className="form-control"
                style={{ width: 'auto', padding: '0.5rem 1rem' }}
                value={selectedStatusFilter}
                onChange={(e) => setSelectedStatusFilter(e.target.value)}
              >
                <option value="ALL">All Appointments</option>
                <option value="PENDING">Pending</option>
                <option value="APPROVED">Approved / Confirmed</option>
                <option value="REJECTED">Rejected / Cancelled</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>

            {isDoctor && (
              <button onClick={handleUnlinkDoctor} className="btn btn-secondary" style={{ marginLeft: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}>
                Unlink Doctor ID (#{doctorId})
              </button>
            )}
          </div>

          {loading ? (
            <p style={{ textAlign: 'center', padding: '2rem' }}>Retrieving appointments...</p>
          ) : filteredAppointments.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem' }}>No appointments registered.</p>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Appt ID</th>
                    {isAdmin && <th>Patient ID / Name</th>}
                    {!isAdmin && <th>Patient Name</th>}
                    <th>Doctor Name</th>
                    <th>Date</th>
                    <th>Time</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th>Fee</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAppointments.map((appt) => (
                    <tr key={appt.id}>
                      <td><strong>#{appt.id}</strong></td>
                      <td>
                        <div style={{ fontWeight: '500' }}>{appt.patientName}</div>
                        {isAdmin && <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>ID: #{appt.patientId}</span>}
                      </td>
                      <td style={{ color: '#ffffff' }}>Dr. {appt.doctorName}</td>
                      <td>{appt.appointmentDate}</td>
                      <td style={{ color: 'var(--warning)' }}>{appt.appointmentTime?.substring(0, 5)}</td>
                      <td>
                        <div style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={appt.reasonForVisit}>
                          {appt.reasonForVisit}
                        </div>
                      </td>
                      <td>
                        <span className={`badge ${getStatusBadgeClass(appt.status)}`}>
                          {appt.status}
                        </span>
                      </td>
                      <td style={{ fontWeight: '600' }}>${appt.consultationFee?.toFixed(2) || '0.00'}</td>
                      <td>
                        <div style={{ display: 'flex', gap: '0.4rem' }}>
                          {/* Approve/Confirm button */}
                          {(appt.status === 'PENDING') && (
                            <button
                              onClick={() => handleStatusChange(appt.id, 'APPROVED')}
                              className="btn btn-success"
                              style={{ padding: '0.3rem 0.5rem', fontSize: '0.8rem' }}
                              title="Approve / Confirm Appointment"
                            >
                              Approve
                            </button>
                          )}
                          
                          {/* Complete button */}
                          {(appt.status === 'CONFIRMED' || appt.status === 'APPROVED' || appt.status === 'PENDING') && (
                            <button
                              onClick={() => handleStatusChange(appt.id, 'COMPLETED')}
                              className="btn btn-primary"
                              style={{ padding: '0.3rem 0.5rem', fontSize: '0.8rem' }}
                              title="Mark as Completed"
                            >
                              Complete
                            </button>
                          )}

                          {/* Reject/Cancel button */}
                          {(appt.status === 'PENDING' || appt.status === 'CONFIRMED' || appt.status === 'APPROVED') && (
                            <button
                              onClick={() => handleStatusChange(appt.id, 'REJECTED')}
                              className="btn btn-danger"
                              style={{ padding: '0.3rem 0.5rem', fontSize: '0.8rem' }}
                              title="Cancel / Reject Appointment"
                            >
                              Reject
                            </button>
                          )}
                          
                          {/* If status is already completed or cancelled, display nothing */}
                          {(appt.status === 'COMPLETED' || appt.status === 'CANCELLED' || appt.status === 'REJECTED') && (
                            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>Locked</span>
                          )}
                        </div>
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

export default AppointmentList;
