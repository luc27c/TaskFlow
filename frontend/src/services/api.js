const API_URL = '/api';

// Get token from localStorage
const getToken = () => localStorage.getItem('token');

// Auth API calls
export const authApi = {
  login: async (email, password) => {
    const res = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.error || 'Login failed');
    }
    return res.json();
  },

  register: async (email, password, firstName, lastName) => {
    const res = await fetch(`${API_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, firstName, lastName }),
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.error || 'Registration failed');
    }
    return res.text(); // Returns token as string
  },
};

// Workflow API calls
export const workflowApi = {
  getAll: async () => {
    const res = await fetch(`${API_URL}/workflows`, {
      headers: { 'Authorization': `Bearer ${getToken()}` },
    });
    if (!res.ok) throw new Error(`${res.status}: Failed to fetch workflows`);
    return res.json();
  },

  create: async (workflow) => {
    const res = await fetch(`${API_URL}/workflows`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`,
      },
      body: JSON.stringify({ ...workflow, active: true }),
    });
    if (!res.ok) throw new Error('Failed to create workflow');
    return res.json();
  },

  update: async (id, workflow) => {
    const res = await fetch(`${API_URL}/workflows/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`,
      },
      body: JSON.stringify(workflow),
    });
    if (!res.ok) throw new Error('Failed to update workflow');
    return res.json();
  },

  delete: async (id) => {
    const res = await fetch(`${API_URL}/workflows/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${getToken()}` },
    });
    if (!res.ok) throw new Error('Failed to delete workflow');
  },

  run: async (id) => {
    const res = await fetch(`${API_URL}/workflows/${id}/run`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${getToken()}` },
    });
    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.error || 'Failed to run workflow');
    }
    return res.json();
  },
};

// Google OAuth API calls
export const googleApi = {
  getAuthUrl: async () => {
    const res = await fetch(`${API_URL}/auth/google/authorize`, {
      headers: { 'Authorization': `Bearer ${getToken()}` },
    });
    if (!res.ok) throw new Error('Failed to get auth URL');
    return res.json();
  },

  getStatus: async () => {
    const res = await fetch(`${API_URL}/auth/google/status`, {
      headers: { 'Authorization': `Bearer ${getToken()}` },
    });
    if (!res.ok) throw new Error('Failed to get status');
    return res.json();
  },

  disconnect: async () => {
    const res = await fetch(`${API_URL}/auth/google/disconnect`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${getToken()}` },
    });
    if (!res.ok) throw new Error('Failed to disconnect');
    return res.json();
  },
};
