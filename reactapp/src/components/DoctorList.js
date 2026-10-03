import React, { useState, useEffect, useCallback } from 'react';
import { doctorAPI } from '../services/api';
import { Link } from 'react-router-dom';

function DoctorList({ user }) {
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [specializationQuery, setSpecializationQuery] = useState('');

  const loadDoctors = useCallback(async (searchVal = '') => {
    setLoading(true);
    setErrorMessage('');
    try {
      let data;
      if (searchVal.trim() !== '') {
        data = await doctorAPI.searchBySpecialization(searchVal);
      } else {
        data = await doctorAPI.getAll();
      }
      setDoctors(data || []);
    } catch (err) {
      console.error('Failed to load doctors:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to retrieve doctors.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDoctors();
  }, [loadDoctors]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    loadDoctors(specializationQuery);
  };

  const handleClearSearch = () => {
    setSpecializationQuery('');
    loadDoctors('');
  };

  const getDoctorInitials = (name) => {
    if (!name) return 'DR';
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  const isAdmin = user && user.role === 'ADMIN';
  const isPatient = user && user.role === 'PATIENT';

  return (
    <div>
      {/* PAGE HEADER */}
      <div className="page-header">
        <div className="page-title-group">
          <h1>Medical Specialist Directory</h1>
          <p>Explore certified healthcare specialists, check consulting hours and book appointments.</p>
        </div>

        {isAdmin && (
          <Link to="/doctors/register" className="btn btn-accent">
            + Register New Doctor
          </Link>
        )}
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}

      {/* FILTER & SEARCH BAR */}
      <form onSubmit={handleSearchSubmit} className="filter-bar" style={{ marginBottom: '1.75rem' }}>
        <div className="filter-group" style={{ flex: '1 1 340px' }}>
          <input
            type="text"
            className="form-control"
            placeholder="Search by specialization (e.g. Cardiologist, Dermatologist, Pediatrician)..."
            value={specializationQuery}
            onChange={(e) => setSpecializationQuery(e.target.value)}
          />
        </div>
        <button type="submit" disabled={loading} className="btn btn-primary">
          {loading ? 'Searching...' : 'Search Specialists'}
        </button>
        {specializationQuery && (
          <button type="button" onClick={handleClearSearch} className="btn btn-secondary">
            Clear
          </button>
        )}
      </form>

      {/* DOCTORS GRID */}
      {loading ? (
        <div className="empty-state-box">
          <div className="empty-state-icon">⏳</div>
          <div className="empty-state-title">Retrieving Specialist Directory...</div>
          <div className="empty-state-desc">Connecting with hospital registry.</div>
        </div>
      ) : doctors.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-state-icon">🩺</div>
          <div className="empty-state-title">No Specialists Found</div>
          <div className="empty-state-desc">
            {specializationQuery 
              ? `No doctors found matching "${specializationQuery}". Try searching with a different specialty.`
              : 'No doctors are currently registered in the system.'}
          </div>
          {specializationQuery && (
            <button onClick={handleClearSearch} className="btn btn-secondary" style={{ marginTop: '0.75rem' }}>
              Reset Search
            </button>
          )}
        </div>
      ) : (
        <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.5rem' }}>
          {doctors.map((d) => (
            <div key={d.id} className="doctor-card">
              <div className="doctor-card-header">
                <div className="doctor-avatar-circle">
                  {getDoctorInitials(d.fullName)}
                </div>
                <div className="doctor-title-box" style={{ flex: 1 }}>
                  <h3>Dr. {d.fullName}</h3>
                  <span className="doctor-spec-pill">{d.specialization}</span>
                </div>
              </div>

              <div className="doctor-info-list">
                <div className="doctor-info-row">
                  <span className="doctor-info-label">Department:</span>
                  <span className="doctor-info-val" style={{ color: 'var(--primary-dark)' }}>{d.departmentName}</span>
                </div>
                <div className="doctor-info-row">
                  <span className="doctor-info-label">Qualification:</span>
                  <span className="doctor-info-val">{d.qualification}</span>
                </div>
                <div className="doctor-info-row">
                  <span className="doctor-info-label">Experience:</span>
                  <span className="doctor-info-val">{d.experienceYears} Years</span>
                </div>
                <div className="doctor-info-row">
                  <span className="doctor-info-label">Consultation Hours:</span>
                  <span className="doctor-info-val" style={{ color: 'var(--primary-dark)' }}>
                    {d.availableFrom?.substring(0, 5)} - {d.availableTo?.substring(0, 5)}
                  </span>
                </div>
                <div className="doctor-info-row" style={{ marginTop: '0.25rem', paddingTop: '0.45rem', borderTop: '1px dashed var(--border-subtle)' }}>
                  <span className="doctor-info-label">Consultation Fee:</span>
                  <span className="doctor-info-val" style={{ fontSize: '1.1rem', color: 'var(--primary-dark)', fontWeight: 800 }}>
                    ${d.consultationFee?.toFixed(2)}
                  </span>
                </div>
              </div>

              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                <div>📧 {d.email}</div>
                <div>📞 {d.phoneNumber}</div>
              </div>

              {isPatient && (
                <Link
                  to={`/appointments/book?doctorId=${d.id}&doctorName=${encodeURIComponent(d.fullName)}`}
                  className="btn btn-primary"
                  style={{ width: '100%', marginTop: 'auto', padding: '0.6rem' }}
                >
                  📅 Book Appointment
                </Link>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default DoctorList;
