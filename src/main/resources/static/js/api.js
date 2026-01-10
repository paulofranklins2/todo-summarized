/**
 * API Client Module
 */
const Api = (() => {
    const getApiBase = () => document.documentElement.dataset.apiBase || '/api';

    const getToken = () => localStorage.getItem('accessToken');

    const buildHeaders = (includeAuth = true) => {
        const headers = { 'Content-Type': 'application/json' };
        if (includeAuth) {
            const token = getToken();
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
        }
        return headers;
    };

    const handleResponse = async (response) => {
        if (response.status === 401 || response.status === 403) {
            const errorBody = await response.json().catch(() => ({}));
            throw new Error(errorBody.message || errorBody.error || 'Authentication required');
        }

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || error.error || `Request failed: ${response.status}`);
        }

        if (response.status === 204) {
            return null;
        }

        return response.json();
    };

    const refreshToken = async () => {
        const refresh = localStorage.getItem('refreshToken');
        if (!refresh || (refresh.match(/\./g) || []).length !== 2) {
            return false;
        }

        try {
            const response = await fetch(`${getApiBase()}/auth/refresh`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken: refresh })
            });

            if (!response.ok) return false;

            const data = await response.json();
            const accessToken = data.accessToken || data.access_token;
            const refreshTokenNew = data.refreshToken || data.refresh_token;
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshTokenNew);
            return true;
        } catch (e) {
            return false;
        }
    };

    const request = async (endpoint, options = {}) => {
        const url = `${getApiBase()}${endpoint}`;
        const headers = buildHeaders(options.auth !== false);
        const config = {
            method: options.method || 'GET',
            headers: headers
        };

        if (options.body && typeof options.body === 'object') {
            config.body = JSON.stringify(options.body);
        }

        const response = await fetch(url, config);
        return handleResponse(response);
    };

    const auth = {
        signup: (data) => request('/auth/signup', { method: 'POST', body: data, auth: false }),
        signin: (data) => request('/auth/signin', { method: 'POST', body: data, auth: false }),
        refresh: (refreshToken) => request('/auth/refresh', { method: 'POST', body: { refreshToken }, auth: false })
    };

    const todos = {
        list: (params = {}) => {
            const searchParams = new URLSearchParams();
            Object.entries(params).forEach(([key, value]) => {
                if (value !== null && value !== undefined && value !== '') {
                    searchParams.append(key, value);
                }
            });
            const query = searchParams.toString();
            return request(`/todos${query ? `?${query}` : ''}`);
        },
        get: (id) => request(`/todos/${id}`),
        create: (data) => request('/todos', { method: 'POST', body: data }),
        update: (id, data) => request(`/todos/${id}`, { method: 'PUT', body: data }),
        delete: (id) => request(`/todos/${id}`, { method: 'DELETE' }),
        updateStatus: (id, status) => request(`/todos/${id}/status?status=${status}`, { method: 'PATCH' })
    };

    const summary = {
        daily: () => request('/summary/daily'),
        ai: (type) => request(`/summary/ai?type=${type}`),
        aiCached: () => request('/summary/ai/cached'),
        types: () => request('/summary/types'),
        aiStatus: () => request('/summary/ai/status')
    };

    const meta = {
        statuses: () => request('/meta/statuses', { auth: false }),
        priorities: () => request('/meta/priorities', { auth: false })
    };

    return { auth, todos, summary, meta };
})();

