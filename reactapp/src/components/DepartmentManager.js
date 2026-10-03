import React, { useState, useEffect, useCallback } from 'react';
import { departmentAPI } from '../services/api';

function DepartmentManager({ user }) {
  const [departments, setDepartments] = useState([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetchLoading, setFetchLoading] = useState(false);

  const fetchDepartments = useCallback(async () => {
    setFetchLoading(true);
    try {
      const data = await departmentAPI.getAll();
      setDepartments(data || []);
    } catch (error) {
      console.error('Failed to fetch departments:', error);
      setErrorMessage('Could not load departments from backend.');
    } finally {
      setFetchLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDepartments();
  }, [fetchDepartments]);

  const validate = () => {
    const tempErrors = {};
    if (!name.trim()) {
      tempErrors.name = 'Department name is required';
    } else if (name.length > 100) {
      tempErrors.name = 'Department name cannot exceed 100 characters';
    }

    if (!description.trim()) {
      tempErrors.description = 'Description is required';
    } else if (description.length > 500) {
      tempErrors.description = 'Description cannot exceed 500 characters';
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
      await departmentAPI.create({
        departmentName: name,
        description
      });
      setSuccessMessage('Medical department created successfully!');
      setName('');
      setDescription('');
      fetchDepartments();
    } catch (error) {
      console.error('Failed to create department:', error);
      const msg = error.response?.data?.message || error.response?.data || 'Failed to create department';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error creating department.');
    } finally {
      setLoading(false);
    }
  };

  const isAdmin = user && user.role === 'ADMIN';

  return (
    <div>
      <div className="page-header">
        <div className="page-title-group">
          <h1>Hospital Departments</h1>
          <p>Organize clinical divisions, specialties, and medical facilities.</p>
        </div>

        <button onClick={fetchDepartments} disabled={fetchLoading} className="btn btn-secondary">
          {fetchLoading ? 'Refreshing...' : '↻ Refresh List'}
        </button>
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      <div className="stats-grid" style={{ gridTemplateColumns: isAdmin ? '1.1fr 1.9fr' : '1fr', gap: '1.5rem', alignItems: 'flex-start' }}>
        {/* CREATION FORM (ADMIN ONLY) */}
        {isAdmin && (
          <div className="card">
            <h2 style={{ fontSize: '1.25rem', marginBottom: '1.25rem' }}>Add New Department</h2>

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label" htmlFor="deptName">
                  Department Name <span style={{ color: 'var(--danger-text)' }}>*</span>
                </label>
                <input
                  id="deptName"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Cardiology, Orthopedics, Pediatrics"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
                {errors.name && <span className="form-error-msg">{errors.name}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="deptDesc">
                  Department Description <span style={{ color: 'var(--danger-text)' }}>*</span>
                </label>
                <textarea
                  id="deptDesc"
                  rows="4"
                  className="form-control"
                  placeholder="Describe the medical scope, equipment, and clinical focus..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
                {errors.description && <span className="form-error-msg">{errors.description}</span>}
              </div>

              <button type="submit" disabled={loading} className="btn btn-primary" style={{ width: '100%', padding: '0.7rem' }}>
                {loading ? 'Creating...' : '+ Create Department'}
              </button>
            </form>
          </div>
        )}

        {/* LIST OF DEPARTMENTS */}
        <div className="card">
          <div className="section-header">
            <h2>Registered Departments ({departments.length})</h2>
          </div>

          {fetchLoading ? (
            <div className="empty-state-box">
              <div className="empty-state-icon">⏳</div>
              <div className="empty-state-title">Loading Departments...</div>
            </div>
          ) : departments.length === 0 ? (
            <div className="empty-state-box">
              <div className="empty-state-icon">🏢</div>
              <div className="empty-state-title">No Departments Created Yet</div>
              <div className="empty-state-desc">Use the form to create clinical departments for doctor assignments.</div>
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Dept ID</th>
                    <th>Department Name</th>
                    <th>Clinical Scope & Description</th>
                  </tr>
                </thead>
                <tbody>
                  {departments.map((dept) => (
                    <tr key={dept.id}>
                      <td><strong>#{dept.id}</strong></td>
                      <td>
                        <strong style={{ color: 'var(--primary-dark)' }}>{dept.departmentName}</strong>
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>{dept.description}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default DepartmentManager;
