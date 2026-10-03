import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI } from '../services/api';

// Utility function to safely parse JWT token client-side
const parseJwt = (token) => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    console.error('Failed to parse JWT token:', e);
    return null;
  }
};

function Login({ onLoginSuccess }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const validate = () => {
    const tempErrors = {};
    if (!email) {
      tempErrors.email = 'Email address is required';
    } else if (!/\S+@\S+\.\S+/.test(email)) {
      tempErrors.email = 'Please enter a valid email address';
    }

    if (!password) {
      tempErrors.password = 'Password is required';
    } else if (password.length < 6) {
      tempErrors.password = 'Password must be at least 6 characters';
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
      const token = await authAPI.login(email, password);

      const claims = parseJwt(token);
      if (claims) {
        const rawRole = claims.role || '';
        const role = rawRole.replace('ROLE_', '');
        const userId = claims.userId;

        const userData = {
          email: claims.sub,
          role: role,
          userId: userId,
          token: token
        };

        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(userData));

        setSuccessMessage('Authentication successful! Opening your clinical portal...');
        onLoginSuccess(userData);

        setTimeout(() => {
          navigate('/');
        }, 1000);
      } else {
        setErrorMessage('Failed to decode authentication token');
      }
    } catch (error) {
      console.error('Login error:', error);
      const msg = error.response?.data?.message || error.response?.data || 'Invalid email or password';
      setErrorMessage(typeof msg === 'string' ? msg : 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-split-container">
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
            Modern Clinical Care & Digital Health Platform
          </h1>
          <p className="auth-hero-subtitle">
            Seamlessly connecting certified medical specialists, verified patients, and hospital administrators in one secure clinical workspace.
          </p>

          <div className="auth-feature-list">
            <div className="auth-feature-item">
              <span className="auth-feature-bullet">✓</span>
              <span>Online Prescription Issuance & Rx Management</span>
            </div>
            <div className="auth-feature-item">
              <span className="auth-feature-bullet">✓</span>
              <span>Direct Doctor Scheduling & Shift Consultations</span>
            </div>
            <div className="auth-feature-item">
              <span className="auth-feature-bullet">✓</span>
              <span>Encrypted Records & Role-Based Access Control</span>
            </div>
          </div>
        </div>

        <div className="auth-hero-footer">
          CarePortal Healthcare System • Clinical Grade Management
        </div>
      </div>

      {/* RIGHT WHITE LOGIN FORM PANE */}
      <div className="auth-form-pane">
        <div className="auth-form-header">
          <h2>Welcome Back</h2>
          <p>Please enter your credentials to access your portal.</p>
        </div>

        {errorMessage && <div className="alert alert-danger">⚠️ {errorMessage}</div>}
        {successMessage && <div className="alert alert-success">✓ {successMessage}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="email">
              Email Address
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              className="form-control"
              placeholder="name@careportal.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            {errors.email && <span className="form-error-msg">{errors.email}</span>}
          </div>

          <div className="form-group">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label className="form-label" htmlFor="password" style={{ marginBottom: 0 }}>
                Password
              </label>
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--primary)',
                  fontSize: '0.8rem',
                  cursor: 'pointer',
                  fontWeight: 600
                }}
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              className="form-control"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{ marginTop: '0.4rem' }}
            />
            {errors.password && <span className="form-error-msg">{errors.password}</span>}
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '0.75rem', padding: '0.75rem', fontSize: '1rem' }}
          >
            {loading ? 'Signing In...' : 'Sign In to Portal'}
          </button>
        </form>

        <div style={{ marginTop: '2rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
          Don't have an account yet?{' '}
          <Link to="/register" style={{ color: 'var(--primary-dark)', fontWeight: 700 }}>
            Create an Account
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Login;
