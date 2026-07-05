// Dynamic Header & Footer Renderer with active menus, user status and unread notification bell

document.addEventListener('DOMContentLoaded', () => {
    renderNavbar();
    renderFooter();
    
    // Sync unread notification count
    if (window.NotificationManager) {
        NotificationManager.updateNavbarUnreadCount();
        // Periodically check (every 60 seconds)
        setInterval(() => NotificationManager.updateNavbarUnreadCount(), 60000);
    }
});

function renderNavbar() {
    const navbarContainer = document.getElementById('navbar-mount');
    if (!navbarContainer) return;

    const isLoggedIn = Auth.isLoggedIn();
    const user = Auth.getUser();
    const isOwner = user ? user.isOwner : false;
    const initial = user && user.firstName ? user.firstName.charAt(0).toUpperCase() : 'U';

    const navbarHTML = `
        <nav class="navbar">
            <div class="nav-container">
                <a href="index.html" class="nav-logo">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="color: var(--color-primary);"><path d="M11 21.88a1.75 1.75 0 0 1-1-1.45V10.13a1.75 1.75 0 0 1 1-1.45l7.5-4a1.75 1.75 0 0 1 2 0l7.5 4a1.75 1.75 0 0 1 1 1.45v10.3a1.75 1.75 0 0 1-1 1.45l-7.5 4a1.75 1.75 0 0 1-2 0z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
                    <span>RentHub</span>
                </a>
                
                <div style="display: flex; gap: var(--space-4); align-items: center;">
                    <a href="marketplace.html" class="nav-link" id="nav-marketplace">Marketplace</a>
                    <a href="how-it-works.html" class="nav-link" id="nav-how">How it Works</a>
                </div>

                <div class="search-wrapper">
                    <span class="input-icon">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                    </span>
                    <input type="text" id="nav-search-input" class="input-field" placeholder="Search gear (e.g. Sony A7)...">
                </div>

                <div class="nav-links">
                    ${isLoggedIn ? `
                        <!-- Notifications Bell -->
                        <div class="notification-bell-container" id="bell-container" aria-label="Notifications Panel" role="button">
                            <span class="nav-link" style="display: flex; align-items: center; padding: 6px;">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                            </span>
                            <span class="bell-badge" id="notification-unread-count" style="display: none;">0</span>
                            
                            <!-- Notifications Dropdown -->
                            <div class="notifications-panel" id="notifications-panel-dropdown">
                                <div class="notifications-panel-header">
                                    <h4>Recent Notifications</h4>
                                </div>
                                <div id="notifications-dropdown-list">
                                    <div class="notification-empty">Loading notifications…</div>
                                </div>
                            </div>
                        </div>

                        <!-- User Profile Menu -->
                        <div class="user-menu-container">
                            <div class="user-avatar" id="avatar-btn">${initial}</div>
                            <div class="user-menu-dropdown" id="user-dropdown-menu">
                                <div style="padding: var(--space-2) var(--space-3); border-bottom: 1px solid var(--border-color); margin-bottom: var(--space-1);">
                                    <div style="font-weight: 600; font-size: 0.85rem;">${Utils.sanitize(user.firstName)} ${Utils.sanitize(user.lastName)}</div>
                                    <div style="font-size: 0.75rem; color: var(--text-secondary);">${Utils.sanitize(user.email)}</div>
                                </div>
                                <a href="dashboard.html" class="user-menu-item">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="9"></rect><rect x="14" y="3" width="7" height="5"></rect><rect x="14" y="12" width="7" height="9"></rect><rect x="3" y="16" width="7" height="5"></rect></svg>
                                    Dashboard
                                </a>
                                <a href="become-owner.html" class="user-menu-item">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
                                    List Your Gear
                                </a>
                                <a href="#" id="logout-btn" class="user-menu-item" style="color: var(--color-danger); border-top: 1px solid var(--border-color); margin-top: var(--space-1); border-radius: 0 0 var(--radius-sm) var(--radius-sm);">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                                    Sign Out
                                </a>
                            </div>
                        </div>
                    ` : `
                        <a href="login.html" class="nav-link" style="display: flex; align-items: center; gap: 4px;">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                            Sign In
                        </a>
                        <a href="become-owner.html" class="btn btn-primary btn-sm">List Gear</a>
                    `}
                </div>
            </div>
        </nav>
    `;
    
    navbarContainer.innerHTML = navbarHTML;

    // Set active link highlight
    const path = window.location.pathname;
    if (path.includes('marketplace.html') && document.getElementById('nav-marketplace')) {
        document.getElementById('nav-marketplace').classList.add('active');
    } else if (path.includes('how-it-works.html') && document.getElementById('nav-how')) {
        document.getElementById('nav-how').classList.add('active');
    }

    // Attach search trigger
    const searchInput = document.getElementById('nav-search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && searchInput.value.trim() !== '') {
                window.location.href = `marketplace.html?search=${encodeURIComponent(searchInput.value.trim())}`;
            }
        });
    }

    // Attach profile dropdown togglers
    const avatarBtn = document.getElementById('avatar-btn');
    const dropdownMenu = document.getElementById('user-dropdown-menu');
    if (avatarBtn && dropdownMenu) {
        avatarBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            dropdownMenu.classList.toggle('active');
            // Close notification panel if open
            const notiDropdown = document.getElementById('notifications-panel-dropdown');
            if (notiDropdown) notiDropdown.classList.remove('active');
        });
    }

    // Notifications dropdown togglers
    const bellContainer = document.getElementById('bell-container');
    const notiDropdown = document.getElementById('notifications-panel-dropdown');
    if (bellContainer && notiDropdown) {
        bellContainer.addEventListener('click', (e) => {
            e.stopPropagation();
            notiDropdown.classList.toggle('active');
            // Close profile dropdown if open
            if (dropdownMenu) dropdownMenu.classList.remove('active');
            
            if (notiDropdown.classList.contains('active') && window.NotificationManager) {
                NotificationManager.loadNavbarNotificationsList();
            }
        });
    }

    // Close dropdowns on body click
    document.addEventListener('click', () => {
        if (dropdownMenu) dropdownMenu.classList.remove('active');
        if (notiDropdown) notiDropdown.classList.remove('active');
    });

    // Attach logout trigger
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            Auth.logout();
        });
    }
}

function renderFooter() {
    const footerContainer = document.getElementById('footer-mount');
    if (!footerContainer) return;

    footerContainer.innerHTML = `
        <footer class="footer">
            <div class="footer-container">
                <div style="display: flex; align-items: center; gap: 8px;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="color: var(--color-primary);"><path d="M11 21.88a1.75 1.75 0 0 1-1-1.45V10.13a1.75 1.75 0 0 1 1-1.45l7.5-4a1.75 1.75 0 0 1 2 0l7.5 4a1.75 1.75 0 0 1 1 1.45v10.3a1.75 1.75 0 0 1-1 1.45l-7.5 4a1.75 1.75 0 0 1-2 0z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
                    <span style="font-weight: 700; color: var(--text-primary);">RentHub</span>
                </div>
                <div style="font-size: 0.8rem; color: var(--text-secondary);">
                    &copy; ${new Date().getFullYear()} RentHub Inc. Peer-to-Peer Premium Equipment sharing. Built for Professionals.
                </div>
            </div>
        </footer>
    `;
}
