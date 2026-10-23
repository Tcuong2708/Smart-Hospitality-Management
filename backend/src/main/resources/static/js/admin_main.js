'use strict';

const CFG = {
    base: window.API_BASE_URL || 'http://localhost:8080/api',
    ai: window.AI_URL || '',
    poll: 3000,
    tz: 'Asia/Ho_Chi_Minh',
};

// ============================================================
// ADM MODAL / TOAST
// ============================================================
const AdmModal = {
    open(target) {
        const el = typeof target === 'string' ? document.getElementById(target) : target;
        if (!el) return;
        el.classList.add('is-open');
        document.body.style.overflow = 'hidden';
        el.dispatchEvent(new CustomEvent('adm:modal-opened'));
    },
    close(target) {
        const el = typeof target === 'string' ? document.getElementById(target) : target;
        if (!el) return;
        el.classList.remove('is-open');
        document.body.style.overflow = '';
        el.dispatchEvent(new CustomEvent('adm:modal-closed'));
    },
    isOpen(target) {
        const el = typeof target === 'string' ? document.getElementById(target) : target;
        return el ? el.classList.contains('is-open') : false;
    }
};

const AdmToast = {
    container: null,
    _ensure() {
        if (this.container && document.body.contains(this.container)) return;
        this.container = document.createElement('div');
        this.container.className = 'adm-toast-container';
        document.body.appendChild(this.container);
    },
    show(msg, type = 'success') {
        this._ensure();
        const icons = {
            success: 'check-circle-fill',
            danger: 'x-circle-fill',
            warning: 'exclamation-triangle-fill',
            info: 'info-circle-fill'
        };
        const t = document.createElement('div');
        t.className = 'adm-toast adm-toast-' + type;
        t.innerHTML = '<i class="bi bi-' + (icons[type] || icons.success) + '"></i>'
            + '<span class="adm-toast-text">' + esc(msg) + '</span>';
        this.container.appendChild(t);
        setTimeout(function () {
            t.style.opacity = '0';
            t.style.transform = 'translateX(20px)';
            t.style.transition = 'all .3s ease';
            setTimeout(function () { t.remove(); }, 300);
        }, 4000);
    }
};

function toast(msg, type) {
    if (type === 'error') type = 'danger';
    AdmToast.show(msg, type || 'success');
}

// ============================================================
// SHELL — sidebar toggle, modal click, dropdown, ESC
// ============================================================
(function initShell() {
    function ready(fn) {
        if (document.readyState !== 'loading') fn();
        else document.addEventListener('DOMContentLoaded', fn);
    }

    ready(function () {
        var toggle = document.getElementById('admSidebarToggle');
        var sidebar = document.getElementById('admSidebar');
        var overlay = document.getElementById('admSidebarOverlay');

        if (toggle) toggle.addEventListener('click', function () {
            if (sidebar) sidebar.classList.toggle('is-open');
            if (overlay) overlay.classList.toggle('is-active');
        });
        if (overlay) overlay.addEventListener('click', function () {
            if (sidebar) sidebar.classList.remove('is-open');
            overlay.classList.remove('is-active');
        });

        document.addEventListener('click', function (e) {
            var openBtn = e.target.closest('[data-adm-open]');
            if (openBtn) AdmModal.open(openBtn.getAttribute('data-adm-open'));

            var closeBtn = e.target.closest('[data-adm-close]');
            if (closeBtn) {
                var m = closeBtn.closest('.adm-modal-backdrop');
                if (m) AdmModal.close(m);
            }

            if (e.target.classList && e.target.classList.contains('adm-modal-backdrop')) {
                AdmModal.close(e.target);
            }

            var ddToggle = e.target.closest('[data-adm-dropdown]');
            if (ddToggle) {
                var parent = ddToggle.closest('.adm-dropdown');
                if (parent) {
                    document.querySelectorAll('.adm-dropdown.is-open').forEach(function (d) {
                        if (d !== parent) d.classList.remove('is-open');
                    });
                    parent.classList.toggle('is-open');
                }
            } else if (!e.target.closest('.adm-dropdown-menu')) {
                document.querySelectorAll('.adm-dropdown.is-open').forEach(function (d) {
                    d.classList.remove('is-open');
                });
            }
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                document.querySelectorAll('.adm-modal-backdrop.is-open').forEach(function (m) {
                    AdmModal.close(m);
                });
            }
        });
    });
})();

// ============================================================
// HELPERS
// ============================================================
async function api(path, opts = {}) {
    const url = path.startsWith('http') ? path : CFG.base + path;
    const res = await fetch(url, {
        ...opts,
        headers: { 'Content-Type': 'application/json', ...(opts.headers || {}) },
    });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const txt = await res.text();
    return txt ? JSON.parse(txt) : null;
}

const docId = d => d?._id ?? d?.id;
const money = v => new Intl.NumberFormat('vi-VN').format(v || 0) + ' đ';

function dateVN(input) {
    if (!input) return '--/--/----';
    const d = new Date(input);
    if (isNaN(d)) return String(input);
    return d.toLocaleDateString('vi-VN', { timeZone: CFG.tz });
}

function dateTimeVN(input) {
    if (!input) return '--/--/---- --:--';
    const d = new Date(input);
    if (isNaN(d)) return String(input);
    const opts = { timeZone: CFG.tz, day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false };
    const parts = new Intl.DateTimeFormat('vi-VN', opts).formatToParts(d);
    const g = k => parts.find(p => p.type === k)?.value || '';
    return `${g('day')}/${g('month')}/${g('year')} ${g('hour')}:${g('minute')}`;
}

function esc(s) {
    if (s == null) return '';
    const d = document.createElement('div');
    d.textContent = String(s);
    return d.innerHTML;
}

function busy(btn, on, label = 'Đang xử lý...') {
    if (!btn) return;
    if (on) {
        btn.dataset.html = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="adm-spinner"></span>' + label;
    } else {
        btn.disabled = false;
        if (btn.dataset.html) btn.innerHTML = btn.dataset.html;
    }

    // ============================================================
    // SHARED TABLE PAGINATION
    // ============================================================
    const AdmPagination = window.AdmPagination = {
        init(options) {
            const table = typeof options.table === 'string'
                ? document.querySelector(options.table) : options.table;
            if (!table) return null;
            const body = table.tBodies[0];
            if (!body) return null;
            const state = {
                table,
                body,
                rowSelector: options.rowSelector || 'tr',
                pageSize: options.pageSize || 10,
                page: 1,
                container: typeof options.container === 'string'
                    ? document.querySelector(options.container) : options.container,
                count: typeof options.count === 'string'
                    ? document.querySelector(options.count) : options.count
            };

            state.refresh = function () {
                const rows = Array.from(state.body.querySelectorAll(state.rowSelector));
                const visible = rows.filter(row => row.dataset.paginationHidden !== 'true');
                const pages = Math.max(1, Math.ceil(visible.length / state.pageSize));
                state.page = Math.min(state.page, pages);
                rows.forEach(row => {
                    row.style.display = 'none';
                });
                visible.slice((state.page - 1) * state.pageSize, state.page * state.pageSize)
                    .forEach(row => { row.style.display = ''; });
                if (state.count) state.count.textContent = visible.length;
                renderPagination(state, pages);
            };
            state.goTo = function (page) {
                state.page = page;
                state.refresh();
            };
            table._admPagination = state;
            state.refresh();
            return state;
        }
    };

    function renderPagination(state, pages) {
        if (!state.container) return;
        if (pages <= 1) {
            state.container.innerHTML = '';
            state.container.style.display = 'none';
            return;
        }
        state.container.style.display = 'flex';
        let html = '<button type="button" class="adm-pagination-btn" data-page="prev"'
            + (state.page === 1 ? ' disabled' : '') + ' aria-label="Trang trước">'
            + '<i class="bi bi-chevron-left"></i></button>';
        for (let page = 1; page <= pages; page++) {
            html += '<button type="button" class="adm-pagination-btn'
                + (page === state.page ? ' is-active' : '') + '" data-page="' + page + '">'
                + page + '</button>';
        }
        html += '<button type="button" class="adm-pagination-btn" data-page="next"'
            + (state.page === pages ? ' disabled' : '') + ' aria-label="Trang sau">'
            + '<i class="bi bi-chevron-right"></i></button>';
        state.container.innerHTML = html;
        state.container.querySelectorAll('[data-page]').forEach(button => {
            button.addEventListener('click', () => {
                if (button.disabled) return;
                const value = button.dataset.page;
                state.goTo(value === 'prev' ? state.page - 1
                    : value === 'next' ? state.page + 1 : Number(value));
            });
        });
    }
}

function tangCua(name) {
    if (!name) return 0;
    const d = String(name).replace(/\D/g, '');
    if (!d) return 0;
    if (d.length <= 3) return parseInt(d[0], 10) || 0;
    return parseInt(d.slice(0, d.length - 2), 10) || 0;
}

// ============================================================
// HÓA ĐƠN
// ============================================================
const HoaDon = {
    init() {
        const modalEl = document.getElementById('createInvoiceModal');
        if (modalEl) {
            modalEl.addEventListener('adm:modal-closed', () => {
                document.getElementById('createInvoiceForm')?.reset();
            });
        }

        document.getElementById('btn-submit-invoice')?.addEventListener('click', () => {
            const form = document.getElementById('createInvoiceForm');
            if (!form) return;
            if (!form.checkValidity()) return form.reportValidity();
            AdmModal.close(modalEl);
            toast('Tạo hóa đơn thành công');
        });

        this.tai();
    },

    async tai() {
        const tbody = document.getElementById('invoice-table-body');
        if (!tbody) return;

        try {
            const list = (await api('/invoices')) || [];

            if (!list.length) {
                tbody.innerHTML = `<tr><td colspan="7">
                    <div class="adm-empty">
                        <i class="bi bi-clipboard-x"></i>
                        <h4>Chưa có hóa đơn nào</h4>
                        <p>Hóa đơn sẽ xuất hiện sau khi khách đặt phòng hoặc thanh toán.</p>
                    </div></td></tr>`;
                return;
            }

            const now = new Date();
            tbody.innerHTML = list.map(inv => {
                const id = docId(inv);
                const out = inv.checkOutDate ? new Date(inv.checkOutDate) : null;
                const daTra = out && out < now;
                const paid = inv.invoice_status === 'Completed' || daTra;

                const badgeCls = paid ? 'adm-badge-success' : 'adm-badge-info';
                const badgeTxt = paid ? 'Đã trả phòng' : 'Đang lưu trú';

                return `<tr>
                    <td class="cell-id text-center">#${esc(id)}</td>
                    <td>
                        <div class="fw-bold text-navy">${esc(inv.guestName || '—')}</div>
                        <div class="text-muted" style="font-size:12px;">
                            <i class="bi bi-telephone-fill" style="font-size:10px;"></i> ${esc(inv.phone || '')}
                        </div>
                    </td>
                    <td class="text-center text-muted">${dateVN(inv.created_at)}</td>
                    <td class="text-center" style="font-size:12.5px;">
                        <div><i class="bi bi-box-arrow-in-right text-success"></i> ${dateVN(inv.checkInDate)}</div>
                        <div><i class="bi bi-box-arrow-left text-danger"></i> ${dateVN(inv.checkOutDate)}</div>
                    </td>
                    <td class="text-right cell-price">${money(inv.total_amount)}</td>
                    <td class="text-center">
                        <span class="adm-badge ${badgeCls}"><span class="dot"></span>${badgeTxt}</span>
                    </td>
                    <td class="text-center">
                        <div class="adm-actions">
                            <a href="/receptionist/invoices/details/${id}" class="adm-action view" title="Chi tiết">
                                <i class="bi bi-eye"></i>
                            </a>
                            <a href="/receptionist/invoices/print/${id}" target="_blank" class="adm-action view" title="In">
                                <i class="bi bi-printer"></i>
                            </a>
                            <a href="/receptionist/invoices/delete/${id}" class="adm-action delete"
                               title="Xóa" onclick="return confirm('Xóa hóa đơn này?')">
                                <i class="bi bi-trash"></i>
                            </a>
                        </div>
                    </td>
                </tr>`;
            }).join('');
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="7">
                <div class="adm-empty">
                    <i class="bi bi-exclamation-triangle" style="color:var(--red);"></i>
                    <h4>Không tải được danh sách hóa đơn</h4>
                    <p>Vui lòng kiểm tra kết nối và thử lại.</p>
                </div></td></tr>`;
        }
    },
};

// ============================================================
// SƠ ĐỒ PHÒNG
// ============================================================
const SoDoPhong = {
    rooms: [],
    types: [],
    transferPrice: 0,

    async init() {
        this.map = document.getElementById('room-map-container');
        this.spinner = document.getElementById('loading-spinner');
        if (!this.map) return;

        try {
            const [rooms, types] = await Promise.all([
                api('/rooms'),
                api('/room-types'),
            ]);
            this.types = types || [];
            const typeMap = new Map(this.types.map(t => [t._id, t]));

            this.rooms = (rooms || []).map(r => {
                const t = typeMap.get(r.room_type_id);
                return {
                    ...r,
                    id: docId(r),
                    maLoai: t?.name || '—',
                    sucChua: t?.max_occupancy || 2,
                    gia: r.price || 0,
                    view: r.view_direction || 'Hướng Thành Phố',
                    _floor: tangCua(r.name),
                };
            });
        } catch {
            this.rooms = [];
        }

        this.initFilters();
        this.initTransferModal();
        this.ve(this.rooms);
    },

    _status(r) {
        if (r.status === 'Trống') return { code: 1, cls: 'status-empty' };
        if (r.status === 'Đang ở') return { code: 2, cls: 'status-occupied' };
        return { code: 3, cls: 'status-dirty' };
    },

    initFilters() {
        const setup = (el, html) => {
            if (!el) return;
            el.innerHTML = html;
            el.addEventListener('click', e => e.stopPropagation());
        };

        const floors = [...new Set(this.rooms.map(r => r._floor))].sort((a, b) => a - b);
        setup(document.getElementById('filterFloorDropdown'),
            floors.map((f, i) => `<label for="cb_floor_${i}">
                <input type="checkbox" class="filter-floor-cb" value="${f}" id="cb_floor_${i}">
                <span>Tầng ${f}</span>
            </label>`).join(''));

        const statuses = [[1, 'Trống'], [2, 'Đang ở (Check-in)'], [3, 'Chờ dọn dẹp (Check-out)']];
        setup(document.getElementById('filterStatusDropdown'),
            statuses.map(([v, t], i) => `<label for="cb_status_${i}">
                <input type="checkbox" class="filter-status-cb" value="${v}" id="cb_status_${i}">
                <span>${t}</span>
            </label>`).join(''));

        const types = [...new Set(this.rooms.map(r => r.maLoai).filter(Boolean))].sort();
        setup(document.getElementById('filterRoomTypeDropdown'),
            types.map((t, i) => `<label for="cb_type_${i}">
                <input type="checkbox" class="filter-type-cb" value="${esc(t)}" id="cb_type_${i}">
                <span>${esc(t)}</span>
            </label>`).join(''));

        const caps = [...new Set(this.rooms.map(r => r.sucChua))].sort((a, b) => a - b);
        setup(document.getElementById('filterCapacityDropdown'),
            caps.map((c, i) => `<label for="cb_cap_${i}">
                <input type="checkbox" class="filter-capacity-cb" value="${c}" id="cb_cap_${i}">
                <span>Tối đa ${c} người</span>
            </label>`).join(''));

        document.querySelectorAll(
            '.filter-floor-cb, .filter-status-cb, .filter-type-cb, .filter-capacity-cb'
        ).forEach(cb => cb.addEventListener('change', () => this.loc()));
    },

    loc() {
        const pick = sel => [...document.querySelectorAll(sel + ':checked')].map(c => c.value);
        const floors = pick('.filter-floor-cb').map(Number);
        const statuses = pick('.filter-status-cb').map(Number);
        const types = pick('.filter-type-cb');
        const caps = pick('.filter-capacity-cb').map(Number);

        this.ve(this.rooms.filter(r => {
            const st = this._status(r).code;
            return (floors.length === 0 || floors.includes(r._floor))
                && (statuses.length === 0 || statuses.includes(st))
                && (types.length === 0 || types.includes(r.maLoai))
                && (caps.length === 0 || caps.includes(r.sucChua));
        }));
    },

    ve(rooms) {
        if (this.spinner) this.spinner.style.display = 'none';

        if (!rooms?.length) {
            this.map.innerHTML = `<div class="adm-empty">
                <i class="bi bi-door-closed"></i>
                <h4>Không có phòng phù hợp</h4>
                <p>Thử bỏ bớt bộ lọc hoặc chọn tiêu chí khác.</p>
            </div>`;
            return;
        }

        const byFloor = rooms.reduce((acc, r) => {
            (acc[r._floor] = acc[r._floor] || []).push(r);
            return acc;
        }, {});

        let html = '';
        Object.keys(byFloor).sort((a, b) => a - b).forEach(floor => {
            html += `<div class="adm-floor-section">
                <div class="adm-floor-head">
                    <h4>Tầng ${floor}</h4>
                    <span>${byFloor[floor].length} phòng</span>
                </div>
                <div class="adm-room-grid">`;

            byFloor[floor].forEach(r => {
                const st = this._status(r);
                let actions = '';

                if (st.code === 1) {
                    actions = `<a href="/receptionist/check-in?roomId=${r.id}" class="adm-btn adm-btn-gold">
                        <i class="bi bi-box-arrow-in-right"></i> Check-in</a>`;
                } else if (st.code === 2) {
                    actions = `
                        <a href="/receptionist/check-out?roomId=${r.id}" class="adm-btn adm-btn-danger">
                            <i class="bi bi-box-arrow-left"></i> Out</a>
                        <button class="adm-btn adm-btn-outline btn-transfer"
                            data-id="${r.id}" data-type="${esc(r.maLoai)}" data-price="${r.gia}"
                            title="Đổi phòng">
                            <i class="bi bi-arrow-left-right"></i>
                        </button>`;
                } else {
                    actions = `<button class="adm-btn adm-btn-outline" disabled>
                        <i class="bi bi-hourglass-split"></i> Chờ dọn</button>`;
                }

                const vIcon = r.view === 'Hướng Biển' ? 'bi-water'
                    : r.view === 'Hướng Núi' ? 'bi-tree'
                        : 'bi-buildings';

                html += `<div class="adm-room-card ${st.cls}">
                    <div class="adm-room-card-head">
                        <h5>P.${esc(r.name)}</h5>
                        <span class="adm-badge adm-badge-cat">${esc(r.maLoai)}</span>
                    </div>
                    <div class="adm-room-card-meta">
                        <span><i class="bi bi-people-fill"></i> ${r.sucChua} khách</span>
                        <span><i class="bi ${vIcon}"></i> ${esc(r.view)}</span>
                    </div>
                    <div class="adm-room-card-actions">${actions}</div>
                </div>`;
            });

            html += `</div></div>`;
        });

        this.map.innerHTML = html;
        this.map.querySelectorAll('.btn-transfer').forEach(btn => {
            btn.addEventListener('click', e => this.moDoi(e.currentTarget));
        });
    },

    initTransferModal() {
        const sel = document.getElementById('new-room-select');
        const reason = document.getElementById('transfer-reason');
        const otherBox = document.getElementById('other-reason-container');
        const newDisp = document.getElementById('new-price-disp');
        const diffDisp = document.getElementById('price-diff-disp');

        sel?.addEventListener('change', e => {
            const p = parseInt(e.target.value, 10) || 0;
            newDisp.textContent = p ? money(p) : '0 đ';
            if (!p) {
                diffDisp.textContent = '0 đ';
                diffDisp.className = 'fw-bold';
                return;
            }
            const d = p - this.transferPrice;
            diffDisp.textContent = money(Math.abs(d)) + (d > 0 ? ' (Thu thêm)' : d < 0 ? ' (Hoàn lại)' : '');
            diffDisp.className = 'fw-bold ' + (d > 0 ? 'text-danger' : d < 0 ? 'text-success' : 'text-navy');
        });

        reason?.addEventListener('change', e => {
            if (otherBox) otherBox.style.display = e.target.value === 'Lý do khác' ? 'block' : 'none';
        });

        document.getElementById('btn-confirm-transfer')?.addEventListener('click', () => {
            if (!sel.value) return alert('Vui lòng chọn phòng mới');
            AdmModal.close('transferRoomModal');
            toast('Đã chuyển phòng, hệ thống đã lưu giao dịch.');
        });
    },

    moDoi(btn) {
        const id = btn.dataset.id;
        this.transferPrice = parseInt(btn.dataset.price, 10) || 0;

        document.getElementById('current-room-id').textContent = id;
        document.getElementById('current-room-type').textContent = btn.dataset.type;
        document.getElementById('current-price-disp').textContent = money(this.transferPrice);
        document.getElementById('new-price-disp').textContent = '0 đ';

        const diff = document.getElementById('price-diff-disp');
        diff.textContent = '0 đ';
        diff.className = 'fw-bold';

        const sel = document.getElementById('new-room-select');
        sel.innerHTML = '<option value="">-- Danh sách phòng trống --</option>';
        this.rooms.filter(r => r.status === 'Trống').forEach(r => {
            const opt = document.createElement('option');
            opt.value = r.gia;
            opt.textContent = `Phòng ${r.name} (${r.maLoai}) - ${money(r.gia)}`;
            sel.appendChild(opt);
        });

        document.getElementById('transfer-reason').value = 'Khách yêu cầu nâng hạng';
        document.getElementById('other-reason-container').style.display = 'none';

        AdmModal.open('transferRoomModal');
    },
};

// ============================================================
// CHECK-IN
// ============================================================
const CheckIn = {
    verifyId: null,
    assignId: null,
    currentId: null,
    polling: null,
    stream: null,
    emptyRooms: [],

    async init() {
        const tbody = document.getElementById('checkin-table-body');
        if (tbody) {
            try { this.emptyRooms = (await api('/rooms/empty')) || []; }
            catch { this.emptyRooms = []; }
            await this.tai();
        }

        document.getElementById('assignRoomCategory')?.addEventListener('change', () => this.vePhongTrong());
        document.getElementById('assignRoomView')?.addEventListener('change', () => this.vePhongTrong());

        const btnConfirm = document.getElementById('btnConfirmAssignRoom');
        if (btnConfirm) btnConfirm.style.display = 'none';

        document.getElementById('front-cccd')?.addEventListener('change', function () {
            CheckIn.preview(this, 'front-preview', 'front-placeholder');
        });
        document.getElementById('back-cccd')?.addEventListener('change', function () {
            CheckIn.preview(this, 'back-preview', 'back-placeholder');
        });

        document.getElementById('btn-start-camera')?.addEventListener('click', () => this.toggleCam());
        document.getElementById('btn-capture-scan')?.addEventListener('click', () => this.chupAnh());
        document.getElementById('btn-extract-upload')?.addEventListener('click', e => this.trichXuat(e.currentTarget));
        document.getElementById('btn-complete-checkin')?.addEventListener('click', e => this.hoanTat(e.currentTarget));
        document.getElementById('verifyIdModal')?.addEventListener('adm:modal-closed', () => this.tatCam());
    },

    async tai() {
        const tbody = document.getElementById('checkin-table-body');
        if (!tbody) return;

        try {
            const list = (await api('/checkin')) || [];

            if (!list.length) {
                tbody.innerHTML = `<tr><td colspan="7">
                    <div class="adm-empty">
                        <i class="bi bi-inbox"></i>
                        <h4>Chưa có phiếu chờ check-in</h4>
                        <p>Đơn đặt phòng sẽ hiện ở đây khi khách đến nhận phòng.</p>
                    </div></td></tr>`;
                return;
            }

            tbody.innerHTML = list.map(it => {
                const id = docId(it);
                const isGuest = !it.customerUserId;
                const price = money(it.totalAmount);
                const roomName = it.roomName || '';
                const verified = it.isVerified === true;

                const roomBadge = roomName
                    ? `<span class="adm-badge adm-badge-success"><i class="bi bi-door-open-fill"></i> Phòng ${esc(roomName)}</span>`
                    : `<span class="adm-badge adm-badge-warning"><i class="bi bi-exclamation-triangle-fill"></i> Chưa xếp phòng</span>`;

                let roomUI = '';
                if (!verified) {
                    roomUI = `
                        <div class="adm-alert adm-alert-warning" style="padding:8px 10px;font-size:11.5px;font-weight:600;margin:0;">
                            <i class="bi bi-shield-lock-fill"></i>
                            <span>Cần xác thực danh tính</span>
                        </div>
                        <button type="button" class="adm-btn adm-btn-outline w-100"
                            onclick="CheckIn.moXacThucMobile(${id}, '${esc(it.customerName || '')}')">
                            <i class="bi bi-phone-vibrate"></i> Xác thực (Mobile)
                        </button>
                        <input type="hidden" name="maPhong" id="room-${id}" value="" required>`;
                } else {
                    const roomLabel = roomName
                        ? `<i class="bi bi-door-open-fill"></i> Phòng ${esc(roomName)} (Đổi)`
                        : `<i class="bi bi-key-fill"></i> Chọn phòng...`;
                    const btnCls = roomName ? 'adm-btn-outline' : 'adm-btn-danger';

                    roomUI = `
                        <div style="font-size:11.5px;color:var(--green);font-weight:700;text-align:center;">
                            <i class="bi bi-check-circle-fill"></i> Đã xác thực FaceID
                        </div>
                        <button type="button" class="adm-btn ${btnCls} w-100" style="justify-content:flex-start;"
                            onclick="CheckIn.moGanPhong(${id}, ${roomName ? `'${esc(roomName)}'` : 'null'})"
                            id="btn-select-room-${id}">${roomLabel}</button>
                        <input type="hidden" name="maPhong" id="room-${id}" value="${esc(roomName)}" required>`;
                }

                return `<tr>
                    <td class="cell-id">#${esc(id)}</td>
                    <td>
                        <div class="fw-bold text-navy">${esc(it.customerName || '—')}</div>
                        ${isGuest ? '<span class="adm-badge adm-badge-neutral" style="font-size:10.5px;padding:3px 8px;margin-top:4px;display:inline-block;">Khách vãng lai</span>' : ''}
                    </td>
                    <td class="text-muted">${esc(it.customerPhone || '')}</td>
                    <td class="text-center">${roomBadge}</td>
                    <td style="font-size:12.5px;">
                        <div class="text-muted">Nhận: <strong class="text-navy">${dateVN(it.expected_in)}</strong></div>
                        <div class="text-muted">Trả: <strong class="text-navy">${dateVN(it.expected_out)}</strong></div>
                    </td>
                    <td class="cell-price text-right">${price}</td>
                    <td class="text-center">
                        <form onsubmit="CheckIn.moCccd(event, ${id})" class="adm-checkin-actions">
                            ${roomUI}
                            <label class="adm-switch">
                                <input type="checkbox" name="isPaidUpfront" value="true"
                                    id="checkPaid-${id}" ${isGuest ? 'checked' : ''}>
                                <span>Thu trước 1 đêm (TM)</span>
                            </label>
                            <button type="submit" class="adm-btn adm-btn-gold w-100" ${!verified ? 'disabled' : ''}>
                                <i class="bi bi-check2-circle"></i> Duyệt nhận phòng
                            </button>
                        </form>
                    </td>
                </tr>`;
            }).join('');
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="7">
                <div class="adm-empty">
                    <i class="bi bi-exclamation-triangle" style="color:var(--red);"></i>
                    <h4>Không tải được danh sách check-in</h4>
                    <p>Vui lòng kiểm tra kết nối và thử lại.</p>
                </div></td></tr>`;
        }
    },

    moXacThucMobile(id, hoTen) {
        this.verifyId = id;

        document.getElementById('mobile-verify-waiting').classList.remove('d-none');
        document.getElementById('mobile-verify-success').classList.add('d-none');
        document.getElementById('mobile-verify-code').textContent =
            'REQ-' + id + '-' + Math.floor(Math.random() * 1000);
        document.getElementById('sync-name').textContent = hoTen;

        document.querySelector('#mobileVerifyModal .btn-outline-secondary')?.classList.remove('d-none');
        document.querySelector('#mobileVerifyModal .btn-warning')?.classList.remove('d-none');
        document.getElementById('btn-continue-room')?.classList.add('d-none');

        const modalEl = document.getElementById('mobileVerifyModal');
        AdmModal.open(modalEl);

        clearInterval(this.polling);
        this.polling = setInterval(async () => {
            try {
                const r = await fetch(`${CFG.base}/v1/reception/bookings/${id}/verification-status`);
                if (r.ok) {
                    const d = await r.json();
                    if (d.isFaceVerified === true) {
                        clearInterval(this.polling);
                        this.moXacThucThanhCong();
                    }
                }
            } catch (e) { /* server chưa bật */ }
        }, CFG.poll);

        modalEl.addEventListener('adm:modal-closed', function onHide() {
            clearInterval(CheckIn.polling);
            modalEl.removeEventListener('adm:modal-closed', onHide);
        });
    },

    moXacThucThanhCong() {
        clearInterval(this.polling);
        document.querySelector('#mobileVerifyModal .btn-outline-secondary')?.classList.add('d-none');
        document.querySelector('#mobileVerifyModal .btn-warning')?.classList.add('d-none');
        document.getElementById('mobile-verify-waiting').classList.add('d-none');
        document.getElementById('mobile-verify-success').classList.remove('d-none');
        document.getElementById('btn-continue-room').classList.remove('d-none');
    },

    xacThucXong() {
        AdmModal.close('mobileVerifyModal');
        this.tai();
        setTimeout(() => this.moGanPhong(this.verifyId, null), 400);
    },

    moGanPhong(checkinId, currentRoomId) {
        this.assignId = checkinId;
        document.getElementById('assignRoomCategory').value = '';
        document.getElementById('assignRoomView').value = '';
        this.vePhongTrong();
        AdmModal.open('assignRoomModal');
    },

    vePhongTrong() {
        const box = document.getElementById('availableRoomsList');
        if (!box) return;

        const cat = document.getElementById('assignRoomCategory').value;
        const view = document.getElementById('assignRoomView').value;

        const list = this.emptyRooms.filter(r =>
            (cat === '' || r.type === cat) &&
            (view === '' || r.view === view)
        );

        if (!list.length) {
            box.innerHTML = '<div class="adm-empty" style="padding:32px 16px;"><i class="bi bi-door-closed" style="font-size:28px;"></i><p>Không có phòng trống phù hợp.</p></div>';
            return;
        }

        box.innerHTML = list.map(r => {
            const vIcon = r.view === 'Hướng Biển' ? 'bi-water'
                : r.view === 'Hướng Núi' ? 'bi-tree'
                    : 'bi-buildings';
            return `<button type="button" class="adm-room-pick"
                onclick="CheckIn.chonPhong('${r.id}', '${esc(r.view)}', '${esc(r.type)}', '${esc(r.name || r.id)}')">
                <div>
                    <h6 class="adm-room-pick-name"><i class="bi bi-door-open-fill"></i> Phòng ${esc(r.name || r.id)}</h6>
                    <span class="adm-room-pick-type">${esc(r.type)}</span>
                </div>
                <div class="adm-room-pick-view"><i class="bi ${vIcon}"></i> ${esc(r.view)}</div>
            </button>`;
        }).join('');
    },

    chonPhong(roomId, view, type, roomName) {
        if (!this.assignId) return;
        document.getElementById('room-' + this.assignId).value = roomName || roomId;

        const btn = document.getElementById('btn-select-room-' + this.assignId);
        if (btn) {
            btn.innerHTML = `<i class="bi bi-door-open-fill"></i> P.${esc(roomName || roomId)} (${esc(view)})`;
            btn.classList.remove('adm-btn-danger');
            btn.classList.add('adm-btn-outline');
        }
        AdmModal.close('assignRoomModal');
    },

    moCccd(event, id) {
        event.preventDefault();
        const form = event.target;
        const maPhong = form.querySelector('input[name="maPhong"]').value;
        const paid = form.querySelector('input[name="isPaidUpfront"]').checked;

        if (!maPhong) return toast('Vui lòng chọn phòng trước khi check-in!', 'warning');

        this.currentId = id;
        document.getElementById('checkin-id').value = id;
        document.getElementById('checkin-room').value = maPhong;
        document.getElementById('checkin-paid').value = paid;

        ['raw-data-result', 'front-cccd', 'back-cccd', 'manual-name', 'manual-id', 'manual-nationality', 'manual-dob']
            .forEach(el => { const e = document.getElementById(el); if (e) e.value = ''; });

        document.getElementById('front-preview').style.display = 'none';
        document.getElementById('front-placeholder').style.display = 'block';
        document.getElementById('back-preview').style.display = 'none';
        document.getElementById('back-placeholder').style.display = 'block';

        this.tatCam();
        AdmModal.open('verifyIdModal');
    },

    preview(input, previewId, placeholderId) {
        if (!input.files?.[0]) return;
        const reader = new FileReader();
        reader.onload = e => {
            const img = document.getElementById(previewId);
            img.src = e.target.result;
            img.style.display = 'block';
            document.getElementById(placeholderId).style.display = 'none';
        };
        reader.readAsDataURL(input.files[0]);
    },

    async trichXuat(btn) {
        const front = document.getElementById('front-cccd');
        if (!front?.files.length) return alert('Vui lòng tải lên ảnh mặt trước CCCD');
        if (!CFG.ai) return alert('Chưa cấu hình AI_URL — không thể trích xuất');

        busy(btn, true, 'Đang kết nối AI...');
        try {
            const fd = new FormData();
            fd.append('cccd_image', front.files[0]);

            const r = await fetch(`${CFG.ai}/api/v1/ai/verify-cccd`, { method: 'POST', body: fd });
            if (!r.ok) throw new Error('AI server lỗi ' + r.status);

            const d = await r.json();
            if (d.success && d.data) {
                document.getElementById('raw-data-result').value = JSON.stringify({
                    'Số CCCD': d.data.id || 'Không rõ',
                    'Họ và tên': d.data.name || 'Không rõ',
                    'Ngày sinh': d.data.dob || 'Không rõ',
                    'Giới tính': d.data.gender || 'Không rõ',
                    'Ngày hết hạn': d.data.date_of_expiry || 'Không rõ',
                }, null, 2);
            } else {
                alert('AI không đọc được. Vui lòng dùng ảnh rõ hơn.');
            }
        } catch (e) {
            alert('Không kết nối được AI Server. Kiểm tra lại AI_URL / ngrok.');
        } finally {
            busy(btn, false);
        }
    },

    async toggleCam() {
        if (this.stream) return this.tatCam();

        const video = document.getElementById('webcam-video');
        const btn = document.getElementById('btn-start-camera');
        const ph = document.getElementById('webcam-placeholder');
        const snap = document.getElementById('btn-capture-scan');

        try {
            this.stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
            video.srcObject = this.stream;
            video.style.display = 'block';
            ph.style.display = 'none';
            btn.innerHTML = '<i class="bi bi-camera-video-off"></i> Tắt Camera';
            btn.classList.remove('adm-btn-primary');
            btn.classList.add('adm-btn-danger');
            if (snap) snap.disabled = false;
        } catch (e) {
            alert('Không truy cập được Camera.');
        }
    },

    tatCam() {
        if (this.stream) {
            this.stream.getTracks().forEach(t => t.stop());
            this.stream = null;
        }
        const video = document.getElementById('webcam-video');
        const ph = document.getElementById('webcam-placeholder');
        const btn = document.getElementById('btn-start-camera');
        const snap = document.getElementById('btn-capture-scan');

        if (video) video.style.display = 'none';
        if (ph) ph.style.display = 'block';
        if (btn) {
            btn.innerHTML = '<i class="bi bi-camera"></i> Bật Camera';
            btn.classList.remove('adm-btn-danger');
            btn.classList.add('adm-btn-primary');
        }
        if (snap) snap.disabled = true;
    },

    chupAnh() {
        const video = document.getElementById('webcam-video');
        const canvas = document.getElementById('webcam-canvas');
        if (!video || !canvas || !this.stream) return;

        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);

        video.style.display = 'none';
        canvas.style.display = 'block';

        alert('Đã lưu ảnh khuôn mặt — sẽ gửi kèm khi hoàn tất check-in.');
        this.tatCam();
        document.getElementById('btn-start-camera').innerHTML =
            '<i class="bi bi-camera-video"></i> Chụp lại';
    },

    async hoanTat(btn) {
        const activeTab = document.querySelector('#verifyTabs .nav-link.active')?.id;
        let data = document.getElementById('raw-data-result').value;

        if (activeTab === 'manual-tab') {
            const name = document.getElementById('manual-name').value;
            const idNum = document.getElementById('manual-id').value;
            if (!name || !idNum) return alert('Nhập đủ Họ tên và Số giấy tờ');
            data = JSON.stringify({
                'Họ Tên': name,
                'Giấy tờ': idNum,
                'Quốc tịch': document.getElementById('manual-nationality').value,
                'Ngày sinh': document.getElementById('manual-dob').value,
            }, null, 2);
        } else if (!data) {
            return alert('Vui lòng quét CCCD để lấy dữ liệu trước');
        }

        if (!confirm('Xác nhận bàn giao phòng, lập biên bản cọc và check-in?')) return;

        const id = document.getElementById('checkin-id').value;
        const maPhong = document.getElementById('checkin-room').value;
        const paid = document.getElementById('checkin-paid').value === 'true';

        busy(btn, true);
        try {
            const qs = new URLSearchParams({ maPhong, isPaidUpfront: String(paid) }).toString();
            const r = await fetch(`/receptionist/checkin/execute/${id}?${qs}`);
            if (!r.ok) throw new Error('Thao tác thất bại');

            toast('Duyệt nhận phòng thành công!');
            AdmModal.close('verifyIdModal');
            this.tai();
        } catch (e) {
            toast('Có lỗi khi check-in', 'danger');
        } finally {
            busy(btn, false);
        }
    },
};

// ============================================================
// TRA CỨU
// ============================================================
const TraCuu = {
    init() {
        document.getElementById('bookingCodeInput')?.closest('form')
            ?.addEventListener('submit', e => this.tim(e));
    },

    async tim(event) {
        event.preventDefault();
        const code = document.getElementById('bookingCodeInput').value.trim();
        if (!code) return;

        try {
            const data = await api('/bookings/search?code=' + encodeURIComponent(code));
            if (!data) return toast(`Không tìm thấy mã "${code}"`, 'danger');
            this.hien(data);
        } catch (e) {
            toast('Không tra cứu được, thử lại sau', 'danger');
        }
    },

    hien(data) {
        document.getElementById('res-hoTen').textContent = data.hoTen;
        document.getElementById('res-ngayCheckIn').textContent = dateTimeVN(data.ngayCheckIn);
        document.getElementById('res-ngayCheckOut').textContent = dateTimeVN(data.ngayCheckOut);

        const badge = document.getElementById('res-trangThai');
        badge.className = 'adm-badge';

        const map = {
            PAID: ['Đã thanh toán', 'adm-badge-success'],
            UNPAID: ['Chưa thanh toán', 'adm-badge-warning'],
            CANCELLED: ['Đã hủy', 'adm-badge-danger'],
            CHECKED_OUT: ['Đã trả phòng', 'adm-badge-neutral'],
        };
        const [txt, cls] = map[data.trangThai] || ['Không xác định', 'adm-badge-neutral'];
        badge.textContent = txt;
        badge.classList.add(...cls.split(' '));

        AdmModal.open('searchResultModal');
    },
};

// ============================================================
// KHỞI CHẠY
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
    HoaDon.init();
    SoDoPhong.init();
    CheckIn.init();
    TraCuu.init();
});
