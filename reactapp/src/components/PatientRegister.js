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

    // Validate Profile fields
    if (!useExistingUser) {
      // Validate User fields
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

    // Validate DOB (must be in the past)
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

      // Step 1: Register user first if not linking existing
      if (!useExistingUser) {
        const userDigitsPhone = userForm.phoneNumber.replace(/\D/g, '');
        const userRes = await authAPI.register({
          ...userForm,
          phoneNumber: userDigitsPhone
        });
        finalUserId = userRes.id;
      }

      // Step 2: Create Patient Profile
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

      setSuccessMessage('Patient profile created successfully!');
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
    <div style={{ maxWidth: '650px', margin: '1rem auto' }} className="card">
      <h2 className="form-title">Patient Profile Registration</h2>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem', justifyContent: 'center' }}>
        <button
          type="button"
          onClick={() => setUseExistingUser(false)}
          className={`btn ${!useExistingUser ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '0.4rem 1rem' }}
        >
          New Patient User
        </button>
        <button
          type="button"
          onClick={() => setUseExistingUser(true)}
          className={`btn ${useExistingUser ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '0.4rem 1rem' }}
        >
          Link Existing User ID
        </button>
      </div>

      <form onSubmit={handleSubmit}>
        {/* User details section */}
        {!useExistingUser ? (
          <div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', borderBottom: '1px solid var(--panel-border)', paddingBottom: '0.5rem' }}>
              1. User Account Credentials
            </h3>
            <div className="form-control-row">
              <div className="form-group">
                <label className="form-label" htmlFor="firstName">First Name</label>
                <input
                  id="firstName"
                  name="firstName"
                  type="text"
                  className="form-control"
                  placeholder="e.g. John"
                  value={userForm.firstName}
                  onChange={handleUserChange}
                />
                {errors.firstName && <span className="form-error-msg">{errors.firstName}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="lastName">Last Name</label>
                <input
                  id="lastName"
                  name="lastName"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Doe"
                  value={userForm.lastName}
                  onChange={handleUserChange}
                />
                {errors.lastName && <span className="form-error-msg">{errors.lastName}</span>}
              </div>
            </div>

            <div className="form-control-row">
              <div className="form-group">
                <label className="form-label" htmlFor="email">Email Address</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  className="form-control"
                  placeholder="e.g. john@example.com"
                  value={userForm.email}
                  onChange={handleUserChange}
                />
                {errors.email && <span className="form-error-msg">{errors.email}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="password">Account Password</label>
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
              <label className="form-label" htmlFor="phoneNumber">Phone Number (10 digits)</label>
              <input
                id="phoneNumber"
                name="phoneNumber"
                type="tel"
                className="form-control"
                placeholder="e.g. 9876543210"
                value={userForm.phoneNumber}
                onChange={handleUserChange}
              />
              {errors.phoneNumber && <span className="form-error-msg">{errors.phoneNumber}</span>}
            </div>
          </div>
        ) : (
          <div className="form-group">
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', borderBottom: '1px solid var(--panel-border)', paddingBottom: '0.5rem' }}>
              1. Associated User ID
            </h3>
            <label className="form-label" htmlFor="userId">User ID Reference</label>
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

        {/* Medical details section */}
        <div>
          <h3 style={{ fontSize: '1.1rem', marginTop: '1.5rem', marginBottom: '1rem', borderBottom: '1px solid var(--panel-border)', paddingBottom: '0.5rem' }}>
            2. Medical Profile Details
          </h3>
          
          <div className="form-control-row">
            <div className="form-group">
              <label className="form-label" htmlFor="dateOfBirth">Date of Birth</label>
              <input
                id="dateOfBirth"
                name="dateOfBirth"
                type="date"
                className="form-control"
                value={profileForm.dateOfBirth}
                onChange={handleProfileChange}
              />
              {errors.dateOfBirth && <span className="form-error-msg">{errors.dateOfBirth}</span>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="gender">Gender</label>
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
              <label className="form-label" htmlFor="bloodGroup">Blood Group</label>
              <select
                id="bloodGroup"
                name="bloodGroup"
                className="form-control"
                value={profileForm.bloodGroup}
                onChange={handleProfileChange}
              >
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

            <div className="form-group">
              <label className="form-label" htmlFor="emergencyContact">Emergency Contact (Phone)</label>
              <input
                id="emergencyContact"
                name="emergencyContact"
                type="tel"
                className="form-control"
                placeholder="Emergency phone number"
                value={profileForm.emergencyContact}
                onChange={handleProfileChange}
              />
              {errors.emergencyContact && <span className="form-error-msg">{errors.emergencyContact}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="address">Permanent Address</label>
            <input
              id="address"
              name="address"
              type="text"
              className="form-control"
              placeholder="e.g. 123 Main St, Springfield"
              value={profileForm.address}
              onChange={handleProfileChange}
            />
            {errors.address && <span className="form-error-msg">{errors.address}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="allergies">Allergies (Optional)</label>
            <textarea
              id="allergies"
              name="allergies"
              rows="2"
              className="form-control"
              placeholder="e.g. Penicillin, Peanuts (leave blank if none)"
              value={profileForm.allergies}
              onChange={handleProfileChange}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="medicalHistory">Medical History Summary (Optional)</label>
            <textarea
              id="medicalHistory"
              name="medicalHistory"
              rows="3"
              className="form-control"
              placeholder="e.g. Hypertension, Diabetes, prior operations..."
              value={profileForm.medicalHistory}
              onChange={handleProfileChange}
            />
          </div>
        </div>

        <button type="submit" disabled={loading} className="btn btn-accent" style={{ width: '100%', marginTop: '1rem' }}>
          {loading ? 'Processing Registration...' : 'Register Patient'}
        </button>
      </form>
    </div>
  );
}

export default PatientRegister;
