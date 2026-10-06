import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI } from '../services/api';

function Register() {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phoneNumber: '',
    role: 'PATIENT',
    dateOfBirth: '',
    gender: 'MALE',
    bloodGroup: 'A_POSITIVE',
    address: '',
    emergencyContact: '',
    allergies: '',
    medicalHistory: ''
  });

  const [showOptionalMedical, setShowOptionalMedical] = useState(false);
  const [registrationResult, setRegistrationResult] = useState(null);
  const [errors, setErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const validate = () => {
    const tempErrors = {};

    if (!formData.firstName.trim()) {
      tempErrors.firstName = 'First name is required';
    } else if (formData.firstName.length < 2 || formData.firstName.length > 50) {
      tempErrors.firstName = 'First name must be between 2 and 50 characters';
    }

    if (!formData.lastName.trim()) {
      tempErrors.lastName = 'Last name is required';
    } else if (formData.lastName.length < 2 || formData.lastName.length > 50) {
      tempErrors.lastName = 'Last name must be between 2 and 50 characters';
    }

    if (!formData.email) {
      tempErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      tempErrors.email = 'Please enter a valid email address';
    }

    if (!formData.password) {
      tempErrors.password = 'Password is required';
    } else if (formData.password.length < 6 || formData.password.length > 100) {
      tempErrors.password = 'Password must be at least 6 characters';
    }

    const digitsOnly = formData.phoneNumber.replace(/\D/g, '');
    if (!formData.phoneNumber) {
      tempErrors.phoneNumber = 'Phone number is required';
    } else if (digitsOnly.length !== 10) {
      tempErrors.phoneNumber = 'Phone number must be exactly 10 digits';
    }

    if (!formData.role) {
      tempErrors.role = 'Role is required';
    }

    if (formData.dateOfBirth) {
      const dob = new Date(formData.dateOfBirth);
      if (dob >= new Date()) {
        tempErrors.dateOfBirth = 'Date of birth must be in the past';
      }
    }

    setErrors(tempErrors);
    return Object.keys(tempErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!validate()) return;

    setLoading(true);
    try {
      const digitsOnly = formData.phoneNumber.replace(/\D/g, '');
      const submitData = {
        ...formData,
        phoneNumber: digitsOnly,
      };

      if (formData.role !== 'PATIENT') {
        delete submitData.dateOfBirth;
        delete submitData.gender;
        delete submitData.bloodGroup;
        delete submitData.address;
        delete submitData.emergencyContact;
        delete submitData.allergies;
        delete submitData.medicalHistory;
      }

      const res = await authAPI.register(submitData);
      setRegistrationResult(res);
    } catch (error) {
      console.error('Registration error:', error);
      const msg = error.response?.data?.message || error.response?.data || 'Registration failed';
      setErrorMessage(typeof msg === 'string' ? msg : 'Registration failed. Check details or email uniqueness.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-split-container" style={{ maxWidth: '1100px' }}>
      {/* LEFT HEALTHCARE HERO SECTION */}
      <div className="auth-hero-pane">
        <div className="auth-hero-brand">
          <div className="auth-hero-brand-icon">
            <span>⚕</span>
          </div>
          <span className="auth-hero-brand-name">CarePortal</span>
        </div>

        <div className="auth-hero-content">
          <h1 className="auth-hero-title">
            Join Our Clinical Healthcare Network
          </h1>
          <p className="auth-hero-subtitle">
            Create your account to access digital consultations, doctor schedules, and electronic medical prescriptions.
          </p>

          <div className="auth-feature-list">
            <div className="auth-feature-item">
              <span className="auth-feature-bullet">1</span>
              <span><strong>Patients:</strong> Book appointments & view prescriptions</span>
            </div>
            <div className="auth-feature-item">
              <span className="auth-feature-bullet">2</span>
              <span><strong>Doctors:</strong> Manage shifts & issue clinical Rx</span>
            </div>
            <div className="auth-feature-item">
              <span className="auth-feature-bullet">3</span>
              <span><strong>Admins:</strong> Manage hospital departments & staff</span>
            </div>
          </div>
        </div>

        <div className="auth-hero-footer">
          Fast • HIPAA-Ready • Verified Healthcare Records
        </div>
      </div>

      {/* RIGHT WHITE REGISTRATION FORM PANE */}
      <div className="auth-form-pane" style={{ padding: '2.5rem 3rem' }}>
        {registrationResult ? (
          /* REGISTRATION SUCCESS CARD */
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <div
              style={{
                width: '68px',
                height: '68px',
                borderRadius: '50%',
                backgroundColor: 'var(--success-bg)',
                color: 'var(--success)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '2.5rem',
                margin: '0 auto 1.25rem auto'
              }}
            >
              ✓
            </div>

            <h2 style={{ color: 'var(--primary-dark)', fontSize: '1.6rem', marginBottom: '0.5rem' }}>
              Patient Registration Successful
            </h2>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1.75rem', fontSize: '0.95rem' }}>
              Your account and clinical health record profile have been created.
            </p>

            <div
              style={{
                backgroundColor: 'var(--bg-main)',
                border: '1px solid var(--border-subtle)',
                borderRadius: '12px',
                padding: '1.5rem',
                maxWidth: '440px',
                margin: '0 auto 1.5rem auto',
                textAlign: 'left'
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '1rem',
                  paddingBottom: '0.75rem',
                  borderBottom: '1px solid var(--border-subtle)'
                }}
              >
                <div>
                  <span style={{ fontWeight: 600, color: 'var(--text-main)', display: 'block' }}>User ID</span>
                  <small style={{ color: 'var(--text-secondary)' }}>Use this for login with existing User ID</small>
                </div>
                <span style={{ fontWeight: 700, fontSize: '1.4rem', color: 'var(--primary-dark)' }}>
                  {registrationResult.id}
                </span>
              </div>

              {registrationResult.patientProfileId && (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <span style={{ fontWeight: 600, color: 'var(--text-main)', display: 'block' }}>Patient Profile ID</span>
                    <small style={{ color: 'var(--text-secondary)' }}>Hospital clinical record reference</small>
                  </div>
                  <span style={{ fontWeight: 700, fontSize: '1.4rem', color: 'var(--text-main)' }}>
                    {registrationResult.patientProfileId}
                  </span>
                </div>
              )}
            </div>

            <div
              className="alert alert-info"
              style={{
                display: 'block',
                textAlign: 'left',
                fontSize: '0.9rem',
                maxWidth: '440px',
                margin: '0 auto 1.75rem auto',
                lineHeight: 1.5
              }}
            >
              <p style={{ margin: '0 0 0.5rem 0', color: 'inherit', textAlign: 'left' }}>
                Please save your User ID for future login.
              </p>
              <p style={{ margin: 0, color: 'inherit', textAlign: 'left' }}>
                You can login using either your User ID or your registered email along with your password.
              </p>
            </div>

            <button
              onClick={() => navigate('/login')}
              className="btn btn-primary"
              style={{ width: '100%', maxWidth: '440px', padding: '0.85rem', fontSize: '1rem', fontWeight: 600 }}
            >
              Proceed to Sign In with User ID / Email →
            </button>
          </div>
        ) : (
          <div>
            <div className="auth-form-header" style={{ marginBottom: '1.5rem' }}>
              <h2>Create Account</h2>
              <p>Register as a Patient, Physician, or Administrator.</p>
            </div>

            {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}

            <form onSubmit={handleSubmit}>
              {/* PERSONAL INFORMATION */}
              <div className="form-control-row">
                <div className="form-group">
                  <label className="form-label" htmlFor="firstName">
                    First Name <span style={{ color: 'var(--danger-text)' }}>*</span>
                  </label>
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    className="form-control"
                    placeholder="Sarah"
                    value={formData.firstName}
                    onChange={handleChange}
                  />
                  {errors.firstName && <span className="form-error-msg">{errors.firstName}</span>}
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="lastName">
                    Last Name <span style={{ color: 'var(--danger-text)' }}>*</span>
                  </label>
                  <input
                    id="lastName"
                    name="lastName"
                    type="text"
                    className="form-control"
                    placeholder="Jenkins"
                    value={formData.lastName}
                    onChange={handleChange}
                  />
                  {errors.lastName && <span className="form-error-msg">{errors.lastName}</span>}
                </div>
              </div>

              <div className="form-control-row">
                <div className="form-group">
                  <label className="form-label" htmlFor="email">
                    Email Address <span style={{ color: 'var(--danger-text)' }}>*</span>
                  </label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    className="form-control"
                    placeholder="sarah.j@careportal.com"
                    value={formData.email}
                    onChange={handleChange}
                  />
                  {errors.email && <span className="form-error-msg">{errors.email}</span>}
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="phoneNumber">
                    Phone (10 Digits) <span style={{ color: 'var(--danger-text)' }}>*</span>
                  </label>
                  <input
                    id="phoneNumber"
                    name="phoneNumber"
                    type="tel"
                    className="form-control"
                    placeholder="9876543210"
                    value={formData.phoneNumber}
                    onChange={handleChange}
                  />
                  {errors.phoneNumber && <span className="form-error-msg">{errors.phoneNumber}</span>}
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="password">
                  Password (Min 6 Characters) <span style={{ color: 'var(--danger-text)' }}>*</span>
                </label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  className="form-control"
                  placeholder="••••••••"
                  value={formData.password}
                  onChange={handleChange}
                />
                {errors.password && <span className="form-error-msg">{errors.password}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="role">
                  Account Role <span style={{ color: 'var(--danger-text)' }}>*</span>
                </label>
                <select
                  id="role"
                  name="role"
                  className="form-control"
                  value={formData.role}
                  onChange={handleChange}
                >
                  <option value="PATIENT">Patient (Book Appointments & View Rx)</option>
                  <option value="DOCTOR">Doctor (Consultations & Create Rx)</option>
                  <option value="ADMIN">Hospital Administrator</option>
                </select>
                {errors.role && <span className="form-error-msg">{errors.role}</span>}
              </div>

              {/* OPTIONAL PATIENT HEALTH PROFILE ACCORDION */}
              {formData.role === 'PATIENT' && (
                <div style={{ marginTop: '0.75rem', marginBottom: '1rem', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '0.75rem 1rem' }}>
                  <div
                    onClick={() => setShowOptionalMedical(!showOptionalMedical)}
                    style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer', userSelect: 'none' }}
                  >
                    <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--primary-dark)' }}>
                      + Additional Medical & Health Profile (Optional)
                    </span>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                      {showOptionalMedical ? '▲ Hide' : '▼ Expand'}
                    </span>
                  </div>

                  {showOptionalMedical && (
                    <div style={{ marginTop: '1rem' }}>
                      <div className="form-control-row">
                        <div className="form-group">
                          <label className="form-label" htmlFor="dateOfBirth">Date of Birth</label>
                          <input
                            id="dateOfBirth"
                            name="dateOfBirth"
                            type="date"
                            className="form-control"
                            max={new Date().toISOString().split('T')[0]}
                            value={formData.dateOfBirth}
                            onChange={handleChange}
                          />
                          {errors.dateOfBirth && <span className="form-error-msg">{errors.dateOfBirth}</span>}
                        </div>

                        <div className="form-group">
                          <label className="form-label" htmlFor="gender">Gender</label>
                          <select
                            id="gender"
                            name="gender"
                            className="form-control"
                            value={formData.gender}
                            onChange={handleChange}
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
                            value={formData.bloodGroup}
                            onChange={handleChange}
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
                          <label className="form-label" htmlFor="emergencyContact">Emergency Contact</label>
                          <input
                            id="emergencyContact"
                            name="emergencyContact"
                            type="tel"
                            className="form-control"
                            placeholder="e.g. 9876543210"
                            value={formData.emergencyContact}
                            onChange={handleChange}
                          />
                        </div>
                      </div>

                      <div className="form-group">
                        <label className="form-label" htmlFor="address">Address</label>
                        <input
                          id="address"
                          name="address"
                          type="text"
                          className="form-control"
                          placeholder="e.g. 123 Main St, City"
                          value={formData.address}
                          onChange={handleChange}
                        />
                      </div>

                      <div className="form-group">
                        <label className="form-label" htmlFor="allergies">Known Allergies</label>
                        <input
                          id="allergies"
                          name="allergies"
                          type="text"
                          className="form-control"
                          placeholder="e.g. Penicillin, Peanuts"
                          value={formData.allergies}
                          onChange={handleChange}
                        />
                      </div>
                    </div>
                  )}
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary"
                style={{ width: '100%', marginTop: '0.75rem', padding: '0.75rem', fontSize: '1rem' }}
              >
                {loading ? 'Creating Account & Profile...' : 'Complete Patient Registration'}
              </button>
            </form>

            <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
              Already have an account?{' '}
              <Link to="/login" style={{ color: 'var(--primary-dark)', fontWeight: 700 }}>
                Sign in with User ID or Email
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default Register;
