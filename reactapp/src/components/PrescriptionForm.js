import React, { useState } from 'react';
import ReactDOM from 'react-dom';
import { prescriptionAPI } from '../services/api';

function PrescriptionForm({ appointment, onSaved, onCancel }) {
  const [diagnosis, setDiagnosis] = useState('');
  const [doctorAdvice, setDoctorAdvice] = useState('');
  const [followUpDate, setFollowUpDate] = useState('');
  const [followUpTime, setFollowUpTime] = useState('');
  const [followUpNotes, setFollowUpNotes] = useState('');
  const [additionalNotes, setAdditionalNotes] = useState('');

  const [medicines, setMedicines] = useState([
    {
      medicineName: '',
      dosage: '',
      frequency: '',
      duration: '',
      instructions: 'After food'
    }
  ]);

  const [errors, setErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);

  if (!appointment) return null;

  const handleAddMedicine = () => {
    setMedicines((prev) => [
      ...prev,
      {
        medicineName: '',
        dosage: '',
        frequency: '',
        duration: '',
        instructions: 'After food'
      }
    ]);
  };

  const handleRemoveMedicine = (index) => {
    if (medicines.length === 1) {
      setErrorMessage('Prescription must contain at least one medication.');
      return;
    }
    setMedicines((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleMedicineChange = (index, field, value) => {
    setMedicines((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const validate = () => {
    const tempErrors = {};
    if (!diagnosis.trim()) {
      tempErrors.diagnosis = 'Clinical diagnosis is required.';
    }

    if (!medicines || medicines.length === 0) {
      tempErrors.medicines = 'Please add at least one medication.';
    } else {
      const medErrors = [];
      medicines.forEach((med, i) => {
        const itemError = {};
        if (!med.medicineName.trim()) itemError.medicineName = 'Medicine name is required';
        if (!med.dosage.trim()) itemError.dosage = 'Dosage is required (e.g. 500mg)';
        if (!med.frequency.trim()) itemError.frequency = 'Frequency is required (e.g. 2 times a day)';
        if (!med.duration.trim()) itemError.duration = 'Duration is required (e.g. 5 days)';
        if (Object.keys(itemError).length > 0) {
          medErrors[i] = itemError;
        }
      });
      if (medErrors.length > 0) {
        tempErrors.medItems = medErrors;
      }
    }

    setErrors(tempErrors);
    return Object.keys(tempErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!validate()) {
      setErrorMessage('Please resolve validation errors in the prescription form.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        appointmentId: appointment.id,
        diagnosis: diagnosis.trim(),
        doctorAdvice: doctorAdvice.trim(),
        additionalNotes: additionalNotes.trim(),
        followUpDate: followUpDate || null,
        followUpTime: followUpTime ? (followUpTime.length === 5 ? `${followUpTime}:00` : followUpTime) : null,
        followUpNotes: followUpNotes.trim() || null,
        medicines: medicines.map((m) => ({
          medicineName: m.medicineName.trim(),
          dosage: m.dosage.trim(),
          frequency: m.frequency.trim(),
          duration: m.duration.trim(),
          instructions: m.instructions.trim()
        }))
      };

      const saved = await prescriptionAPI.create(payload);
      setSuccessMessage('Prescription successfully created and published to patient record!');
      setTimeout(() => {
        if (onSaved) onSaved(saved);
      }, 1000);
    } catch (err) {
      console.error('Prescription save error:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to create prescription';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error saving prescription. Verify appointment status.');
    } finally {
      setLoading(false);
    }
  };

  const apptTimeFormatted = appointment.appointmentTime
    ? (typeof appointment.appointmentTime === 'string'
        ? appointment.appointmentTime.substring(0, 5)
        : String(appointment.appointmentTime))
    : '';

  const modalContent = (
    <div className="prescription-modal-backdrop" onClick={onCancel}>
      <div className="prescription-form-modal-content" onClick={(e) => e.stopPropagation()}>
        {/* HEADER */}
        <div className="form-modal-header">
          <div>
            <h2>Create Online Medical Prescription</h2>
            <p style={{ margin: 0, fontSize: '0.85rem' }}>
              Issue digital clinical prescription for Appointment #{appointment.id || 'N/A'}
            </p>
          </div>
          {onCancel && (
            <button type="button" onClick={onCancel} className="close-btn" aria-label="Close prescription form">
              ✕
            </button>
          )}
        </div>

        {/* PATIENT CONTEXT BANNER */}
        <div className="patient-context-banner">
          <div className="context-item">
            <span className="context-label">Patient:</span>
            <strong>{appointment.patientName || 'Patient'}</strong>
            {appointment.patientId && <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>ID: #{appointment.patientId}</span>}
          </div>
          <div className="context-item">
            <span className="context-label">Doctor:</span>
            <strong>
              {appointment.doctorName
                ? (appointment.doctorName.startsWith('Dr. ') ? appointment.doctorName : `Dr. ${appointment.doctorName}`)
                : 'Attending Physician'}
            </strong>
          </div>
          <div className="context-item">
            <span className="context-label">Appt Date:</span>
            <strong>
              {appointment.appointmentDate || 'Today'}
              {apptTimeFormatted && ` (${apptTimeFormatted})`}
            </strong>
          </div>
          <div className="context-item">
            <span className="context-label">Status:</span>
            <span className="badge badge-completed">COMPLETED</span>
          </div>
        </div>

        {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
        {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

        <form onSubmit={handleSubmit}>
          {/* CLINICAL DIAGNOSIS */}
          <div className="form-group">
            <label className="form-label" htmlFor="diagnosisInput">
              Clinical Diagnosis & Findings <span style={{ color: 'var(--danger-text)' }}>*</span>
            </label>
            <textarea
              id="diagnosisInput"
              rows={2}
              className="form-control"
              placeholder="e.g. Acute Viral Bronchitis, Mild dehydration, Allergic Rhinitis"
              value={diagnosis}
              onChange={(e) => setDiagnosis(e.target.value)}
            />
            {errors.diagnosis && <span className="form-error-msg">{errors.diagnosis}</span>}
          </div>

          {/* MEDICATIONS LIST */}
          <div className="medicines-editor-section">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <div>
                <h4 style={{ margin: 0, color: 'var(--primary-dark)', fontSize: '1rem' }}>
                  💊 Prescribed Medications (Rx)
                </h4>
                <p style={{ margin: 0, fontSize: '0.8rem' }}>Specify medicine name, dosage, frequency, and duration.</p>
              </div>
              <button
                type="button"
                onClick={handleAddMedicine}
                className="btn btn-primary"
                style={{ padding: '0.35rem 0.75rem', fontSize: '0.85rem' }}
              >
                + Add Medication
              </button>
            </div>

            {errors.medicines && <div className="alert alert-danger" style={{ padding: '0.5rem' }}>{errors.medicines}</div>}

            <div className="medicines-input-list">
              {medicines.map((med, index) => {
                const itemErr = errors.medItems && errors.medItems[index] ? errors.medItems[index] : {};
                return (
                  <div key={index} className="medicine-row-card">
                    <div className="med-row-header">
                      <span className="med-number-tag">Medication #{index + 1}</span>
                      {medicines.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleRemoveMedicine(index)}
                          className="btn btn-danger"
                          style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                          title="Remove medication"
                        >
                          ✕ Remove
                        </button>
                      )}
                    </div>

                    <div className="medicine-inputs-grid">
                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="form-label" style={{ fontSize: '0.78rem' }}>Medicine Name *</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. Amoxicillin"
                          value={med.medicineName}
                          onChange={(e) => handleMedicineChange(index, 'medicineName', e.target.value)}
                        />
                        {itemErr.medicineName && <span className="form-error-msg">{itemErr.medicineName}</span>}
                      </div>

                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="form-label" style={{ fontSize: '0.78rem' }}>Dosage *</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. 500mg"
                          value={med.dosage}
                          onChange={(e) => handleMedicineChange(index, 'dosage', e.target.value)}
                        />
                        {itemErr.dosage && <span className="form-error-msg">{itemErr.dosage}</span>}
                      </div>

                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="form-label" style={{ fontSize: '0.78rem' }}>Frequency *</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. 3 times daily"
                          value={med.frequency}
                          onChange={(e) => handleMedicineChange(index, 'frequency', e.target.value)}
                        />
                        {itemErr.frequency && <span className="form-error-msg">{itemErr.frequency}</span>}
                      </div>

                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="form-label" style={{ fontSize: '0.78rem' }}>Duration *</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. 5 days"
                          value={med.duration}
                          onChange={(e) => handleMedicineChange(index, 'duration', e.target.value)}
                        />
                        {itemErr.duration && <span className="form-error-msg">{itemErr.duration}</span>}
                      </div>

                      <div className="form-group" style={{ marginBottom: 0, gridColumn: 'span 4' }}>
                        <label className="form-label" style={{ fontSize: '0.78rem' }}>Instructions / Administration</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. Take with food, drink plenty of water"
                          value={med.instructions}
                          onChange={(e) => handleMedicineChange(index, 'instructions', e.target.value)}
                        />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* DOCTOR'S ADVICE */}
          <div className="form-group">
            <label className="form-label" htmlFor="doctorAdviceInput">Doctor's Advice / Care Plan</label>
            <textarea
              id="doctorAdviceInput"
              rows={2}
              className="form-control"
              placeholder="e.g. Complete full antibiotic course, rest, avoid strenuous exercise."
              value={doctorAdvice}
              onChange={(e) => setDoctorAdvice(e.target.value)}
            />
          </div>

          {/* FOLLOW-UP DETAILS (OPTIONAL) */}
          <div className="form-section-header" style={{ marginTop: '1.25rem', marginBottom: '0.75rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span>🔄</span>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                Follow-Up Consultation (Optional)
              </h3>
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: '0.25rem 0 0 0' }}>
              Set a scheduled follow-up review. The system will automatically notify the patient prior to the date.
            </p>
          </div>

          <div className="form-control-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-group">
              <label className="form-label" htmlFor="followUpDateInput">📅 Follow-Up Date</label>
              <input
                id="followUpDateInput"
                type="date"
                className="form-control"
                min={new Date().toISOString().split('T')[0]}
                value={followUpDate}
                onChange={(e) => setFollowUpDate(e.target.value)}
              />
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Target consultation review date</span>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="followUpTimeInput">⏰ Follow-Up Time</label>
              <input
                id="followUpTimeInput"
                type="time"
                className="form-control"
                value={followUpTime}
                onChange={(e) => setFollowUpTime(e.target.value)}
              />
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Target appointment time (e.g. 10:00 AM)</span>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="followUpNotesInput">📝 Follow-Up Instructions / Goals</label>
            <input
              id="followUpNotesInput"
              type="text"
              className="form-control"
              placeholder="e.g. Review blood pressure, check blood test results, adjust medication dosage."
              value={followUpNotes}
              onChange={(e) => setFollowUpNotes(e.target.value)}
            />
          </div>

          {/* ADDITIONAL NOTES */}
          <div className="form-group">
            <label className="form-label" htmlFor="additionalNotesInput">Additional Notes / Warnings</label>
            <textarea
              id="additionalNotesInput"
              rows={2}
              className="form-control"
              placeholder="e.g. In case of high fever or allergic reaction, contact emergency care immediately."
              value={additionalNotes}
              onChange={(e) => setAdditionalNotes(e.target.value)}
            />
          </div>

          {/* ACTIONS */}
          <div className="form-actions-row">
            {onCancel && (
              <button type="button" onClick={onCancel} disabled={loading} className="btn btn-secondary">
                Cancel
              </button>
            )}
            <button type="submit" disabled={loading} className="btn btn-primary" style={{ minWidth: '180px' }}>
              {loading ? 'Saving Prescription...' : '✓ Save & Issue Prescription'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );

  return ReactDOM.createPortal(modalContent, document.body);
}

export default PrescriptionForm;
