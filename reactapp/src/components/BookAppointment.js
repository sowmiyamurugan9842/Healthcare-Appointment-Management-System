import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { doctorAPI, patientAPI, appointmentAPI, waitlistAPI } from '../services/api';

function BookAppointment({ user }) {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [doctors, setDoctors] = useState([]);
  const [patients, setPatients] = useState([]);
  const [patientName, setPatientName] = useState('');
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

  // Available Slots state
  const [availableSlots, setAvailableSlots] = useState([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [slotsFetched, setSlotsFetched] = useState(false);

  // Waitlist modal states
  const [showWaitlistModal, setShowWaitlistModal] = useState(false);
  const [waitlistPreferredTime, setWaitlistPreferredTime] = useState('');
  const [waitlistReason, setWaitlistReason] = useState('');
  const [waitlistLoading, setWaitlistLoading] = useState(false);
  const [waitlistError, setWaitlistError] = useState('');

  const isAdmin = user && user.role === 'ADMIN';
  const isPatient = user && user.role === 'PATIENT';

  // Fetch available slots whenever doctorId or appointmentDate changes
  useEffect(() => {
    const fetchSlots = async () => {
      if (!doctorId || !appointmentDate) {
        setAvailableSlots([]);
        setSlotsFetched(false);
        return;
      }
      setSlotsLoading(true);
      try {
        const res = await doctorAPI.getAvailableSlots(doctorId, appointmentDate);
        const slots = res.availableSlots || [];
        setAvailableSlots(slots);
        setSlotsFetched(true);
        // If current appointment time is no longer available, clear it
        setAppointmentTime((prevTime) => {
          if (prevTime && !slots.includes(prevTime.substring(0, 5))) {
            return '';
          }
          return prevTime;
        });
      } catch (err) {
        console.warn('Could not fetch doctor available slots:', err);
        setAvailableSlots([]);
        setSlotsFetched(true);
      } finally {
        setSlotsLoading(false);
      }
    };

    fetchSlots();
  }, [doctorId, appointmentDate]);

  useEffect(() => {
    const loadMetadata = async () => {
      setFetchLoading(true);
      try {
        const docList = await doctorAPI.getAll();
        setDoctors(docList || []);

        const urlDocId = searchParams.get('doctorId');
        if (urlDocId) {
          setDoctorId(urlDocId);
        } else if (docList && docList.length > 0) {
          setDoctorId(docList[0].id);
        }

        if (isAdmin) {
          const patList = await patientAPI.getAll();
          setPatients(patList || []);
          if (patList && patList.length > 0) {
            setPatientId(patList[0].id);
          }
        } else if (isPatient) {
          try {
            const profile = await patientAPI.getMe();
            if (profile) {
              setPatientName(profile.fullName || `${profile.firstName || ''} ${profile.lastName || ''}`.trim());
              const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
              if (profile.id) {
                cachedUser.patientId = profile.id;
                localStorage.setItem('user', JSON.stringify(cachedUser));
              }
            }
          } catch (profileErr) {
            console.debug('Patient profile retrieval on book appointment:', profileErr);
          }
        }
      } catch (err) {
        console.error('Failed to load initial data for booking:', err);
        setErrorMessage('Failed to load doctor/patient registries. Check backend connection.');
      } finally {
        setFetchLoading(false);
      }
    };

    loadMetadata();
  }, [searchParams, isAdmin, isPatient]);

  const selectedDoctorObj = doctors.find((d) => String(d.id) === String(doctorId));

  const validate = () => {
    const tempErrors = {};

    if (!doctorId) {
      tempErrors.doctorId = 'Please select a consulting doctor';
    }

    if (isAdmin && !patientId) {
      tempErrors.patientId = 'Please select a patient profile';
    }

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
        ...(isAdmin && patientId ? { patientId: Number(patientId) } : {}),
        appointmentDate,
        appointmentTime: formatTime(appointmentTime),
        reasonForVisit: reason
      };

      await appointmentAPI.book(appointmentData);

      setSuccessMessage('Appointment booked successfully! Our clinical staff will review your request.');

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
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header">
        <div className="page-title-group">
          <h1>Schedule Medical Consultation</h1>
          <p>Book an in-person or telehealth appointment with a hospital specialist.</p>
        </div>
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      <div className="card">
        {fetchLoading ? (
          <div className="empty-state-box">
            <div className="empty-state-icon">⏳</div>
            <div className="empty-state-title">Loading Medical Registries...</div>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            {/* LOGGED IN PATIENT IDENTIFIER */}
            {isPatient && (
              <div
                style={{
                  backgroundColor: 'var(--primary-light, #eff6ff)',
                  border: '1px solid var(--primary-light-border, #bfdbfe)',
                  borderRadius: 'var(--radius-md, 8px)',
                  padding: '0.85rem 1.25rem',
                  marginBottom: '1.5rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem'
                }}
              >
                <span style={{ fontSize: '1.3rem' }}>👤</span>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary, #64748b)', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.05em' }}>
                    Patient Account
                  </div>
                  <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--primary-dark, #1e3a8a)' }}>
                    Booking appointment for: {patientName || (user?.firstName ? `${user.firstName} ${user.lastName || ''}`.trim() : (user?.email || 'Logged-in Patient'))}
                  </div>
                </div>
              </div>
            )}

            {/* 1. SELECT DOCTOR */}
            <div className="form-group">
              <label className="form-label" htmlFor="doctorSelect">
                1. Select Consulting Physician <span style={{ color: 'var(--danger-text)' }}>*</span>
              </label>
              {doctors.length === 0 ? (
                <span className="form-error-msg">No doctors currently available in the registry.</span>
              ) : (
                <select
                  id="doctorSelect"
                  className="form-control"
                  value={doctorId}
                  onChange={(e) => setDoctorId(e.target.value)}
                >
                  {doctors.map((d) => (
                    <option key={d.id} value={d.id}>
                      Dr. {d.fullName} — {d.specialization} ({d.departmentName}) • Fee: ${d.consultationFee?.toFixed(2)}
                    </option>
                  ))}
                </select>
              )}
              {errors.doctorId && <span className="form-error-msg">{errors.doctorId}</span>}
            </div>

            {/* SELECTED DOCTOR HIGHLIGHT CARD */}
            {selectedDoctorObj && (
              <div
                style={{
                  backgroundColor: 'var(--primary-light)',
                  border: '1px solid var(--primary-light-border)',
                  borderRadius: 'var(--radius-md)',
                  padding: '1rem 1.25rem',
                  marginBottom: '1.5rem',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '0.75rem'
                }}
              >
                <div>
                  <strong style={{ color: 'var(--primary-dark)', fontSize: '1rem' }}>
                    Dr. {selectedDoctorObj.fullName}
                  </strong>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    {selectedDoctorObj.specialization} • {selectedDoctorObj.qualification} ({selectedDoctorObj.departmentName})
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', fontWeight: 700 }}>Fee</span>
                  <div style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--primary-dark)' }}>
                    ${selectedDoctorObj.consultationFee?.toFixed(2)}
                  </div>
                </div>
              </div>
            )}

            {/* 2. ADMIN PATIENT SELECTOR */}
            {isAdmin && (
              <div className="form-group">
                <label className="form-label" htmlFor="patientSelect">
                  2. Select Patient Profile <span style={{ color: 'var(--danger-text)' }}>*</span>
                </label>
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
                        {p.fullName} (ID: #{p.id} • {p.phoneNumber})
                      </option>
                    ))}
                  </select>
                )}
                {errors.patientId && <span className="form-error-msg">{errors.patientId}</span>}
              </div>
            )}

            {/* 3. DATE & TIME */}
            <div className="form-control-row">
              <div className="form-group">
                <label className="form-label" htmlFor="apptDate">
                  3. Appointment Date <span style={{ color: 'var(--danger-text)' }}>*</span>
                </label>
                <input
                  id="apptDate"
                  type="date"
                  className="form-control"
                  min={new Date().toISOString().split('T')[0]}
                  value={appointmentDate}
                  onChange={(e) => setAppointmentDate(e.target.value)}
                />
                {errors.appointmentDate && <span className="form-error-msg">{errors.appointmentDate}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="apptTime">
                  Appointment Time <span style={{ color: 'var(--danger-text)' }}>*</span>
                </label>
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

            {/* AUTOMATIC AVAILABLE SLOTS CALCULATOR */}
            {appointmentDate && (
              <div
                style={{
                  backgroundColor: 'var(--bg-card)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  padding: '1rem',
                  marginBottom: '1.25rem'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    🕒 Available Consultation Slots (30 min duration)
                  </span>
                  {slotsLoading && <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Calculating available slots...</span>}
                </div>

                {slotsLoading ? (
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', padding: '0.5rem 0' }}>
                    Checking doctor schedule and existing bookings...
                  </div>
                ) : slotsFetched && availableSlots.length === 0 ? (
                  <div
                    style={{
                      backgroundColor: '#fff7ed',
                      border: '1px solid #fed7aa',
                      borderRadius: 'var(--radius-md)',
                      padding: '1.25rem',
                      marginTop: '0.5rem'
                    }}
                  >
                    <div style={{ fontSize: '0.95rem', color: '#9a3412', fontWeight: 700, marginBottom: '0.4rem' }}>
                      ⚠️ No appointment slots are currently available.
                    </div>
                    <p style={{ fontSize: '0.85rem', color: '#7c2d12', margin: '0 0 1rem 0' }}>
                      All consultation slots for <strong>Dr. {selectedDoctorObj?.fullName}</strong> on <strong>{appointmentDate}</strong> are fully booked. Would you like to join the waitlist? You will be notified automatically if an appointment opens up.
                    </p>
                    <button
                      type="button"
                      id="joinWaitlistBtn"
                      className="btn"
                      onClick={() => {
                        setWaitlistReason(reason || 'Consultation request via waitlist');
                        setWaitlistPreferredTime(appointmentTime || '');
                        setWaitlistError('');
                        setShowWaitlistModal(true);
                      }}
                      style={{
                        backgroundColor: '#ea580c',
                        color: '#ffffff',
                        fontWeight: 600,
                        border: 'none',
                        padding: '0.55rem 1.1rem',
                        borderRadius: '6px',
                        cursor: 'pointer',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.5rem',
                        boxShadow: '0 2px 4px rgba(234, 88, 12, 0.2)'
                      }}
                    >
                      📋 Join Waitlist
                    </button>
                  </div>
                ) : availableSlots.length > 0 ? (
                  <div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.5rem' }}>
                      {availableSlots.map((slot) => {
                        const isSelected = appointmentTime === slot || appointmentTime === `${slot}:00`;
                        return (
                          <button
                            key={slot}
                            type="button"
                            onClick={() => setAppointmentTime(slot)}
                            style={{
                              padding: '0.4rem 0.8rem',
                              borderRadius: 'var(--radius-sm, 6px)',
                              border: isSelected ? '2px solid var(--primary-color)' : '1px solid var(--border-color)',
                              backgroundColor: isSelected ? 'var(--primary-color)' : 'var(--bg-main, #f8fafc)',
                              color: isSelected ? '#ffffff' : 'var(--text-primary)',
                              fontWeight: isSelected ? 700 : 500,
                              fontSize: '0.85rem',
                              cursor: 'pointer',
                              transition: 'all 0.15s ease'
                            }}
                          >
                            {slot}
                          </button>
                        );
                      })}
                    </div>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.5rem', display: 'block' }}>
                      Click any slot above to select it automatically.
                    </span>
                  </div>
                ) : (
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    Select a date above to view available slots.
                  </div>
                )}
              </div>
            )}


            {/* 4. REASON FOR VISIT */}
            <div className="form-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <label className="form-label" htmlFor="apptReason" style={{ marginBottom: 0 }}>
                  4. Reason for Consultation <span style={{ color: 'var(--danger-text)' }}>*</span>
                </label>
                <span style={{ fontSize: '0.75rem', color: reason.length < 10 || reason.length > 200 ? 'var(--danger-text)' : 'var(--text-muted)' }}>
                  {reason.length}/200 characters (min 10)
                </span>
              </div>
              <textarea
                id="apptReason"
                rows="3"
                className="form-control"
                placeholder="Describe your symptoms, checkup purpose, or follow-up notes..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                style={{ marginTop: '0.4rem' }}
              />
              {errors.reason && <span className="form-error-msg">{errors.reason}</span>}
            </div>

            {/* SUBMIT BUTTON */}
            <button
              type="submit"
              disabled={loading || !doctorId || (isAdmin && !patientId) || availableSlots.length === 0}
              className="btn btn-primary"
              style={{ width: '100%', padding: '0.8rem', fontSize: '1rem', marginTop: '0.5rem' }}
            >
              {loading ? 'Submitting Appointment Request...' : '✓ Confirm & Schedule Appointment'}
            </button>
          </form>
        )}
      </div>

      {/* JOIN WAITLIST MODAL */}
      {showWaitlistModal && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem'
          }}
        >
          <div
            className="card"
            style={{
              maxWidth: '520px',
              width: '100%',
              borderRadius: 'var(--radius-lg, 12px)',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
              animation: 'modalSlideIn 0.2s ease-out'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ margin: 0, fontSize: '1.25rem', color: 'var(--text-primary)' }}>
                📋 Join Appointment Waitlist
              </h3>
              <button
                type="button"
                onClick={() => setShowWaitlistModal(false)}
                style={{ background: 'none', border: 'none', fontSize: '1.25rem', cursor: 'pointer', color: 'var(--text-muted)' }}
              >
                ✕
              </button>
            </div>

            {waitlistError && <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>⚠️ {waitlistError}</div>}

            <div
              style={{
                backgroundColor: 'var(--bg-main, #f8fafc)',
                border: '1px solid var(--border-color)',
                borderRadius: '8px',
                padding: '0.85rem 1rem',
                marginBottom: '1rem',
                fontSize: '0.875rem'
              }}
            >
              <div><strong>Doctor:</strong> Dr. {selectedDoctorObj?.fullName} ({selectedDoctorObj?.specialization})</div>
              <div style={{ marginTop: '0.25rem' }}><strong>Date:</strong> {appointmentDate}</div>
            </div>

            <form
              onSubmit={async (e) => {
                e.preventDefault();
                setWaitlistError('');
                if (!waitlistReason.trim() || waitlistReason.trim().length < 10) {
                  setWaitlistError('Please provide a reason for consultation (minimum 10 characters).');
                  return;
                }
                setWaitlistLoading(true);
                try {
                  const payload = {
                    doctorId: Number(doctorId),
                    appointmentDate,
                    preferredTime: waitlistPreferredTime ? (waitlistPreferredTime.split(':').length === 2 ? `${waitlistPreferredTime}:00` : waitlistPreferredTime) : null,
                    reasonForVisit: waitlistReason.trim()
                  };
                  const res = await waitlistAPI.join(payload);
                  setShowWaitlistModal(false);
                  setSuccessMessage(`✅ You have been added to the waitlist (Position #${res.queuePosition || 1}). We will notify you when a slot opens up!`);
                  setWaitlistReason('');
                } catch (err) {
                  console.error('Waitlist join error:', err);
                  const msg = err.response?.data?.message || err.response?.data || 'Failed to join waitlist';
                  setWaitlistError(typeof msg === 'string' ? msg : 'Error joining waitlist.');
                } finally {
                  setWaitlistLoading(false);
                }
              }}
            >
              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label className="form-label" htmlFor="waitlistPrefTime">
                  Preferred Time (Optional)
                </label>
                <input
                  id="waitlistPrefTime"
                  type="time"
                  className="form-control"
                  value={waitlistPreferredTime}
                  onChange={(e) => setWaitlistPreferredTime(e.target.value)}
                />
                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem', display: 'block' }}>
                  Leave blank if you are open to <strong>any available time</strong> on this date.
                </span>
              </div>

              <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                <label className="form-label" htmlFor="waitlistReasonInput">
                  Reason for Visit <span style={{ color: 'var(--danger-text)' }}>*</span>
                </label>
                <textarea
                  id="waitlistReasonInput"
                  rows="3"
                  className="form-control"
                  placeholder="Describe your medical condition or consultation purpose..."
                  value={waitlistReason}
                  onChange={(e) => setWaitlistReason(e.target.value)}
                />
              </div>

              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setShowWaitlistModal(false)}
                  disabled={waitlistLoading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="confirmJoinWaitlistSubmitBtn"
                  className="btn btn-primary"
                  disabled={waitlistLoading}
                  style={{ backgroundColor: '#ea580c', borderColor: '#ea580c' }}
                >
                  {waitlistLoading ? 'Joining Waitlist...' : '✓ Confirm & Join Waitlist'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default BookAppointment;
