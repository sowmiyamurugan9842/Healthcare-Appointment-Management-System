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
    
    // First Name validation (2-50 characters)
    if (!formData.firstName.trim()) {
      tempErrors.firstName = 'First name is required';
    } else if (formData.firstName.length < 2 || formData.firstName.length > 50) {
      tempErrors.firstName = 'First name must be between 2 and 50 characters';
    }

    // Last Name validation (2-50 characters)
    if (!formData.lastName.trim()) {
      tempErrors.lastName = 'Last name is required';
    } else if (formData.lastName.length < 2 || formData.lastName.length > 50) {
      tempErrors.lastName = 'Last name must be between 2 and 50 characters';
    }

    // Email validation
    if (!formData.email) {
      tempErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      tempErrors.email = 'Please enter a valid email address';
    }

    // Password validation (6-100 characters)
    if (!formData.password) {
      tempErrors.password = 'Password is required';
    } else if (formData.password.length < 6 || formData.password.length > 100) {
      tempErrors.password = 'Password must be between 6 and 100 characters';
    }

    // Phone Number validation (exactly 10 digits)
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
      // Ensure phone number sent contains only digits if backend expects standard format
      const digitsOnly = formData.phoneNumber.replace(/\D/g, '');
      const submitData = {
        ...formData,
        phoneNumber: digitsOnly
      };
      
      await authAPI.register(submitData);
      
      setSuccessMessage('Registration successful! Redirecting to login...');
      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (error) {
      console.error('Registration error:', error);
      const msg = error.response?.data?.message || error.response?.data || 'Registration failed';
      setErrorMessage(typeof msg === 'string' ? msg : 'Registration failed. Check details or email uniqueness.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '550px', margin: '3rem auto 0' }} className="card">
      <h2 className="form-title">Create Account</h2>

      {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
      {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-control-row">
          <div className="form-group">
            <label className="form-label" htmlFor="firstName">First Name</label>
            <input
              id="firstName"
              name="firstName"
              type="text"
              className="form-control"
              placeholder="John"
              value={formData.firstName}
              onChange={handleChange}
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
              placeholder="Doe"
              value={formData.lastName}
              onChange={handleChange}
            />
            {errors.lastName && <span className="form-error-msg">{errors.lastName}</span>}
          </div>
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="email">Email Address</label>
          <input
            id="email"
            name="email"
            type="email"
            className="form-control"
            placeholder="john.doe@example.com"
            value={formData.email}
            onChange={handleChange}
          />
          {errors.email && <span className="form-error-msg">{errors.email}</span>}
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            className="form-control"
            placeholder="Min 6 characters"
            value={formData.password}
            onChange={handleChange}
          />
          {errors.password && <span className="form-error-msg">{errors.password}</span>}
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="phoneNumber">Phone Number (10 Digits)</label>
          <input
            id="phoneNumber"
            name="phoneNumber"
            type="tel"
            className="form-control"
            placeholder="1234567890"
            value={formData.phoneNumber}
            onChange={handleChange}
          />
          {errors.phoneNumber && <span className="form-error-msg">{errors.phoneNumber}</span>}
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="role">Sign Up As</label>
          <select
            id="role"
            name="role"
            className="form-control"
            value={formData.role}
            onChange={handleChange}
          >
            <option value="PATIENT">Patient</option>
            <option value="DOCTOR">Doctor</option>
            <option value="ADMIN">Admin</option>
          </select>
          {errors.role && <span className="form-error-msg">{errors.role}</span>}
        </div>

        <button type="submit" disabled={loading} className="btn btn-primary" style={{ width: '100%', marginTop: '1rem' }}>
          {loading ? 'Registering...' : 'Register'}
        </button>
      </form>

      <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.9rem' }}>
        <p>Already have an account? <Link to="/login" style={{ color: 'var(--primary)', fontWeight: '500' }}>Login here</Link></p>
      </div>
    </div>
  );
}

export default Register;
