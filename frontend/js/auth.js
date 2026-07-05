// Authentication Utility Library for RentHub

const Auth = {
    // Save token and user details to localStorage (handles both 2 and 3 argument formats)
    login(token, refreshTokenOrUser, userObj) {
        localStorage.setItem('auth_token', token);
        if (typeof refreshTokenOrUser === 'string') {
            localStorage.setItem('auth_refresh_token', refreshTokenOrUser);
            localStorage.setItem('user_profile', JSON.stringify(userObj));
        } else {
            localStorage.setItem('user_profile', JSON.stringify(refreshTokenOrUser));
        }
    },

    // Clear session details
    logout() {
        // Send a call to backend to revoke refresh token if possible
        const refreshToken = localStorage.getItem('auth_refresh_token');
        if (refreshToken) {
            fetch('http://localhost:8080/api/v1/auth/logout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            }).catch(() => {});
        }

        localStorage.removeItem('auth_token');
        localStorage.removeItem('auth_refresh_token');
        localStorage.removeItem('user_profile');
        window.location.href = 'index.html';
    },

    // Check if user is currently logged in
    isLoggedIn() {
        return !!localStorage.getItem('auth_token');
    },

    // Get current access token
    getToken() {
        return localStorage.getItem('auth_token');
    },

    // Get current logged-in user profile
    getUser() {
        const profile = localStorage.getItem('user_profile');
        return profile ? JSON.parse(profile) : null;
    },

    // Require user session or redirect to login.html
    requireAuth() {
        if (!this.isLoggedIn()) {
            window.location.href = 'login.html?redirect=' + encodeURIComponent(window.location.pathname + window.location.search);
        }
    },

    // Require specific role or throw error / redirect
    requireOwner() {
        this.requireAuth();
        const user = this.getUser();
        if (!user || !user.isOwner) {
            window.location.href = 'become-owner.html';
        }
    },

    // Require admin privileges
    requireAdmin() {
        this.requireAuth();
        const user = this.getUser();
        if (!user || !user.roles || !user.roles.includes('ROLE_ADMIN')) {
            window.location.href = 'index.html';
        }
    }
};

window.Auth = Auth;
