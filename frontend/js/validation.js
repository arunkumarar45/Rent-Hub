// Real-time Form Validation and Floating Label Helper Library

const Validation = {
    // Initialise floating label animations based on input states
    initFloatingLabels() {
        const groups = document.querySelectorAll('.floating-group');
        groups.forEach(group => {
            const input = group.querySelector('.form-input');
            const label = group.querySelector('.form-label');
            if (!input || !label) return;

            // Trigger float state check
            const checkValue = () => {
                if (input.value.trim() !== "" || input.placeholder !== " ") {
                    label.style.top = "6px";
                    label.style.transform = "translateY(0) scale(0.75)";
                } else {
                    label.style.top = "50%";
                    label.style.transform = "translateY(-50%) scale(1)";
                }
            };

            input.addEventListener('focus', () => {
                label.style.top = "6px";
                label.style.transform = "translateY(0) scale(0.75)";
                label.style.color = "var(--color-primary)";
            });

            input.addEventListener('blur', () => {
                checkValue();
                label.style.color = "var(--text-secondary)";
            });

            input.addEventListener('input', checkValue);
            // Initial check
            checkValue();
        });
    },

    // Email validation using RFC 5322 regex
    validateEmail(email) {
        const re = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return re.test(String(email).toLowerCase());
    },

    // North American and international phone regex check
    validatePhone(phone) {
        if (!phone) return true; // Optional fields can be empty
        const re = /^\+?[0-9\s\-()]{7,20}$/;
        return re.test(phone);
    },

    // Calculate password strength score (0-4)
    getPasswordStrength(password) {
        if (!password) return { score: 0, text: 'Empty', color: 'var(--text-muted)' };
        let score = 0;
        if (password.length >= 8) score++;
        if (/[A-Z]/.test(password)) score++;
        if (/[a-z]/.test(password) && /[0-9]/.test(password)) score++;
        if (/[^A-Za-z0-9]/.test(password)) score++;

        const levels = [
            { text: 'Too Short', color: 'var(--color-danger)' },
            { text: 'Weak', color: 'var(--color-danger)' },
            { text: 'Fair', color: 'var(--color-warning)' },
            { text: 'Good', color: 'var(--color-info)' },
            { text: 'Strong', color: 'var(--color-success)' }
        ];

        return {
            score,
            ...levels[score]
        };
    },

    // Show/Hide field-level validation errors
    setFieldError(inputEl, errorMsg) {
        const group = inputEl.closest('.floating-group');
        if (!group) return;

        let errorSpan = group.querySelector('.error-msg');
        if (!errorSpan) {
            errorSpan = document.createElement('span');
            errorSpan.className = 'error-msg';
            group.appendChild(errorSpan);
        }

        if (errorMsg) {
            group.classList.add('invalid');
            errorSpan.textContent = errorMsg;
            errorSpan.style.display = 'block';
        } else {
            group.classList.remove('invalid');
            errorSpan.style.display = 'none';
        }
    }
};

window.Validation = Validation;
