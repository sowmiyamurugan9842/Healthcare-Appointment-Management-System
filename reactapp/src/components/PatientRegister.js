import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI, patientAPI } from '../services/api';

function PatientRegister() {
  const [useExistingUser, setUseExistingUser] = useState(false);

  // User account state
  const [userForm, setUserForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phoneNumber: '',
    role: 'PATIENT'
  });

  // Profile details state
  const [profileForm, setProfileForm] = useState({
    userId: '',
    dateOfBirth: '',
    gender: 'MALE',
    bloodGroup: 'A_POSITIVE',
    address: '',
    emergencyContact: '',
    allergies: '',
    medicalHistory: ''
  });

  const [errors, setErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

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

    if (!profileForm.dateOfBirth) {
      tempErrors.dateOfBirth = 'Date of birth is required';
    } else {
      const selectedDate = new Date(profileForm.dateOfBirth);
      const today = new Date();
      if (selectedDate >= today) {
        tempErrors.dateOfBirth = 'Date of birth must be in the past';
      }
    }

    if (!profileForm.address.trim()) {
      tempErrors.address = 'Address is required';
    }

    if (!profileForm.emergencyContact.trim()) {
      tempErrors.emergencyContact = 'Emergency contact is required';
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

      await patientAPI.create({
        userId: finalUserId,
        dateOfBirth: profileForm.dateOfBirth,
        gender: profileForm.gender,
        bloodGroup: profileForm.bloodGroup,
        address: profileForm.address,
        emergencyContact: profileForm.emergencyContact,
        allergies: profileForm.allergies,
        medicalHistory: profileForm.medicalHistory
      });

      setSuccessMessage('Patient medical profile registered successfully!');
      setTimeout(() => {
        navigate('/patients');
      }, 1500);
    } catch (error) {
      console.error('Failed to register patient profile:', error);
      const msg = error.response?.data?.message || error.response?.data || 'Failed to create patient profile';
      setErrorMessage(typeof msg === 'string' ? msg : 'Error processing profile registration.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '750px', margin: '0 auto' }}>
      <div className="page-header">
        <div className="page-title-group">
          <h1>Register Patient Profile</h1>
          <p>Create electronic health records, emergency contacts, and medical history profiles.</p>
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
            Create New Patient User
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
          {/* USER ACCOUNT CREDENTIALS */}
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
                    placeholder="e.g. Alex"
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
                    placeholder="e.g. Taylor"
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
                    placeholder="e.g. alex.taylor@example.com"
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
                1. Associated User Record
              </h3>
              <label className="form-label" htmlFor="userId">User ID Reference *</label>
              <input
                id="userId"
                name="userId"
                type="number"
                className="form-control"
                placeholder="e.g. 5"
                value={profileForm.userId}
                onChange={handleProfileChange}
              />
              {errors.userId && <span className="form-error-msg">{errors.userId}</span>}
            </div>
          )}

          {/* MEDICAL PROFILE DETAILS */}
          <div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: 'var(--primary-dark)', paddingBottom: '0.4rem', borderBottom: '1px solid var(--border-subtle)' }}>
              2. Medical Profile & Health History
            </h3>

            <div className="form-control-row">
              <div className="form-group">
                <label className="form-label" htmlFor="dateOfBirth">Date of Birth *</label>
                <input
                  id="dateOfBirth"
                  name="dateOfBirth"
                  type="date"
                  className="form-control"
                  max={new Date().toISOString().split('T')[0]}
                  value={profileForm.dateOfBirth}
                  onChange={handleProfileChange}
                />
                {errors.dateOfBirth && <span className="form-error-msg">{errors.dateOfBirth}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="gender">Gender *</label>
                <select
                  id="gender"
                  name="gender"
                  className="form-control"
                  value={profileForm.gender}
                  onChange={handleProfileChange}
                >
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
            </div>

            <div className="form-control-row">
              <div className="form-group">
                <label className="form-label" htmlFor="bloodGroup">Blood Group *</label>
                <select
                  id="bloodGroup"
                  name="bloodGroup"
                  className="form-control"
                  value={profileForm.bloodGroup}
                  onChange={handleProfileChange}
                >
                  <option value="A_POSITIVE">A+ (A Positive)</option>
                  <option value="A_NEGATIVE">A- (A Negative)</option>
                  <option value="B_POSITIVE">B+ (B Positive)</option>
                  <option value="B_NEGATIVE">B- (B Negative)</option>
                  <option value="AB_POSITIVE">AB+ (AB Positive)</option>
                  <option value="AB_NEGATIVE">AB- (AB Negative)</option>
                  <option value="O_POSITIVE">O+ (O Positive)</option>
                  <option value="O_NEGATIVE">O- (O Negative)</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="emergencyContact">Emergency Contact (Phone) *</label>
                <input
                  id="emergencyContact"
                  name="emergencyContact"
                  type="tel"
                  className="form-control"
                  placeholder="e.g. 9876543210"
                  value={profileForm.emergencyContact}
                  onChange={handleProfileChange}
                />
                {errors.emergencyContact && <span className="form-error-msg">{errors.emergencyContact}</span>}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="address">Permanent Residential Address *</label>
              <input
                id="address"
                name="address"
                type="text"
                className="form-control"
                placeholder="e.g. 452 Medical Center Blvd, Suite 100"
                value={profileForm.address}
                onChange={handleProfileChange}
              />
              {errors.address && <span className="form-error-msg">{errors.address}</span>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="allergies">Known Allergies (Optional)</label>
              <textarea
                id="allergies"
                name="allergies"
                rows="2"
                className="form-control"
                placeholder="e.g. Penicillin, Peanuts, Latex, Aspirin..."
                value={profileForm.allergies}
                onChange={handleProfileChange}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="medicalHistory">Prior Medical Conditions & History (Optional)</label>
              <textarea
                id="medicalHistory"
                name="medicalHistory"
                rows="2"
                className="form-control"
                placeholder="e.g. Hypertension, Asthma, Prior surgeries..."
                value={profileForm.medicalHistory}
                onChange={handleProfileChange}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', padding: '0.8rem', fontSize: '1rem', marginTop: '1rem' }}
          >
            {loading ? 'Processing Registration...' : '✓ Complete Patient Registration'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default PatientRegister;
