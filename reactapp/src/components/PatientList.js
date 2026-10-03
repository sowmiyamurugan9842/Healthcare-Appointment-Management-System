import React, { useState, useEffect, useCallback } from 'react';
import { patientAPI } from '../services/api';
import { Link } from 'react-router-dom';

function PatientList({ user }) {
  const [patients, setPatients] = useState([]);
  const [filteredPatients, setFilteredPatients] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Filters state
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedGender, setSelectedGender] = useState('ALL');
  const [selectedBloodGroup, setSelectedBloodGroup] = useState('ALL');

  const loadPatients = useCallback(async () => {
    setLoading(true);
    setErrorMessage('');
    try {
      const data = await patientAPI.getAll();
      setPatients(data || []);
      setFilteredPatients(data || []);
    } catch (err) {
      console.error('Failed to load patients:', err);
      setErrorMessage(err.response?.data?.message || 'Unauthorized or failed to retrieve patients.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPatients();
  }, [loadPatients]);

  useEffect(() => {
    let result = patients;

    if (searchTerm.trim() !== '') {
      const term = searchTerm.toLowerCase();
      result = result.filter(
        (p) =>
          (p.fullName && p.fullName.toLowerCase().includes(term)) ||
          (p.email && p.email.toLowerCase().includes(term)) ||
          (p.phoneNumber && p.phoneNumber.includes(term)) ||
          (String(p.id).includes(term))
      );
    }

    if (selectedGender !== 'ALL') {
      result = result.filter((p) => p.gender === selectedGender);
    }

    if (selectedBloodGroup !== 'ALL') {
      result = result.filter((p) => p.bloodGroup === selectedBloodGroup);
    }

    setFilteredPatients(result);
  }, [searchTerm, selectedGender, selectedBloodGroup, patients]);

  const isAdmin = user && user.role === 'ADMIN';

  if (!isAdmin) {
    return (
      <div className="card" style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <h2>Access Denied</h2>
        <p style={{ marginTop: '1rem' }}>Only administrators can access the patient registry directory.</p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem' }}>Go to Dashboard</Link>
      </div>
    );
  }

  return (
    <div>
      {/* PAGE HEADER */}
      <div className="page-header">
        <div className="page-title-group">
          <h1>Patient Health Registry</h1>
          <p>Manage, filter, and audit registered electronic medical records.</p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to="/patients/register" className="btn btn-accent">
            + Register New Patient
          </Link>
          <button onClick={loadPatients} disabled={loading} className="btn btn-secondary">
            {loading ? 'Refreshing...' : '↻ Refresh List'}
          </button>
        </div>
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}

      <div className="card">
        {/* TOOLBAR FILTERS */}
        <div className="filter-bar" style={{ justifyContent: 'space-between' }}>
          <div className="filter-group" style={{ flex: '1 1 280px' }}>
            <input
              type="text"
              className="form-control"
              placeholder="Search by name, email, phone, or Patient ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            <div className="filter-group">
              <label className="form-label" style={{ marginBottom: 0, marginRight: '0.25rem' }} htmlFor="genderFilter">
                Gender:
              </label>
              <select
                id="genderFilter"
                className="form-control"
                style={{ width: 'auto', padding: '0.45rem 0.85rem' }}
                value={selectedGender}
                onChange={(e) => setSelectedGender(e.target.value)}
              >
                <option value="ALL">All Genders</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div className="filter-group">
              <label className="form-label" style={{ marginBottom: 0, marginRight: '0.25rem' }} htmlFor="bloodFilter">
                Blood Group:
              </label>
              <select
                id="bloodFilter"
                className="form-control"
                style={{ width: 'auto', padding: '0.45rem 0.85rem' }}
                value={selectedBloodGroup}
                onChange={(e) => setSelectedBloodGroup(e.target.value)}
              >
                <option value="ALL">All Blood Groups</option>
                <option value="A_POSITIVE">A+</option>
                <option value="A_NEGATIVE">A-</option>
                <option value="B_POSITIVE">B+</option>
                <option value="B_NEGATIVE">B-</option>
                <option value="AB_POSITIVE">AB+</option>
                <option value="AB_NEGATIVE">AB-</option>
                <option value="O_POSITIVE">O+</option>
                <option value="O_NEGATIVE">O-</option>
              </select>
            </div>
          </div>
        </div>

        {/* PATIENTS TABLE */}
        {loading ? (
          <div className="empty-state-box">
            <div className="empty-state-icon">⏳</div>
            <div className="empty-state-title">Retrieving Patient Profiles...</div>
          </div>
        ) : filteredPatients.length === 0 ? (
          <div className="empty-state-box">
            <div className="empty-state-icon">👥</div>
            <div className="empty-state-title">No Patient Records Found</div>
            <div className="empty-state-desc">
              {searchTerm || selectedGender !== 'ALL' || selectedBloodGroup !== 'ALL'
                ? 'No patient records match the selected filter criteria.'
                : 'No patients are currently registered in the hospital registry.'}
            </div>
          </div>
        ) : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Patient ID</th>
                  <th>Full Name</th>
                  <th>Email & Phone</th>
                  <th>DOB</th>
                  <th>Gender</th>
                  <th>Blood Group</th>
                  <th>Emergency Contact</th>
                  <th>Residential Address</th>
                </tr>
              </thead>
              <tbody>
                {filteredPatients.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <strong>#{p.id}</strong>
                    </td>
                    <td>
                      <strong style={{ color: 'var(--text-primary)' }}>{p.fullName}</strong>
                    </td>
                    <td>
                      <div style={{ fontSize: '0.85rem' }}>{p.email}</div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{p.phoneNumber}</div>
                    </td>
                    <td>{p.dateOfBirth}</td>
                    <td>
                      <span className="user-tag" style={{ background: 'var(--bg-surface-subtle)', color: 'var(--text-secondary)' }}>
                        {p.gender}
                      </span>
                    </td>
                    <td>
                      <span className="badge badge-completed" style={{ fontSize: '0.75rem' }}>
                        {p.bloodGroup.replace('_POSITIVE', '+').replace('_NEGATIVE', '-')}
                      </span>
                    </td>
                    <td>
                      <span style={{ color: 'var(--primary-dark)', fontWeight: 600 }}>
                        {p.emergencyContact}
                      </span>
                    </td>
                    <td>
                      <div style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={p.address}>
                        {p.address}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default PatientList;
