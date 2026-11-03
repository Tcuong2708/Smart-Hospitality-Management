(function (window, document) {
    'use strict';

    const DEFAULT_INTERVAL = 12000;
    const MAX_BACKOFF = 48000;

    function compact(html) {
        return String(html || '').replace(/\s+/g, ' ').trim();
    }

    function userIsBusy(container) {
        if (document.hidden) return true;
        if (document.querySelector('.adm-modal.is-open, .modal.show, [role="dialog"][aria-hidden="false"]')) return true;
        const active = document.activeElement;
        return !!(active && container.contains(active) && /^(INPUT|SELECT|TEXTAREA|BUTTON)$/.test(active.tagName));
    }

    function start(options) {
        const selector = options && options.container;
        const container = selector ? document.querySelector(selector) : null;
        if (!container) return;

        const countElement = options.count ? document.querySelector(options.count) : null;
        const baseInterval = Math.max(Number(options.interval) || DEFAULT_INTERVAL, 8000);
        let delay = baseInterval;
        let timer = null;
        let controller = null;

        async function refresh() {
            if (userIsBusy(container)) {
                schedule(baseInterval);
                return;
            }

            controller = new AbortController();
            const timeout = window.setTimeout(() => controller.abort(), 8000);
            try {
                const response = await fetch(window.location.href, {
                    method: 'GET',
                    headers: {
                        'Accept': 'text/html',
                        'X-MayHotel-Background-Refresh': 'true'
                    },
                    cache: 'no-store',
                    credentials: 'same-origin',
                    signal: controller.signal
                });
                if (!response.ok) throw new Error('HTTP ' + response.status);

                const parsed = new DOMParser().parseFromString(await response.text(), 'text/html');
                const incoming = parsed.querySelector(selector);
                if (!incoming) throw new Error('Không tìm thấy vùng dữ liệu cập nhật');

                if (compact(container.innerHTML) !== compact(incoming.innerHTML)) {
                    container.innerHTML = incoming.innerHTML;
                    if (countElement && options.count) {
                        const incomingCount = parsed.querySelector(options.count);
                        if (incomingCount) countElement.textContent = incomingCount.textContent;
                    }
                    const search = options.search ? document.querySelector(options.search) : null;
                    if (search && search.value) search.dispatchEvent(new Event('input', { bubbles: true }));
                    document.dispatchEvent(new CustomEvent('mayhotel:live-updated', {
                        detail: { container: selector }
                    }));
                }
                delay = baseInterval;
            } catch (error) {
                if (error.name !== 'AbortError') {
                    console.debug('[MAY HOTEL] Tạm hoãn đồng bộ nền:', error.message);
                }
                delay = Math.min(delay * 2, MAX_BACKOFF);
            } finally {
                window.clearTimeout(timeout);
                controller = null;
                schedule(delay);
            }
        }

        function schedule(wait) {
            window.clearTimeout(timer);
            timer = window.setTimeout(refresh, wait);
        }

        document.addEventListener('visibilitychange', function () {
            if (!document.hidden) schedule(800);
        });
        window.addEventListener('beforeunload', function () {
            window.clearTimeout(timer);
            if (controller) controller.abort();
        }, { once: true });

        schedule(baseInterval);
    }

    window.MayHotelLiveRefresh = { start: start };
})(window, document);
