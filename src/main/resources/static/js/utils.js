/**
 * Utility Functions
 * Shared helpers for formatting, DOM manipulation, etc.
 */
const Utils = (() => {
    /**
     * Format date for display.
     */
    const formatDate = (dateString) => {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    };

    /**
     * Format date-time for display.
     */
    const formatDateTime = (dateString) => {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    /**
     * Format date for datetime-local input.
     */
    const formatDateForInput = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        return date.toISOString().slice(0, 16);
    };

    /**
     * Format enum value to display label.
     * Converts ENUM_NAME to "Enum Name".
     */
    const formatEnumLabel = (value) => {
        if (!value) return '';
        return value
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
            .join(' ');
    };

    /**
     * Check if a date is overdue.
     */
    const isOverdue = (dueDate, status) => {
        if (!dueDate || status === 'COMPLETED' || status === 'CANCELLED') return false;
        const due = new Date(dueDate);
        const now = new Date();
        // Set both to start of day for comparison
        due.setHours(23, 59, 59, 999);
        return due < now;
    };

    /**
     * Check if a date is today.
     */
    const isDueToday = (dueDate, status) => {
        if (!dueDate || status === 'COMPLETED' || status === 'CANCELLED') return false;
        const due = new Date(dueDate);
        const now = new Date();
        return due.toDateString() === now.toDateString();
    };

    /**
     * Get due indicator class based on due date.
     * Returns: 'overdue', 'today', 'upcoming', or null
     */
    const getDueIndicator = (dueDate, status) => {
        if (!dueDate || status === 'COMPLETED' || status === 'CANCELLED') return null;
        if (isOverdue(dueDate, status)) return 'overdue';
        if (isDueToday(dueDate, status)) return 'today';
        return 'upcoming';
    };

    /**
     * Escape HTML to prevent XSS.
     */
    const escapeHtml = (text) => {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    };

    /**
     * Format markdown text to HTML using marked.js library.
     */
    const formatMarkdown = (text) => {
        if (!text) return '';

        marked.setOptions({
            breaks: true,
            gfm: true,
            headerIds: false,
            mangle: false
        });

        return marked.parse(text);
    };

    /**
     * Debounce function.
     */
    const debounce = (func, wait) => {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), wait);
        };
    };

    /**
     * Show loading state on button.
     */
    const setButtonLoading = (button, loading) => {
        if (loading) {
            button.disabled = true;
            button.dataset.originalText = button.innerHTML;
            button.innerHTML = '<span class="spinner"></span>';
        } else {
            button.disabled = false;
            button.innerHTML = button.dataset.originalText || button.innerHTML;
        }
    };

    return {
        formatDate,
        formatDateTime,
        formatDateForInput,
        formatEnumLabel,
        isOverdue,
        isDueToday,
        getDueIndicator,
        escapeHtml,
        formatMarkdown,
        debounce,
        setButtonLoading
    };
})();

/**
 * Toast Notification System
 */
const Toast = (() => {
    const container = document.getElementById('toastContainer');

    const show = (message, type = 'info', duration = 4000) => {
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `
            <span class="toast-message">${Utils.escapeHtml(message)}</span>
            <button class="toast-close" aria-label="Close">&times;</button>
        `;

        const closeBtn = toast.querySelector('.toast-close');
        closeBtn.addEventListener('click', () => removeToast(toast));

        container.appendChild(toast);

        setTimeout(() => removeToast(toast), duration);
    };

    const removeToast = (toast) => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 200);
    };

    return {
        success: (msg) => show(msg, 'success'),
        error: (msg) => show(msg, 'error'),
        warning: (msg) => show(msg, 'warning'),
        info: (msg) => show(msg, 'info')
    };
})();

/**
 * Modal Manager
 */
const Modal = (() => {
    const open = (modalId) => {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('active');
            document.body.style.overflow = 'hidden';
        }
    };

    const close = (modalId) => {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('active');
            document.body.style.overflow = '';
        }
    };

    const init = (modalId, openBtnId, closeBtnIds = []) => {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        // Open button
        if (openBtnId) {
            const openBtn = document.getElementById(openBtnId);
            if (openBtn) {
                openBtn.addEventListener('click', () => open(modalId));
            }
        }

        // Close buttons
        closeBtnIds.forEach(btnId => {
            const btn = document.getElementById(btnId);
            if (btn) {
                btn.addEventListener('click', () => close(modalId));
            }
        });

        // Close on overlay click
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                close(modalId);
            }
        });

        // Close on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && modal.classList.contains('active')) {
                close(modalId);
            }
        });
    };

    return { open, close, init };
})();

/**
 * Component Renderer
 * Creates reusable UI components.
 */
const Components = (() => {
    /**
     * Render a status badge.
     */
    const statusBadge = (status) => {
        const label = Utils.formatEnumLabel(status);
        return `<span class="badge badge-status-${status.toLowerCase()}">${label}</span>`;
    };

    /**
     * Render a priority badge.
     */
    const priorityBadge = (priority) => {
        const label = Utils.formatEnumLabel(priority);
        return `<span class="badge badge-priority-${priority.toLowerCase()}">${label}</span>`;
    };

    return {
        statusBadge,
        priorityBadge
    };
})();

/**
 * Dropdown Loader
 * Populates select elements from API.
 */
const DropdownLoader = (() => {
    let statusesCache = null;
    let prioritiesCache = null;

    const loadStatuses = async () => {
        if (statusesCache) return statusesCache;
        try {
            statusesCache = await Api.meta.statuses();
            return statusesCache;
        } catch (e) {
            console.error('Failed to load statuses:', e);
            return [];
        }
    };

    const loadPriorities = async () => {
        if (prioritiesCache) return prioritiesCache;
        try {
            prioritiesCache = await Api.meta.priorities();
            return prioritiesCache;
        } catch (e) {
            console.error('Failed to load priorities:', e);
            return [];
        }
    };

    const populateSelect = (selectEl, items, includeAll = false, allLabel = 'All') => {
        if (!selectEl) return;

        selectEl.innerHTML = '';

        if (includeAll) {
            const opt = document.createElement('option');
            opt.value = '';
            opt.textContent = allLabel;
            selectEl.appendChild(opt);
        }

        items.forEach(item => {
            const opt = document.createElement('option');
            opt.value = item.value;
            opt.textContent = item.label;
            selectEl.appendChild(opt);
        });
    };

    return {
        loadStatuses,
        loadPriorities,
        populateSelect
    };
})();

