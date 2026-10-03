import React, { useState } from 'react';
import ReactDOM from 'react-dom';
import { prescriptionAPI } from '../services/api';

function PrescriptionView({ prescription, onClose, user }) {
  const [downloading, setDownloading] = useState(false);
  const [sendingWhatsApp, setSendingWhatsApp] = useState(false);
  const [actionMessage, setActionMessage] = useState('');
  const [actionError, setActionError] = useState('');
  const [whatsappStatus, setWhatsappStatus] = useState(prescription?.whatsappStatus || 'PENDING');

  if (!prescription) return null;

  const isDoctorOrAdmin = user && (user.role === 'DOCTOR' || user.role === 'ADMIN');

  const handlePrint = () => {
    window.print();
  };

  const handleDownloadPdf = async () => {
    if (!prescription.id) return;
    setDownloading(true);
    setActionError('');
    setActionMessage('');
    try {
      await prescriptionAPI.downloadPdf(prescription.id);
      setActionMessage('PDF prescription successfully downloaded.');
    } catch (err) {
      console.error('PDF download error:', err);
      setActionError(err.response?.data?.message || 'Failed to download prescription PDF.');
    } finally {
      setDownloading(false);
    }
  };

  const handleSendWhatsApp = async () => {
    if (!prescription.id) return;
    setSendingWhatsApp(true);
    setActionError('');
    setActionMessage('');
    try {
      const res = await prescriptionAPI.sendWhatsApp(prescription.id);
      setWhatsappStatus(res.deliveryStatus);
      if (res.deliveryStatus === 'SENT') {
        setActionMessage('✓ ' + (res.message || 'Prescription dispatched to patient WhatsApp!'));
      } else if (res.deliveryStatus === 'NOT_CONFIGURED') {
        setActionMessage('ℹ️ ' + (res.message || 'WhatsApp credentials not configured in environment. PDF is stored and ready for in-app download.'));
      } else {
        setActionError('⚠️ WhatsApp delivery status: ' + (res.message || 'Failed to send message.'));
      }
    } catch (err) {
      console.error('WhatsApp send error:', err);
      setActionError(err.response?.data?.message || 'Failed to trigger WhatsApp delivery.');
      setWhatsappStatus('FAILED');
    } finally {
      setSendingWhatsApp(false);
    }
  };

  const medicines = prescription.medicines && prescription.medicines.length > 0
    ? prescription.medicines
    : (prescription.medications ? [{
        medicineName: prescription.medications,
        dosage: prescription.dosageInstructions || 'As prescribed',
        frequency: 'As directed',
        duration: 'As instructed',
        instructions: prescription.additionalNotes || 'Follow instructions'
      }] : []);

  const apptTimeFormatted = prescription.appointmentTime
    ? (typeof prescription.appointmentTime === 'string'
        ? prescription.appointmentTime.substring(0, 5)
        : String(prescription.appointmentTime))
    : '';

  const getWhatsAppBadge = () => {
    const s = (whatsappStatus || 'PENDING').toUpperCase();
    if (s === 'SENT') return { label: '✓ WhatsApp: Sent', cls: 'badge-approved' };
    if (s === 'FAILED') return { label: '⚠️ WhatsApp: Failed', cls: 'badge-rejected' };
    if (s === 'NOT_CONFIGURED') return { label: 'ℹ️ WhatsApp: Unconfigured', cls: 'badge-expired' };
    return { label: '⏳ WhatsApp: Pending', cls: 'badge-pending' };
  };

  const waBadge = getWhatsAppBadge();

  const modalContent = (
    <div className="prescription-modal-backdrop" onClick={onClose}>
      <div className="prescription-modal-content" onClick={(e) => e.stopPropagation()}>
        {/* ACTION BAR (Hidden in print) */}
        <div className="prescription-modal-actions no-print" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
            {/* DOWNLOAD PDF */}
            <button
              onClick={handleDownloadPdf}
              disabled={downloading}
              className="btn btn-primary"
              style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', backgroundColor: '#075E66', borderColor: '#075E66' }}
            >
              <span>{downloading ? '⏳' : '📥'}</span> {downloading ? 'Downloading...' : 'Download PDF'}
            </button>

            {/* SEND / RETRY WHATSAPP FOR DOCTOR & ADMIN */}
            {isDoctorOrAdmin && (
              <button
                onClick={handleSendWhatsApp}
                disabled={sendingWhatsApp}
                className="btn btn-secondary"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                  borderColor: whatsappStatus === 'FAILED' ? '#ef4444' : '#10b981',
                  color: whatsappStatus === 'FAILED' ? '#b91c1c' : '#047857'
                }}
              >
                <span>{sendingWhatsApp ? '⏳' : '📱'}</span>
                {sendingWhatsApp
                  ? 'Dispatching...'
                  : whatsappStatus === 'FAILED'
                  ? 'Retry WhatsApp'
                  : 'Send WhatsApp'}
              </button>
            )}

            {/* PRINT */}
            <button onClick={handlePrint} className="btn btn-secondary" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <span>🖨️</span> Print
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <span className={`badge ${waBadge.cls}`} title={prescription.whatsappError || ''}>
              {waBadge.label}
            </span>
            {onClose && (
              <button onClick={onClose} className="btn btn-secondary" style={{ padding: '0.4rem 0.8rem' }}>
                Close
              </button>
            )}
          </div>
        </div>

        {actionMessage && <div className="alert alert-success no-print" style={{ margin: '0.75rem 1.5rem 0' }}>{actionMessage}</div>}
        {actionError && <div className="alert alert-danger no-print" style={{ margin: '0.75rem 1.5rem 0' }}>{actionError}</div>}

        {/* PRINTABLE DIGITAL PRESCRIPTION SHEET */}
        <div className="digital-prescription-card print-area">
          {/* HEADER */}
          <div className="prescription-header">
            <div className="prescription-clinic-brand">
              <span className="clinic-icon">⚕</span>
              <div>
                <h2>CarePortal Healthcare System</h2>
                <p className="clinic-tagline">Clinical Care & Telehealth Consultation</p>
              </div>
            </div>
            <div className="prescription-badge-box">
              <span className="rx-symbol">℞</span>
              <div className="prescription-id-tag">
                <strong>Prescription ID:</strong> #{prescription.id || 'N/A'}
              </div>
            </div>
          </div>

          {/* DOCTOR & PATIENT INFO GRID */}
          <div className="prescription-info-grid">
            <div className="info-block">
              <span className="info-label">Doctor Information</span>
              <div className="info-value doc-name">
                {prescription.doctorName?.startsWith('Dr. ') ? prescription.doctorName : `Dr. ${prescription.doctorName || 'Specialist'}`}
              </div>
              {prescription.doctorSpecialization && (
                <div className="info-subtext">{prescription.doctorSpecialization}</div>
              )}
            </div>

            <div className="info-block">
              <span className="info-label">Patient Information</span>
              <div className="info-value">{prescription.patientName || 'Patient'}</div>
              {prescription.patientId && (
                <div className="info-subtext">Patient ID: #{prescription.patientId}</div>
              )}
            </div>

            <div className="info-block">
              <span className="info-label">Consultation Date</span>
              <div className="info-value">{prescription.appointmentDate || 'Today'}</div>
              {apptTimeFormatted && (
                <div className="info-subtext">Time: {apptTimeFormatted}</div>
              )}
            </div>

            <div className="info-block">
              <span className="info-label">Appointment Reference</span>
              <div className="info-value">Appt #{prescription.appointmentId || 'N/A'}</div>
              <div className="info-subtext">Status: Completed</div>
            </div>
          </div>

          {/* DIAGNOSIS */}
          <div className="prescription-section">
            <div className="section-title">
              <span>🩺</span> Diagnosis & Clinical Findings
            </div>
            <div className="diagnosis-box">
              {prescription.diagnosis}
            </div>
          </div>

          {/* MEDICATIONS / RX TABLE */}
          <div className="prescription-section">
            <div className="section-title">
              <span>💊</span> Prescribed Medications (Rx)
            </div>

            <div className="table-container">
              <table className="prescription-medicines-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Medicine Name</th>
                    <th>Dosage</th>
                    <th>Frequency</th>
                    <th>Duration</th>
                    <th>Instructions</th>
                  </tr>
                </thead>
                <tbody>
                  {medicines.map((med, idx) => (
                    <tr key={idx}>
                      <td><strong>{idx + 1}</strong></td>
                      <td><strong style={{ color: 'var(--primary-dark)' }}>{med.medicineName}</strong></td>
                      <td><span className="med-pill">{med.dosage}</span></td>
                      <td>{med.frequency}</td>
                      <td>{med.duration}</td>
                      <td style={{ color: 'var(--text-secondary)' }}>{med.instructions || 'After food'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* DOCTOR'S ADVICE */}
          {(prescription.doctorAdvice || prescription.dosageInstructions) && (
            <div className="prescription-section">
              <div className="section-title">
                <span>📋</span> Doctor's Advice & Care Plan
              </div>
              <div className="advice-box">
                {prescription.doctorAdvice || prescription.dosageInstructions}
              </div>
            </div>
          )}

          {/* ADDITIONAL NOTES */}
          {prescription.additionalNotes && (
            <div className="prescription-section">
              <div className="section-title">
                <span>📝</span> Additional Notes
              </div>
              <div className="notes-box">
                {prescription.additionalNotes}
              </div>
            </div>
          )}

          {/* FOOTER: FOLLOW-UP & SIGNATURE */}
          <div className="prescription-footer">
            <div className="follow-up-card" style={{ flex: 1.2, minWidth: '240px' }}>
              <span className="info-label">Follow-Up Consultation Details</span>
              {prescription.followUpDate || prescription.nextVisitDate ? (
                <div style={{ marginTop: '0.35rem' }}>
                  <div className="follow-up-date" style={{ fontWeight: 600, color: 'var(--primary-dark)', fontSize: '0.95rem' }}>
                    📅 Date: {prescription.followUpDate || prescription.nextVisitDate}
                  </div>
                  {prescription.followUpTime && (
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-primary)', marginTop: '0.2rem' }}>
                      ⏰ Time: {prescription.followUpTime.substring(0, 5)}
                    </div>
                  )}
                  {prescription.followUpNotes && (
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: '0.25rem', fontStyle: 'italic' }}>
                      📝 Notes: {prescription.followUpNotes}
                    </div>
                  )}
                </div>
              ) : (
                <div className="follow-up-date" style={{ color: 'var(--text-muted)', fontSize: '0.85rem', fontStyle: 'italic', marginTop: '0.35rem' }}>
                  No follow-up scheduled.
                </div>
              )}
            </div>

            <div className="doctor-signature-box">
              <div className="signature-line" />
              <div className="signature-doctor-name">
                {prescription.doctorName?.startsWith('Dr. ') ? prescription.doctorName : `Dr. ${prescription.doctorName || 'Attending Physician'}`}
              </div>
              <div className="signature-seal">✓ Verified Digital Prescription</div>
            </div>
          </div>

          <div className="prescription-bottom-notice">
            CarePortal Telehealth System • Valid digital clinical record • Generated on {new Date().toLocaleDateString()}
          </div>
        </div>
      </div>
    </div>
  );

  return ReactDOM.createPortal(modalContent, document.body);
}

export default PrescriptionView;
