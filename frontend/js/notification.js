// Toast Notification and Navbar Alerts Engine

const Toast = {
    // Create toast elements dynamically
    show(title, message, type = 'info') {
        let container = document.getElementById('toast-root-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-root-container';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        toast.innerHTML = `
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-msg">${message}</div>
            </div>
            <button class="toast-close">&times;</button>
        `;

        container.appendChild(toast);

        // Auto remove
        const removeTimeout = setTimeout(() => {
            toast.style.animation = 'fadeOut 200ms forwards';
            toast.addEventListener('animationend', () => toast.remove());
        }, 4000);

        // Close on click
        toast.querySelector('.toast-close').addEventListener('click', () => {
            clearTimeout(removeTimeout);
            toast.remove();
        });
    },

    success(message, title = 'Success') { this.show(title, message, 'success'); },
    error(message, title = 'Error') { this.show(title, message, 'error'); },
    warning(message, title = 'Warning') { this.show(title, message, 'warning'); },
    info(message, title = 'Info') { this.show(title, message, 'info'); }
};

const NotificationManager = {
    // Fetch count of unread notifications from REST backend
    async updateNavbarUnreadCount() {
        if (!window.Auth || !Auth.isLoggedIn()) return;
        try {
            const res = await apiFetch('/notifications/unread-count');
            const countBadge = document.getElementById('notification-unread-count');
            if (countBadge && res && res.data !== undefined) {
                const count = res.data;
                if (count > 0) {
                    countBadge.textContent = count;
                    countBadge.style.display = 'flex';
                } else {
                    countBadge.style.display = 'none';
                }
            }
        } catch (err) {
            console.error("Failed to load notifications unread count:", err);
        }
    },

    // Load unread list inside navbar dropdown
    async loadNavbarNotificationsList() {
        if (!window.Auth || !Auth.isLoggedIn()) return;
        const panelList = document.getElementById('notifications-dropdown-list');
        if (!panelList) return;

        panelList.innerHTML = `<div class="notification-empty">Loading notifications…</div>`;

        try {
            const res = await apiFetch('/notifications');
            panelList.innerHTML = '';

            if (res && res.data && res.data.length > 0) {
                res.data.slice(0, 5).forEach(noti => {
                    const item = document.createElement('div');
                    item.className = `notification-panel-item ${noti.isRead ? '' : 'unread'}`;
                    item.onclick = () => this.markAsRead(noti.id);
                    item.innerHTML = `
                        <div class="notification-item-title">${Utils.sanitize(noti.title)}</div>
                        <div class="notification-item-desc">${Utils.sanitize(noti.message)}</div>
                    `;
                    panelList.appendChild(item);
                });
            } else {
                panelList.innerHTML = `<div class="notification-empty">All caught up!</div>`;
            }
        } catch (err) {
            panelList.innerHTML = `<div class="notification-empty text-danger">Failed to load alerts</div>`;
        }
    },

    // Mark single notification as read
    async markAsRead(id) {
        try {
            await apiFetch(`/notifications/${id}/read`, { method: 'PATCH' });
            await this.updateNavbarUnreadCount();
            await this.loadNavbarNotificationsList();
        } catch (err) {
            console.error("Failed to mark read:", err);
        }
    }
};

window.Toast = Toast;
window.NotificationManager = NotificationManager;
