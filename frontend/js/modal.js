// Universal Modal Handling and Accessibility Controls

const Modal = {
    // Open a modal and its background backdrop
    open(modalId, backdropId) {
        const modal = document.getElementById(modalId);
        const backdrop = document.getElementById(backdropId);
        if (!modal || !backdrop) return;

        // Apply visibility classes
        backdrop.classList.add('visible');
        modal.classList.add('visible');

        // Prevent body scrolling
        document.body.style.overflow = 'hidden';

        // Setup accessibility focus trap
        this.trapFocus(modal);

        // Escape key list hook
        const escHandler = (e) => {
            if (e.key === 'Escape') {
                this.close(modalId, backdropId);
                document.removeEventListener('keydown', escHandler);
            }
        };
        document.addEventListener('keydown', escHandler);

        // Dismiss modal if clicking backdrop
        const clickHandler = (e) => {
            if (e.target === backdrop) {
                this.close(modalId, backdropId);
                backdrop.removeEventListener('click', clickHandler);
            }
        };
        backdrop.addEventListener('click', clickHandler);
    },

    // Close modal and backdrop
    close(modalId, backdropId) {
        const modal = document.getElementById(modalId);
        const backdrop = document.getElementById(backdropId);
        if (modal) modal.classList.remove('visible');
        if (backdrop) backdrop.classList.remove('visible');

        // Restore scroll
        document.body.style.overflow = '';
    },

    // Accessibility focus trap inside modal window
    trapFocus(modalElement) {
        const focusableElements = modalElement.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
        if (focusableElements.length === 0) return;

        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        modalElement.addEventListener('keydown', function(e) {
            if (e.key !== 'Tab') return;

            if (e.shiftKey) { // Shift + Tab
                if (document.activeElement === firstElement) {
                    lastElement.focus();
                    e.preventDefault();
                }
            } else { // Tab
                if (document.activeElement === lastElement) {
                    firstElement.focus();
                    e.preventDefault();
                }
            }
        });

        // Autofocus first element
        firstElement.focus();
    }
};

window.Modal = Modal;
