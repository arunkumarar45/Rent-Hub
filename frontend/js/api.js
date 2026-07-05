// Production-ready API client wrapper for RentHub REST backend
// API_BASE_URL is set by config.js (injected at runtime) or defaults to localhost
const API_BASE_URL = (window.RENTHUB_CONFIG && window.RENTHUB_CONFIG.apiBaseUrl)
    ? window.RENTHUB_CONFIG.apiBaseUrl
    : 'http://localhost:8080/api/v1';
const activeControllers = {};

class ApiError extends Error {
    constructor(message, status, data = null) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.data = data; // validation errors object
    }
}

async function apiFetch(path, options = {}) {
    // 1. Offline Detection
    if (!navigator.onLine) {
        if (window.Toast) {
            window.Toast.error("You are currently offline. Please check your network connection.");
        }
        throw new ApiError("Network connection offline", 0);
    }

    const url = `${API_BASE_URL}${path}`;
    options.headers = options.headers || {};

    // 2. Abort Controller to prevent duplicate requests / race conditions
    if (options.abortKey) {
        if (activeControllers[options.abortKey]) {
            activeControllers[options.abortKey].abort();
        }
        const controller = new AbortController();
        options.signal = controller.signal;
        activeControllers[options.abortKey] = controller;
    }

    // 3. Inject Authorization Header
    const token = localStorage.getItem('auth_token');
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }

    // Automatically set Content-Type for JSON payloads
    if (options.body && !(options.body instanceof FormData) && typeof options.body === 'object') {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }

    const retries = options.retries !== undefined ? options.retries : 2;
    const timeoutMs = options.timeout !== undefined ? options.timeout : 10000;

    let attempt = 0;
    while (attempt <= retries) {
        try {
            // 4. Timeout mechanism using AbortController and Promise.race
            const controller = new AbortController();
            const signal = options.signal || controller.signal;
            const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

            const fetchPromise = fetch(url, { ...options, signal });
            const response = await fetchPromise;
            clearTimeout(timeoutId);

            // Cleanup active controller
            if (options.abortKey && activeControllers[options.abortKey] && signal === options.signal) {
                delete activeControllers[options.abortKey];
            }

            // 5. Token expiration / 401 handling
            if (response.status === 401) {
                // Prevent infinite loop if we are already attempting refresh
                if (path === '/auth/refresh') {
                    handleLogoutRedirect();
                    throw new ApiError("Session expired", 401);
                }

                // Attempt to refresh JWT token
                const refreshSuccessful = await attemptTokenRefresh();
                if (refreshSuccessful) {
                    // Retry original call with new token
                    const newToken = localStorage.getItem('auth_token');
                    options.headers['Authorization'] = `Bearer ${newToken}`;
                    attempt++;
                    continue;
                } else {
                    handleLogoutRedirect();
                    throw new ApiError("Session expired. Please log in again.", 401);
                }
            }

            // 6. Handle HTTP errors
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const result = await response.json();
                if (!response.ok) {
                    throw new ApiError(result.message || 'API request failed', response.status, result.data);
                }
                return result;
            } else {
                if (!response.ok) {
                    const text = await response.text();
                    throw new ApiError(text || 'API request failed', response.status);
                }
                return response;
            }
        } catch (error) {
            // Ignore cancelled/aborted requests
            if (error.name === 'AbortError') {
                throw error;
            }

            attempt++;
            if (attempt > retries) {
                console.error(`API Fetch failed after ${attempt} attempts:`, error);
                throw error;
            }
            // Exponential backoff
            await new Promise(res => setTimeout(res, 200 * attempt));
        }
    }
}

// Function to call token refresh API
async function attemptTokenRefresh() {
    const refreshToken = localStorage.getItem('auth_refresh_token');
    if (!refreshToken) return false;

    try {
        const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (response.ok) {
            const data = await response.json();
            if (data && data.data && data.data.accessToken) {
                localStorage.setItem('auth_token', data.data.accessToken);
                if (data.data.refreshToken) {
                    localStorage.setItem('auth_refresh_token', data.data.refreshToken);
                }
                return true;
            }
        }
    } catch (err) {
        console.error("Token refresh failed:", err);
    }
    return false;
}

function handleLogoutRedirect() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_refresh_token');
    localStorage.removeItem('user_profile');
    
    const isPublic = window.location.pathname.endsWith('login.html') || 
                     window.location.pathname.endsWith('index.html') || 
                     window.location.pathname.endsWith('signup.html') || 
                     window.location.pathname.endsWith('marketplace.html') || 
                     window.location.pathname.endsWith('how-it-works.html');

    if (!isPublic) {
        window.location.href = 'login.html';
    }
}

window.apiFetch = apiFetch;
