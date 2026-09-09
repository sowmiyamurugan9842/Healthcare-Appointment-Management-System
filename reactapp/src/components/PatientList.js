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

  // Handle filtering
  useEffect(() => {
    let result = patients;

    // Search filter (by name, email, phone)
    if (searchTerm.trim() !== '') {
      const term = searchTerm.toLowerCase();
      result = result.filter(
        (p) =>
          (p.fullName && p.fullName.toLowerCase().includes(term)) ||
          (p.email && p.email.toLowerCase().includes(term)) ||
          (p.phoneNumber && p.phoneNumber.includes(term))
      );
    }

    // Gender filter
    if (selectedGender !== 'ALL') {
      result = result.filter((p) => p.gender === selectedGender);
    }

    // Blood Group filter
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
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem' }}>Go to Home</Link>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="flex-between" style={{ marginBottom: '2rem' }}>
        <div>
          <h1>Patient Registry</h1>
          <p>Manage and filter registered patient medical profiles.</p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to="/patients/register" className="btn btn-accent">
            + Register Patient
          </Link>
          <button onClick={loadPatients} disabled={loading} className="btn btn-secondary">
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}

      {/* Filter Toolbar */}
      <div className="filter-bar">
        <div className="filter-group" style={{ flex: '1 1 300px' }}>
          <input
            type="text"
            className="form-control"
            placeholder="Search by name, email or phone..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        
        <div className="filter-group">
          <label className="form-label" style={{ marginBottom: 0, marginRight: '0.5rem' }} htmlFor="genderFilter">Gender:</label>
          <select
            id="genderFilter"
            className="form-control"
            style={{ width: 'auto', padding: '0.5rem 1rem' }}
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
          <label className="form-label" style={{ marginBottom: 0, marginRight: '0.5rem' }} htmlFor="bloodFilter">Blood Group:</label>
          <select
            id="bloodFilter"
            className="form-control"
            style={{ width: 'auto', padding: '0.5rem 1rem' }}
            value={selectedBloodGroup}
            onChange={(e) => setSelectedBloodGroup(e.target.value)}
          >
            <option value="ALL">All Groups</option>
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

      {/* Patients List Table */}
      {loading ? (
        <p style={{ textAlign: 'center', padding: '2rem' }}>Retrieving patient profiles...</p>
      ) : filteredPatients.length === 0 ? (
        <p style={{ textAlign: 'center', padding: '2rem' }}>No patients found matching the criteria.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Patient ID</th>
                <th>Full Name</th>
                <th>Email</th>
                <th>Phone Number</th>
                <th>DOB</th>
                <th>Gender</th>
                <th>Blood</th>
                <th>Address</th>
                <th>Emergency Contact</th>
              </tr>
            </thead>
            <tbody>
              {filteredPatients.map((p) => (
                <tr key={p.id}>
                  <td><strong>#{p.id}</strong></td>
                  <td style={{ fontWeight: '600', color: '#ffffff' }}>{p.fullName}</td>
                  <td>{p.email}</td>
                  <td>{p.phoneNumber}</td>
                  <td>{p.dateOfBirth}</td>
                  <td>
                    <span className="user-tag" style={{ background: 'rgba(255,255,255,0.05)', color: 'var(--text-secondary)' }}>
                      {p.gender}
                    </span>
                  </td>
                  <td>
                    <span className="user-tag" style={{ background: 'var(--primary-glow)', color: 'var(--primary)' }}>
                      {p.bloodGroup.replace('_POSITIVE', '+').replace('_NEGATIVE', '-')}
                    </span>
                  </td>
                  <td><div style={{ maxWidth: '180px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.address}</div></td>
                  <td style={{ color: 'var(--accent)', fontWeight: '500' }}>{p.emergencyContact}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default PatientList;
