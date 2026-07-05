// Shared UI builders and State Controllers

const UI = {
    // Generate and inject skeleton loader cards or table rows into container
    showSkeleton(container, type = 'card-grid', count = 3) {
        if (!container) return;
        container.innerHTML = '';
        
        let skeletonHTML = '';
        if (type === 'card-grid') {
            for (let i = 0; i < count; i++) {
                skeletonHTML += `
                    <div class="card" style="padding: var(--space-3)">
                        <div class="skeleton skeleton-image mb-3"></div>
                        <div class="skeleton skeleton-text" style="width: 40%"></div>
                        <div class="skeleton skeleton-text" style="width: 80%"></div>
                        <div class="skeleton skeleton-text" style="width: 90%; height: 24px"></div>
                        <div class="skeleton skeleton-text" style="width: 30%"></div>
                    </div>
                `;
            }
        } else if (type === 'table') {
            skeletonHTML = `
                <div class="table-responsive">
                    <table>
                        <thead>
                            <tr>
                                <th style="width: 25%"><div class="skeleton skeleton-text" style="width: 50%"></div></th>
                                <th style="width: 25%"><div class="skeleton skeleton-text" style="width: 40%"></div></th>
                                <th style="width: 15%"><div class="skeleton skeleton-text" style="width: 60%"></div></th>
                                <th style="width: 15%"><div class="skeleton skeleton-text" style="width: 70%"></div></th>
                                <th style="width: 20%"><div class="skeleton skeleton-text" style="width: 80%"></div></th>
                            </tr>
                        </thead>
                        <tbody>
                            ${Array(count).fill().map(() => `
                                <tr>
                                    <td><div class="skeleton skeleton-text" style="width: 75%"></div></td>
                                    <td><div class="skeleton skeleton-text" style="width: 60%"></div></td>
                                    <td><div class="skeleton skeleton-text" style="width: 50%"></div></td>
                                    <td><div class="skeleton skeleton-text" style="width: 80%"></div></td>
                                    <td><div class="skeleton skeleton-text" style="width: 90%"></div></td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            `;
        } else if (type === 'metrics') {
            for (let i = 0; i < count; i++) {
                skeletonHTML += `
                    <div class="metric-card">
                        <div class="skeleton skeleton-text" style="width: 50%; height: 14px"></div>
                        <div class="skeleton skeleton-text" style="width: 80%; height: 32px; margin-top: var(--space-2)"></div>
                    </div>
                `;
            }
        }

        container.innerHTML = skeletonHTML;
    },

    // Inject illustrated, modern empty status cards into target container
    showEmptyState(container, title, message, actionBtnHTML = '') {
        if (!container) return;
        container.innerHTML = `
            <div style="text-align: center; padding: var(--space-12) var(--space-6); color: var(--text-secondary); width: 100%;">
                <div style="margin: 0 auto var(--space-4) auto; width: 64px; height: 64px; color: var(--text-muted); display: flex; align-items: center; justify-content: center;">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="m15 9-6 6M9 9l6 6"/></svg>
                </div>
                <h3 style="font-size: 1.15rem; font-weight: 600; color: var(--text-primary); margin-bottom: var(--space-1);">${Utils.sanitize(title)}</h3>
                <p style="font-size: 0.875rem; color: var(--text-secondary); margin-bottom: var(--space-4); max-width: 320px; margin-left: auto; margin-right: auto;">${Utils.sanitize(message)}</p>
                ${actionBtnHTML}
            </div>
        `;
    },

    // Show/Hide spinner loading indicator inside HTML buttons
    setButtonLoading(buttonEl, isLoading, defaultText) {
        if (!buttonEl) return;
        if (isLoading) {
            buttonEl.classList.add('btn-loading');
            buttonEl.disabled = true;
            buttonEl.textContent = '';
        } else {
            buttonEl.classList.remove('btn-loading');
            buttonEl.disabled = false;
            buttonEl.textContent = defaultText;
        }
    }
};

window.UI = UI;
