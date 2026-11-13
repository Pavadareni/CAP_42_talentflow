
import axios from 'axios';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 20000
});

// Attach JWT to protected backend requests
http.interceptors.request.use(config => {
  const isLoginRequest = config.url?.includes('/api/auth/login');

  if (!isLoginRequest) {
    const token = localStorage.getItem('tf_token');

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }

  return config;
});

// Handle authentication errors
http.interceptors.response.use(
  response => response,
  error => {
    const isLoginRequest = error.config?.url?.includes('/api/auth/login');

    if (error.response?.status === 401 && !isLoginRequest) {
      localStorage.removeItem('tf_token');
      localStorage.removeItem('tf_user');

      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

// Python FastAPI optimizer
export const optimizerApi = axios.create({
  baseURL: import.meta.env.VITE_OPTIMIZER_BASE_URL || 'http://localhost:8001',
  timeout: 120000
});

export const api = {
  // Authentication
  login: async body =>
    (await http.post('/api/auth/login', body)).data,

  // Employees
  employees: async () =>
    (await http.get('/api/employees')).data,

  createEmployee: async payload =>
    (await http.post('/api/employees', payload)).data,

  deactivateEmployee: async id =>
    (await http.delete(`/api/employees/${id}`)).data,

  // Employee self-service
  me: async () =>
    (await http.get('/api/employees/me')).data,

  mySkills: async () =>
    (await http.get('/api/employees/me/skills')).data,

  myAvailability: async () =>
    (await http.get('/api/employees/me/availability')).data,

  myAllocations: async () =>
    (await http.get('/api/employees/me/allocations')).data,

  // Projects
  projects: async () =>
    (await http.get('/api/projects')).data,


projectById: async id =>
  (await http.get(`/api/projects/${id}`)).data,

createProject: async payload =>
  (await http.post('/api/projects', payload)).data,

updateProject: async (id, payload) =>
  (await http.put(`/api/projects/${id}`, payload)).data,

deleteProject: async id =>
  (await http.delete(`/api/projects/${id}`)).data,

  // Employee skills management
  employeeSkills: async employeeId =>
    (await http.get(`/api/employees/${employeeId}/skills`)).data,

  assignEmployeeSkill: async (employeeId, payload) =>
    (await http.post(`/api/employees/${employeeId}/skills`, payload)).data,

  updateEmployeeSkill: async (employeeId, skillId, payload) =>
    (await http.put(
      `/api/employees/${employeeId}/skills/${skillId}`,
      payload
    )).data,

  removeEmployeeSkill: async (employeeId, skillId) =>
    (await http.delete(
      `/api/employees/${employeeId}/skills/${skillId}`
    )).data,


  // Skill catalogue
  skills: async () =>
    (await http.get('/api/skills')).data,

  // Employee skills
  employeeSkills: async employeeId =>
    (await http.get(`/api/employees/${employeeId}/skills`)).data,

  assignEmployeeSkill: async (employeeId, payload) =>
    (await http.post(
      `/api/employees/${employeeId}/skills`,
      payload
    )).data,

  updateEmployeeSkill: async (employeeId, skillId, payload) =>
    (await http.put(
      `/api/employees/${employeeId}/skills/${skillId}`,
      payload
    )).data,

  removeEmployeeSkill: async (employeeId, skillId) =>
    (await http.delete(
      `/api/employees/${employeeId}/skills/${skillId}`
    )).data,


  // Allocations
  allocations: async () =>
    (await http.get('/api/allocations')).data,

  proposed: async () =>
    (await http.get('/api/allocations/proposed')).data,

  approve: async id =>
    (await http.patch(`/api/allocations/${id}/approve`)).data,

  reject: async (id, reason) =>
    (await http.patch(`/api/allocations/${id}/reject`, { reason })).data,


  // Python optimization
  optimize: async (id, hours) => {
    const response = await optimizerApi.post(
      `/optimization/project/${id}`,
      null,
      {
        params: {
          requested_hours: hours
        },

        // Expected business outcomes may use HTTP 4xx.
        // Let the frontend process their JSON responses.
        validateStatus: status => status >= 200 && status < 500
      }
    );

    let data = response.data;

    // Support FastAPI HTTPException(detail=result)
    if (data?.detail && typeof data.detail === 'object') {
      data = data.detail;
    }

    // Support a JSON-encoded detail string
    if (typeof data?.detail === 'string') {
      try {
        const parsed = JSON.parse(data.detail);
        if (parsed?.status) data = parsed;
      } catch {
        // Keep the original response for normal error handling.
      }
    }

    return data;
  }

  


    
};  

export default http;
