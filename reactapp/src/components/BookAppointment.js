import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { doctorAPI, patientAPI, appointmentAPI } from '../services/api';

function BookAppointment({ user }) {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [doctors, setDoctors] = useState([]);
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fetchLoading, setFetchLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Form states
  const [doctorId, setDoctorId] = useState('');
  const [patientId, setPatientId] = useState('');
  const [appointmentDate, setAppointmentDate] = useState('');
  const [appointmentTime, setAppointmentTime] = useState('');
  const [reason, setReason] = useState('');
  const [errors, setErrors] = useState({});

  const isAdmin = user && user.role === 'ADMIN';
  const isPatient = user && user.role === 'PATIENT';

  // Load doctors & patients list
  useEffect(() => {
    const loadMetadata = async () => {
      setFetchLoading(true);
      try {
        const docList = await doctorAPI.getAll();
        setDoctors(docList || []);
        
        // Auto-select doctor from query param if available
        const urlDocId = searchParams.get('doctorId');
        if (urlDocId) {
          setDoctorId(urlDocId);
        } else if (docList && docList.length > 0) {
          setDoctorId(docList[0].id);
        }

        // Admins need to select a patient, so fetch all patients
        if (isAdmin) {
          const patList = await patientAPI.getAll();
          setPatients(patList || []);
          if (patList && patList.length > 0) {
            setPatientId(patList[0].id);
          }
        } else if (isPatient) {
          // If Patient, try to get linked patient profile ID from localStorage
          const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
          if (cachedUser.patientId) {
            setPatientId(cachedUser.patientId);
          }
        }
      } catch (err) {
        console.error('Failed to load initial data for booking:', err);
        setErrorMessage('Failed to load doctor/patient registries. Check authentication or backend.');
      } finally {
        setFetchLoading(false);
      }
    };
    
    loadMetadata();
  }, [searchParams, isAdmin, isPatient]);

  const validate = () => {
    const tempErrors = {};
    
    if (!doctorId) {
      tempErrors.doctorId = 'Doctor must be selected';
    }
    
    if (!patientId) {
      tempErrors.patientId = 'Patient ID is required';
    }

    // Date must be today or in the future
    if (!appointmentDate) {
      tempErrors.appointmentDate = 'Appointment date is required';
    } else {
      const selectedDate = new Date(appointmentDate + 'T00:00:00');
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (selectedDate < today) {
        tempErrors.appointmentDate = 'Appointment date must be today or in the future';
      }
    }

    if (!appointmentTime) {
      tempErrors.appointmentTime = 'Appointment time is required';
    }

    // Reason must be 10-200 characters
    if (!reason.trim()) {
      tempErrors.reason = 'Reason for visit is required';
    } else if (reason.length < 10 || reason.length > 200) {
      tempErrors.reason = 'Reason must be between 10 and 200 characters';
    }

    setErrors(tempErrors);
    return Object.keys(tempErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!validate()) return;
    setLoading(true);

    try {
      const formatTime = (timeStr) => {
        if (timeStr && timeStr.split(':').length === 2) {
          return `${timeStr}:00`;
        }
        return timeStr;
      };

      const appointmentData = {
        doctorId: Number(doctorId),
        patientId: Number(patientId),
        appointmentDate,
        appointmentTime: formatTime(appointmentTime),
        reasonForVisit: reason // mapping entered reason to backend expected reasonForVisit
      };

      await appointmentAPI.book(appointmentData);
      
      setSuccessMessage('Appointment booked successfully!');
      
      // Store patient ID in local user storage to remember it for future bookings
      if (isPatient) {
        const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
        cachedUser.patientId = patientId;
        localStorage.setItem('user', JSON.stringify(cachedUser));
      }

      setReason('');
      setAppointmentDate('');
      setAppointmentTime('');
      
      setTimeout(() => {
        if (isPatient) {
          navigate('/patient-appointments');
        } else {
          navigate('/appointments');
        }
      }, 1500);
    } catch (error) {
      console.error('Failed to book appointment:', error);
      const msg = error.response?.data?.message || error.response?.data || 'Failed to book appointment';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error processing appointment booking.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '550px', margin: '2rem auto 0' }} className="card">
      <h2 className="form-title">Book Medical Appointment</h2>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      {fetchLoading ? (
        <p style={{ textAlign: 'center', padding: '1rem' }}>Loading required data...</p>
      ) : (
        <form onSubmit={handleSubmit}>
          {/* Patient Selection/Input */}
          {isAdmin ? (
            <div className="form-group">
              <label className="form-label" htmlFor="patientSelect">Select Patient</label>
              {patients.length === 0 ? (
                <span className="form-error-msg">No patients registered. Please register a patient first.</span>
              ) : (
                <select
                  id="patientSelect"
                  className="form-control"
                  value={patientId}
                  onChange={(e) => setPatientId(e.target.value)}
                >
                  {patients.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.fullName} (ID: #{p.id})
                    </option>
                  ))}
                </select>
              )}
              {errors.patientId && <span className="form-error-msg">{errors.patientId}</span>}
            </div>
          ) : (
            <div className="form-group">
              <label className="form-label" htmlFor="patientId">Your Patient ID</label>
              <input
                id="patientId"
                type="number"
                className="form-control"
                placeholder="Enter your Patient ID profile reference"
                value={patientId}
                onChange={(e) => setPatientId(e.target.value)}
              />
              <span className="form-label" style={{ fontSize: '0.75rem', marginTop: '0.25rem', color: 'var(--text-muted)' }}>
                Note: This is the ID returned when your patient medical file is created by an Admin.
              </span>
              {errors.patientId && <span className="form-error-msg">{errors.patientId}</span>}
            </div>
          )}

          {/* Doctor Selection */}
          <div className="form-group">
            <label className="form-label" htmlFor="doctorSelect">Select Doctor</label>
            {doctors.length === 0 ? (
              <span className="form-error-msg">No doctors available. Please register a doctor first.</span>
            ) : (
              <select
                id="doctorSelect"
                className="form-control"
                value={doctorId}
                onChange={(e) => setDoctorId(e.target.value)}
              >
                {doctors.map((d) => (
                  <option key={d.id} value={d.id}>
                    Dr. {d.fullName} ({d.specialization} - Fee: ${d.consultationFee})
                  </option>
                ))}
              </select>
            )}
            {errors.doctorId && <span className="form-error-msg">{errors.doctorId}</span>}
          </div>

          {/* Appointment Date and Time */}
          <div className="form-control-row">
            <div className="form-group">
              <label className="form-label" htmlFor="apptDate">Appointment Date</label>
              <input
                id="apptDate"
                type="date"
                className="form-control"
                value={appointmentDate}
                onChange={(e) => setAppointmentDate(e.target.value)}
              />
              {errors.appointmentDate && <span className="form-error-msg">{errors.appointmentDate}</span>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="apptTime">Appointment Time</label>
              <input
                id="apptTime"
                type="time"
                className="form-control"
                value={appointmentTime}
                onChange={(e) => setAppointmentTime(e.target.value)}
              />
              {errors.appointmentTime && <span className="form-error-msg">{errors.appointmentTime}</span>}
            </div>
          </div>

          {/* Reason for Visit */}
          <div className="form-group">
            <label className="form-label" htmlFor="apptReason">Reason for Visit (10 - 200 chars)</label>
            <textarea
              id="apptReason"
              rows="3"
              className="form-control"
              placeholder="Provide a summary of the symptoms or checkup purpose..."
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            {errors.reason && <span className="form-error-msg">{errors.reason}</span>}
          </div>

          <button
            type="submit"
            disabled={loading || !doctorId || !patientId}
            className="btn btn-accent"
            style={{ width: '100%', marginTop: '1rem' }}
          >
            {loading ? 'Submitting Appointment...' : 'Schedule Appointment'}
          </button>
        </form>
      )}
    </div>
  );
}

export default BookAppointment;
