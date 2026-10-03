import React from 'react';
import ReactDOM from 'react-dom';

function AppointmentDetailsModal({ appointment, onClose, onCreateRx, onViewRx }) {
  if (!appointment) return null;

  const isCompleted = appointment.status?.toUpperCase() === 'COMPLETED';

  // Format date: e.g. "2026-10-02" -> "02 Oct 2026"
  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const parts = String(dateStr).split('-');
      if (parts.length === 3) {
        const year = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10) - 1;
        const day = parseInt(parts[2], 10);
        const d = new Date(year, month, day);
        if (!isNaN(d.getTime())) {
          return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
        }
      }
      const d = new Date(dateStr);
      return isNaN(d.getTime()) ? dateStr : d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  };

  // Format time: e.g. "10:00:00" -> "10:00 AM"
  const formatTime = (timeStr) => {
    if (!timeStr) return 'N/A';
    try {
      const parts = String(timeStr).split(':');
      if (parts.length >= 2) {
        let hour = parseInt(parts[0], 10);
        const minute = parts[1].padStart(2, '0');
        const ampm = hour >= 12 ? 'PM' : 'AM';
        hour = hour % 12;
        hour = hour === 0 ? 12 : hour;
        const formattedHour = hour < 10 ? `0${hour}` : `${hour}`;
        return `${formattedHour}:${minute} ${ampm}`;
      }
      return timeStr;
    } catch {
      return timeStr;
    }
  };

  const formattedDoctorName = appointment.doctorName?.startsWith('Dr. ')
    ? appointment.doctorName
    : `Dr. ${appointment.doctorName || 'Attending Specialist'}`;

  const modalContent = (
    <div className="prescription-modal-backdrop" onClick={onClose}>
      <div 
        className="prescription-form-modal-content"
        style={{ maxWidth: '620px', padding: '2rem' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* HEADER */}
        <div className="form-modal-header" style={{ marginBottom: '1.25rem', paddingBottom: '0.85rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
              <span style={{ fontSize: '1.3rem' }}>📋</span>
              <h2 style={{ fontSize: '1.35rem', margin: 0, color: 'var(--primary-dark)' }}>
                Appointment Details
              </h2>
            </div>
            <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
              Completed Clinical Consultation Record #{appointment.id}
            </p>
          </div>
          <button 
            type="button" 
            onClick={onClose} 
            className="close-btn" 
            title="Close modal"
            aria-label="Close"
          >
            &times;
          </button>
        </div>

        {/* SUMMARY BADGE BANNER */}
        <div 
          style={{
            background: 'linear-gradient(135deg, var(--primary-light) 0%, #F0FDFA 100%)',
            border: '1px solid var(--primary-light-border)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.15rem 1.25rem',
            marginBottom: '1.5rem',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '0.75rem'
          }}
        >
          <div>
            <div style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)', fontWeight: 700 }}>
              Consultation Reference
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--primary-dark)', marginTop: '0.15rem' }}>
              Appointment #{appointment.id}
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span className="badge badge-completed" style={{ fontSize: '0.85rem', padding: '0.35rem 0.85rem', fontWeight: 700 }}>
              ✓ {appointment.status || 'COMPLETED'}
            </span>
          </div>
        </div>

        {/* DETAILS GRID / CARD */}
        <div 
          style={{
            background: '#FFFFFF',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.25rem 1.5rem',
            marginBottom: '1.5rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem'
          }}
        >
          {/* PATIENT */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', paddingBottom: '0.85rem', borderBottom: '1px solid var(--border-subtle)' }}>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, minWidth: '110px' }}>
              Patient
            </span>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                {appointment.patientName || 'N/A'}
              </div>
              {appointment.patientId && (
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  Patient ID: #{appointment.patientId}
                </div>
              )}
            </div>
          </div>

          {/* DOCTOR */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', paddingBottom: '0.85rem', borderBottom: '1px solid var(--border-subtle)' }}>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, minWidth: '110px' }}>
              Doctor
            </span>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--primary-dark)' }}>
                {formattedDoctorName}
              </div>
              {appointment.departmentName && (
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  {appointment.departmentName}
                </div>
              )}
            </div>
          </div>

          {/* DATE */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: '0.85rem', borderBottom: '1px solid var(--border-subtle)' }}>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, minWidth: '110px' }}>
              Date
            </span>
            <span style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              {formatDate(appointment.appointmentDate)}
            </span>
          </div>

          {/* TIME */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: '0.85rem', borderBottom: '1px solid var(--border-subtle)' }}>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, minWidth: '110px' }}>
              Time
            </span>
            <span style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--primary-dark)' }}>
              {formatTime(appointment.appointmentTime)}
            </span>
          </div>

          {/* REASON */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', paddingBottom: '0.85rem', borderBottom: '1px solid var(--border-subtle)' }}>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, minWidth: '110px' }}>
              Reason
            </span>
            <div style={{ maxWidth: '340px', textAlign: 'right', fontSize: '0.95rem', color: 'var(--text-primary)', fontWeight: 500 }}>
              {appointment.reasonForVisit || 'General Clinical Consultation'}
            </div>
          </div>

          {/* STATUS */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: '0.85rem', borderBottom: '1px solid var(--border-subtle)' }}>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, minWidth: '110px' }}>
              Status
            </span>
            <span className="badge badge-completed" style={{ fontSize: '0.8rem', padding: '0.25rem 0.75rem' }}>
              {appointment.status || 'COMPLETED'}
            </span>
          </div>

          {/* CONSULTATION FEE */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, minWidth: '110px' }}>
              Fee
            </span>
            <span style={{ fontSize: '1rem', fontWeight: 800, color: 'var(--primary-dark)' }}>
              ${appointment.consultationFee?.toFixed(2) || '0.00'}
            </span>
          </div>
        </div>

        {/* FOOTER ACTIONS */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', paddingTop: '0.5rem' }}>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            {isCompleted && onCreateRx && (
              <button
                type="button"
                onClick={() => onCreateRx(appointment)}
                className="btn btn-accent"
                style={{ padding: '0.45rem 0.9rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
                title="Create Online Prescription"
              >
                <span>📝</span> Create Rx
              </button>
            )}
            {isCompleted && onViewRx && (
              <button
                type="button"
                onClick={() => onViewRx(appointment)}
                className="btn btn-secondary"
                style={{ padding: '0.45rem 0.9rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
                title="View Existing Prescription"
              >
                <span>📄</span> View Rx
              </button>
            )}
          </div>

          <button
            type="button"
            onClick={onClose}
            className="btn btn-primary"
            style={{ padding: '0.45rem 1.4rem', fontSize: '0.85rem', minWidth: '100px' }}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );

  return ReactDOM.createPortal(modalContent, document.body);
}

export default AppointmentDetailsModal;
