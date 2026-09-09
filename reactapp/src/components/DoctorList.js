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

  const isAdmin = user && user.role === 'ADMIN';

  return (
    <div className="card">
      <div className="flex-between" style={{ marginBottom: '2rem' }}>
        <div>
          <h1>Doctor Directory</h1>
          <p>Find medical specialists and view their availability.</p>
        </div>
        
        {isAdmin && (
          <Link to="/doctors/register" className="btn btn-accent">
            + Register Doctor
          </Link>
        )}
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}

      {/* Specialization Search Bar */}
      <form onSubmit={handleSearchSubmit} className="filter-bar">
        <div className="filter-group" style={{ flex: '1 1 350px' }}>
          <input
            type="text"
            className="form-control"
            placeholder="Search by Specialization (e.g. Cardiologist, Pediatrician)..."
            value={specializationQuery}
            onChange={(e) => setSpecializationQuery(e.target.value)}
          />
        </div>
        <button type="submit" disabled={loading} className="btn btn-primary">
          {loading ? 'Searching...' : 'Search'}
        </button>
        {specializationQuery && (
          <button type="button" onClick={handleClearSearch} className="btn btn-secondary">
            Clear
          </button>
        )}
      </form>

      {/* Doctor Cards Grid */}
      {loading ? (
        <p style={{ textAlign: 'center', padding: '2rem' }}>Retrieving doctor directory...</p>
      ) : doctors.length === 0 ? (
        <p style={{ textAlign: 'center', padding: '2rem' }}>No doctors found.</p>
      ) : (
        <div className="dashboard-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', marginTop: '1.5rem' }}>
          {doctors.map((d) => (
            <div key={d.id} className="card" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem', border: '1px solid var(--panel-border)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <h3 style={{ fontSize: '1.2rem', color: '#ffffff' }}>Dr. {d.fullName}</h3>
                  <span className="user-tag" style={{ background: 'var(--primary-glow)', color: 'var(--primary)', marginTop: '0.25rem', display: 'inline-block' }}>
                    {d.specialization}
                  </span>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Fee</span>
                  <div style={{ fontSize: '1.15rem', fontWeight: '700', color: 'var(--accent)' }}>
                    ${d.consultationFee?.toFixed(2)}
                  </div>
                </div>
              </div>

              <div style={{ borderTop: '1px solid rgba(255,255,255,0.06)', paddingTop: '0.75rem', fontSize: '0.9rem', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>Department:</span>{' '}
                  <strong style={{ color: '#ffffff' }}>{d.departmentName}</strong>
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>Qualification:</span>{' '}
                  <span style={{ color: 'var(--text-primary)' }}>{d.qualification}</span>
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>Experience:</span>{' '}
                  <span style={{ color: 'var(--text-primary)' }}>{d.experienceYears} years</span>
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>Hours:</span>{' '}
                  <span style={{ color: 'var(--warning)', fontWeight: '500' }}>
                    {d.availableFrom?.substring(0, 5)} - {d.availableTo?.substring(0, 5)}
                  </span>
                </div>
              </div>

              <div style={{ borderTop: '1px solid rgba(255,255,255,0.06)', paddingTop: '0.75rem', fontSize: '0.85rem', display: 'flex', flexDirection: 'column', gap: '0.25rem', color: 'var(--text-muted)' }}>
                <div>Email: {d.email}</div>
                <div>Phone: {d.phoneNumber}</div>
                <div>Doctor ID: #{d.id}</div>
              </div>

              {user?.role === 'PATIENT' && (
                <Link
                  to={`/appointments/book?doctorId=${d.id}&doctorName=${encodeURIComponent(d.fullName)}`}
                  className="btn btn-primary"
                  style={{ width: '100%', marginTop: 'auto', padding: '0.5rem' }}
                >
                  Book Appointment
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
