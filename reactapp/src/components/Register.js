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
  });

  const [errors, setErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
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
    setSuccessMessage('');

    if (!validate()) return;

    setLoading(true);
    try {
      const digitsOnly = formData.phoneNumber.replace(/\D/g, '');
      const submitData = {
        ...formData,
        phoneNumber: digitsOnly,
      };

      await authAPI.register(submitData);

      setSuccessMessage('Account registered successfully! Redirecting to login...');
      setTimeout(() => {
        navigate('/login');
      }, 1500);
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
            Create your account to access digital consultations, doctor schedules, and digital medical prescriptions.
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
        <div className="auth-form-header" style={{ marginBottom: '1.5rem' }}>
          <h2>Create Account</h2>
          <p>Register as a Patient, Physician, or Administrator.</p>
        </div>

        {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
        {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

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

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '0.75rem', padding: '0.75rem', fontSize: '1rem' }}
          >
            {loading ? 'Creating Account...' : 'Complete Registration'}
          </button>
        </form>

        <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'var(--primary-dark)', fontWeight: 700 }}>
            Sign in here
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Register;
