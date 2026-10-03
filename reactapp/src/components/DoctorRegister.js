import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI, doctorAPI, departmentAPI } from '../services/api';

function DoctorRegister() {
  const [useExistingUser, setUseExistingUser] = useState(false);
  const [departments, setDepartments] = useState([]);
  const [fetchLoading, setFetchLoading] = useState(false);

  // User account state
  const [userForm, setUserForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phoneNumber: '',
    role: 'DOCTOR'
  });

  // Profile details state
  const [profileForm, setProfileForm] = useState({
    userId: '',
    departmentId: '',
    qualification: '',
    specialization: '',
    experienceYears: '',
    consultationFee: '',
    availableFrom: '09:00',
    availableTo: '17:00'
  });

  const [errors, setErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const loadDepartments = async () => {
      setFetchLoading(true);
      try {
        const data = await departmentAPI.getAll();
        setDepartments(data || []);
        if (data && data.length > 0) {
          setProfileForm((prev) => ({ ...prev, departmentId: data[0].id }));
        }
      } catch (err) {
        console.error('Failed to load departments:', err);
        setErrorMessage('Failed to load departments. Please create a department first.');
      } finally {
        setFetchLoading(false);
      }
    };
    loadDepartments();
  }, []);

  const validate = () => {
    const tempErrors = {};

    if (!useExistingUser) {
      if (!userForm.firstName.trim()) tempErrors.firstName = 'First name is required';
      if (!userForm.lastName.trim()) tempErrors.lastName = 'Last name is required';

      if (!userForm.email) {
        tempErrors.email = 'Email is required';
      } else if (!/\S+@\S+\.\S+/.test(userForm.email)) {
        tempErrors.email = 'Valid email is required';
      }

      if (!userForm.password) {
        tempErrors.password = 'Password is required';
      } else if (userForm.password.length < 6) {
        tempErrors.password = 'Password must be at least 6 characters';
      }

      const digitsOnly = userForm.phoneNumber.replace(/\D/g, '');
      if (!userForm.phoneNumber) {
        tempErrors.phoneNumber = 'Phone number is required';
      } else if (digitsOnly.length !== 10) {
        tempErrors.phoneNumber = 'Phone number must be exactly 10 digits';
      }
    } else {
      if (!profileForm.userId) {
        tempErrors.userId = 'User ID is required';
      }
    }

    if (!profileForm.departmentId) {
      tempErrors.departmentId = 'Department is required. Create one if needed.';
    }

    if (!profileForm.qualification.trim()) {
      tempErrors.qualification = 'Qualification is required';
    }

    if (!profileForm.specialization.trim()) {
      tempErrors.specialization = 'Specialization is required';
    }

    if (profileForm.experienceYears === '' || Number(profileForm.experienceYears) < 0) {
      tempErrors.experienceYears = 'Experience years must be 0 or more';
    }

    if (profileForm.consultationFee === '' || Number(profileForm.consultationFee) < 0) {
      tempErrors.consultationFee = 'Consultation fee must be 0 or more';
    }

    if (!profileForm.availableFrom) {
      tempErrors.availableFrom = 'Availability start time is required';
    }

    if (!profileForm.availableTo) {
      tempErrors.availableTo = 'Availability end time is required';
    }

    setErrors(tempErrors);
    return Object.keys(tempErrors).length === 0;
  };

  const handleUserChange = (e) => {
    const { name, value } = e.target;
    setUserForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!validate()) return;

    setLoading(true);
    try {
      let finalUserId = profileForm.userId;

      if (!useExistingUser) {
        const userDigitsPhone = userForm.phoneNumber.replace(/\D/g, '');
        const userRes = await authAPI.register({
          ...userForm,
          phoneNumber: userDigitsPhone
        });
        finalUserId = userRes.id;
      }

      const formatTime = (timeStr) => {
        if (timeStr && timeStr.split(':').length === 2) {
          return `${timeStr}:00`;
        }
        return timeStr;
      };

      await doctorAPI.create({
        userId: finalUserId,
        departmentId: Number(profileForm.departmentId),
        qualification: profileForm.qualification,
        specialization: profileForm.specialization,
        experienceYears: Number(profileForm.experienceYears),
        consultationFee: Number(profileForm.consultationFee),
        availableFrom: formatTime(profileForm.availableFrom),
        availableTo: formatTime(profileForm.availableTo)
      });

      setSuccessMessage('Doctor professional profile registered successfully!');
      setTimeout(() => {
        navigate('/doctors');
      }, 1500);
    } catch (error) {
      console.error('Failed to create doctor profile:', error);
      const msg = error.response?.data?.message || error.response?.data || 'Failed to create doctor profile';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error processing doctor registration.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '750px', margin: '0 auto' }}>
      <div className="page-header">
        <div className="page-title-group">
          <h1>Register Medical Doctor</h1>
          <p>Create credentials and assign clinical departments to new hospital physicians.</p>
        </div>
      </div>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      <div className="card">
        {/* MODE TOGGLE */}
        <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.75rem', justifyContent: 'center' }}>
          <button
            type="button"
            onClick={() => setUseExistingUser(false)}
            className={`btn ${!useExistingUser ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '0.45rem 1.25rem' }}
          >
            Create New Doctor User
          </button>
          <button
            type="button"
            onClick={() => setUseExistingUser(true)}
            className={`btn ${useExistingUser ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '0.45rem 1.25rem' }}
          >
            Link Existing User ID
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          {/* SECTION 1: USER ACCOUNT */}
          {!useExistingUser ? (
            <div style={{ marginBottom: '1.75rem' }}>
              <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: 'var(--primary-dark)', paddingBottom: '0.4rem', borderBottom: '1px solid var(--border-subtle)' }}>
                1. Account Credentials & Contact
              </h3>

              <div className="form-control-row">
                <div className="form-group">
                  <label className="form-label" htmlFor="firstName">First Name *</label>
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    className="form-control"
                    placeholder="e.g. Sarah"
                    value={userForm.firstName}
                    onChange={handleUserChange}
                  />
                  {errors.firstName && <span className="form-error-msg">{errors.firstName}</span>}
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="lastName">Last Name *</label>
                  <input
                    id="lastName"
                    name="lastName"
                    type="text"
                    className="form-control"
                    placeholder="e.g. Jenkins"
                    value={userForm.lastName}
                    onChange={handleUserChange}
                  />
                  {errors.lastName && <span className="form-error-msg">{errors.lastName}</span>}
                </div>
              </div>

              <div className="form-control-row">
                <div className="form-group">
                  <label className="form-label" htmlFor="email">Email Address *</label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    className="form-control"
                    placeholder="e.g. dr.jenkins@hospital.com"
                    value={userForm.email}
                    onChange={handleUserChange}
                  />
                  {errors.email && <span className="form-error-msg">{errors.email}</span>}
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="password">Initial Password *</label>
                  <input
                    id="password"
                    name="password"
                    type="password"
                    className="form-control"
                    placeholder="Min 6 characters"
                    value={userForm.password}
                    onChange={handleUserChange}
                  />
                  {errors.password && <span className="form-error-msg">{errors.password}</span>}
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="phoneNumber">Phone Number (10 digits) *</label>
                <input
                  id="phoneNumber"
                  name="phoneNumber"
                  type="tel"
                  className="form-control"
                  placeholder="9876543210"
                  value={userForm.phoneNumber}
                  onChange={handleUserChange}
                />
                {errors.phoneNumber && <span className="form-error-msg">{errors.phoneNumber}</span>}
              </div>
            </div>
          ) : (
            <div className="form-group" style={{ marginBottom: '1.75rem' }}>
              <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: 'var(--primary-dark)', paddingBottom: '0.4rem', borderBottom: '1px solid var(--border-subtle)' }}>
                1. Link Existing User Record
              </h3>
              <label className="form-label" htmlFor="userId">User ID Reference *</label>
              <input
                id="userId"
                name="userId"
                type="number"
                className="form-control"
                placeholder="e.g. 10"
                value={profileForm.userId}
                onChange={handleProfileChange}
              />
              {errors.userId && <span className="form-error-msg">{errors.userId}</span>}
            </div>
          )}

          {/* SECTION 2: PROFESSIONAL PROFILE */}
          <div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: 'var(--primary-dark)', paddingBottom: '0.4rem', borderBottom: '1px solid var(--border-subtle)' }}>
              2. Professional Profile & Practice
            </h3>

            <div className="form-control-row">
              <div className="form-group">
                <label className="form-label" htmlFor="departmentId">Department *</label>
                {fetchLoading ? (
                  <span style={{ fontSize: '0.85rem' }}>Loading departments...</span>
                ) : departments.length === 0 ? (
                  <span className="form-error-msg">No departments available. Create one first!</span>
                ) : (
                  <select
                    id="departmentId"
                    name="departmentId"
                    className="form-control"
                    value={profileForm.departmentId}
                    onChange={handleProfileChange}
                  >
                    {departments.map((dept) => (
                      <option key={dept.id} value={dept.id}>
                        {dept.departmentName}
                      </option>
                    ))}
                  </select>
                )}
                {errors.departmentId && <span className="form-error-msg">{errors.departmentId}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="specialization">Specialization *</label>
                <input
                  id="specialization"
                  name="specialization"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Cardiologist, Neurologist"
                  value={profileForm.specialization}
                  onChange={handleProfileChange}
                />
                {errors.specialization && <span className="form-error-msg">{errors.specialization}</span>}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="qualification">Medical Qualifications *</label>
              <input
                id="qualification"
                name="qualification"
                type="text"
                className="form-control"
                placeholder="e.g. MD, MBBS, FACC"
                value={profileForm.qualification}
                onChange={handleProfileChange}
              />
              {errors.qualification && <span className="form-error-msg">{errors.qualification}</span>}
            </div>

            <div className="form-control-row">
              <div className="form-group">
                <label className="form-label" htmlFor="experienceYears">Experience (Years) *</label>
                <input
                  id="experienceYears"
                  name="experienceYears"
                  type="number"
                  min="0"
                  className="form-control"
                  placeholder="e.g. 12"
                  value={profileForm.experienceYears}
                  onChange={handleProfileChange}
                />
                {errors.experienceYears && <span className="form-error-msg">{errors.experienceYears}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="consultationFee">Consultation Fee ($) *</label>
                <input
                  id="consultationFee"
                  name="consultationFee"
                  type="number"
                  step="0.01"
                  min="0"
                  className="form-control"
                  placeholder="e.g. 150.00"
                  value={profileForm.consultationFee}
                  onChange={handleProfileChange}
                />
                {errors.consultationFee && <span className="form-error-msg">{errors.consultationFee}</span>}
              </div>
            </div>

            <div className="form-control-row">
              <div className="form-group">
                <label className="form-label" htmlFor="availableFrom">Shift Start Time *</label>
                <input
                  id="availableFrom"
                  name="availableFrom"
                  type="time"
                  className="form-control"
                  value={profileForm.availableFrom}
                  onChange={handleProfileChange}
                />
                {errors.availableFrom && <span className="form-error-msg">{errors.availableFrom}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="availableTo">Shift End Time *</label>
                <input
                  id="availableTo"
                  name="availableTo"
                  type="time"
                  className="form-control"
                  value={profileForm.availableTo}
                  onChange={handleProfileChange}
                />
                {errors.availableTo && <span className="form-error-msg">{errors.availableTo}</span>}
              </div>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading || departments.length === 0}
            className="btn btn-primary"
            style={{ width: '100%', padding: '0.8rem', fontSize: '1rem', marginTop: '1rem' }}
          >
            {loading ? 'Processing Doctor Registration...' : '✓ Complete Doctor Registration'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default DoctorRegister;
