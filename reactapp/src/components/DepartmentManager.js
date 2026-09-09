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
      setSuccessMessage('Department created successfully!');
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
    <div className="grid-2">
      {/* Creation form (only visible to ADMIN) */}
      {isAdmin ? (
        <div className="card">
          <h2 className="form-title" style={{ textAlign: 'left', marginBottom: '1.5rem' }}>Create Department</h2>
          
          {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
          {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="deptName">Department Name</label>
              <input
                id="deptName"
                type="text"
                className="form-control"
                placeholder="e.g. Cardiology"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
              {errors.name && <span className="form-error-msg">{errors.name}</span>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="deptDesc">Description</label>
              <textarea
                id="deptDesc"
                rows="4"
                className="form-control"
                placeholder="Describe the medical department..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
              {errors.description && <span className="form-error-msg">{errors.description}</span>}
            </div>

            <button type="submit" disabled={loading} className="btn btn-primary" style={{ width: '100%' }}>
              {loading ? 'Creating...' : 'Create Department'}
            </button>
          </form>
        </div>
      ) : (
        <div className="card">
          <h2>Department Management</h2>
          <p style={{ marginTop: '1rem' }}>Only administrators can create new hospital departments. If you need a department created, please contact support.</p>
        </div>
      )}

      {/* List of Departments */}
      <div className="card">
        <div className="flex-between" style={{ marginBottom: '1.5rem' }}>
          <h2>Hospital Departments</h2>
          <button onClick={fetchDepartments} disabled={fetchLoading} className="btn btn-secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}>
            {fetchLoading ? 'Loading...' : 'Refresh'}
          </button>
        </div>

        {departments.length === 0 ? (
          <p style={{ textAlign: 'center', padding: '2rem' }}>No departments registered yet.</p>
        ) : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                {departments.map((dept) => (
                  <tr key={dept.id}>
                    <td><strong>#{dept.id}</strong></td>
                    <td style={{ fontWeight: '600', color: '#ffffff' }}>{dept.departmentName}</td>
                    <td style={{ color: 'var(--text-secondary)' }}>{dept.description}</td>
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

export default DepartmentManager;
