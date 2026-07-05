// Utility Helpers for RentHub Frontend

const Utils = {
    // Format numeric cents into USD currency string
    formatCurrency(amountInCents) {
        if (amountInCents === null || amountInCents === undefined) return '$0.00';
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amountInCents / 100);
    },

    // Format ISO string dates into readable dates
    formatDate(dateStr, showTime = false) {
        if (!dateStr) return '—';
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return dateStr;

        const options = {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        };

        if (showTime) {
            options.hour = '2-digit';
            options.minute = '2-digit';
        }

        return date.toLocaleDateString('en-US', options);
    },

    // Debounce calls to limit API requests (e.g. keyboard searches)
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },

    // Throttle calls to limit frequency (e.g. click spam prevention)
    throttle(func, limit) {
        let inThrottle;
        return function executedFunction(...args) {
            if (!inThrottle) {
                func(...args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },

    // Sanitize user-provided HTML inputs to prevent XSS vulnerability
    sanitize(str) {
        if (!str) return '';
        const temp = document.createElement('div');
        temp.textContent = str;
        return temp.innerHTML;
    }
};
window.Utils = Utils;
