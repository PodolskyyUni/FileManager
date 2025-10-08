const API_URL = 'http://localhost:8081/api';

const api = {
  request: async (url, options = {}) => {
    const token = localStorage.getItem('token');
    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
        ...options.headers
      },
      ...options
    };

    try {
      const response = await fetch(`${API_URL}${url}`, config);

      if (response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        window.location.reload();
        return;
      }

      if (!response.ok) {
        const error = await response.text();
        throw new Error(error || `HTTP ${response.status}`);
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }
      return response;
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  },

  get: (url) => api.request(url),
  post: (url, data) => api.request(url, { method: 'POST', body: JSON.stringify(data) }),
  put: (url, data) => api.request(url, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (url) => api.request(url, { method: 'DELETE' })
};

const auth = {
  register: (username, password, email) =>
    api.post('/auth/register', { username, password, email }),

  login: (username, password) =>
    api.post('/auth/login', { username, password }),

  validate: () => api.get('/auth/validate'),
};

const files = {
  upload: async (file) => {
    const token = localStorage.getItem('token');
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${API_URL}/files/upload`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    });

    if (!response.ok) throw new Error('Upload failed');
    return response.json();
  },

  update: async (fileId, file) => {
    const token = localStorage.getItem('token');
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${API_URL}/files/${fileId}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    });

    if (!response.ok) throw new Error('Update failed');
    return response.json();
  },

  list: async (ascending, types) => {
    let url = '/files/list';
    const params = new URLSearchParams();
    if (ascending != null) params.append('ascending', ascending);
    if (types && types.length > 0) types.forEach(t => params.append('types', t));
    if (params.toString()) url += `?${params.toString()}`;

    return api.get(url);
  },

  download: async (fileId) => {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_URL}/files/download/${fileId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Download failed');
    return response.blob();
  },

  delete: (fileId) => api.delete(`/files/${fileId}`),

  getMetadata: (fileId) => api.get(`/files/${fileId}`),
};

export { auth, files };
export default api;