(function () {
    function createToast(message, type) {
        var container = document.getElementById('toast-container');
        if (!container) return;
        var t = document.createElement('div');
        t.className = 'toast ' + (type || '');
        var msg = document.createElement('div');
        msg.className = 'msg';
        msg.textContent = message;
        var close = document.createElement('button');
        close.className = 'close';
        close.textContent = '×';
        close.setAttribute('aria-label', 'Fechar');
        close.onclick = function () { if (container.contains(t)) container.removeChild(t); };
        t.appendChild(msg);
        t.appendChild(close);
        container.appendChild(t);
        setTimeout(function () { if (container.contains(t)) container.removeChild(t); }, 6000);
    }

    function showFlashToasts() {
        var s = document.getElementById('flash-success');
        var e = document.getElementById('flash-error');
        if (s && s.textContent.trim().length) createToast(s.textContent.trim(), 'success');
        if (e && e.textContent.trim().length) createToast(e.textContent.trim(), 'error');
    }

    // Expose a tiny API
    window.Toast = {
        create: createToast,
        showFlash: showFlashToasts
    };

    document.addEventListener('DOMContentLoaded', showFlashToasts);
})();
