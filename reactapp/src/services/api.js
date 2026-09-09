import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
});

// Request interceptor to add authorization token if available
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const authAPI = {
  login: async (email, password) => {
    // POST /api/auth/login returns JWT token string directly
    const response = await api.post('/api/auth/login', { email, password });
    return response.data;
  },
  register: async (userData) => {
    // POST /api/auth/register
    const response = await api.post('/api/auth/register', userData);
    return response.data;
  }
};

export const departmentAPI = {
  create: async (departmentData) => {
    const response = await api.post('/api/departments', departmentData);
    return response.data;
  },
  getAll: async () => {
    const response = await api.get('/api/departments');
    return response.data;
  }
};

export const patientAPI = {
  create: async (patientData) => {
    const response = await api.post('/api/patients', patientData);
    return response.data;
  },
  getAll: async () => {
    const response = await api.get('/api/patients');
    return response.data;
  },
  getById: async (id) => {
    const response = await api.get(`/api/patients/${id}`);
    return response.data;
  }
};

export const doctorAPI = {
  create: async (doctorData) => {
    const response = await api.post('/api/doctors', doctorData);
    return response.data;
  },
  getAll: async () => {
    const response = await api.get('/api/doctors');
    return response.data;
  },
  searchBySpecialization: async (specialization) => {
    const response = await api.get(`/api/doctors/search`, {
      params: { specialization }
    });
    return response.data;
  }
};

export const appointmentAPI = {
  book: async (appointmentData) => {
    const response = await api.post('/api/appointments', appointmentData);
    return response.data;
  },
  getAll: async () => {
    const response = await api.get('/api/appointments');
    return response.data;
  },
  getByPatient: async (patientId) => {
    const response = await api.get(`/api/appointments/patient/${patientId}`);
    return response.data;
  },
  getByDoctor: async (doctorId) => {
    const response = await api.get(`/api/appointments/doctor/${doctorId}`);
    return response.data;
  },
  // Update status with fallback handling for PATCH status
  updateStatus: async (appointmentId, status) => {
    try {
      // 1. Try spec-defined PATCH request first
      const response = await api.patch(`/api/appointments/${appointmentId}/status`, { status });
      return response.data;
    } catch (error) {
      // 2. Fall back to backend PUT endpoints if PATCH status returns 404/405 (Not Found / Method Not Allowed)
      if (error.response && (error.response.status === 404 || error.response.status === 405)) {
        console.warn(`PATCH status endpoint not available. Falling back to PUT mapping for status: ${status}`);
        
        let response;
        if (status === 'APPROVED' || status === 'CONFIRMED') {
          response = await api.put(`/api/appointments/${appointmentId}/confirm`);
        } else if (status === 'REJECTED' || status === 'CANCELLED') {
          response = await api.put(`/api/appointments/${appointmentId}/cancel`);
        } else if (status === 'COMPLETED') {
          response = await api.put(`/api/appointments/${appointmentId}/complete`);
        } else {
          throw new Error(`Unsupported fallback status: ${status}`);
        }
        return response.data;
      }
      // Re-throw if it's another error (e.g., validation, 403 Forbidden, 500 Server Error)
      throw error;
    }
  }
};

export default api;
