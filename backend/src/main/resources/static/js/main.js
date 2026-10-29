/* May Hotel - frontend script
   Viết lại 12/2025, gộp từ nhiều file rời
   Backend: Node + MongoDB (REST /api/*)
*/

'use strict';

// Đổi URL khi deploy
const API_BASE = window.API_BASE_URL || '/api';
const CHAT_API = window.CHAT_API_URL || '/api/v1/ai/chat';
const USER_ID = localStorage.getItem('userId') || 'guest';
const REVIEW_DELAY = 5000;


// ============================================================
// Helper
// ============================================================

async function api(path, options = {}) {
    const url = path.startsWith('http') ? path : (path.startsWith('/api') ? path : API_BASE + (path.startsWith('/') ? path : '/' + path));
    const res = await fetch(url, {
        ...options,
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const text = await res.text();
    return text ? JSON.parse(text) : null;
}

function formatDate(input) {
    if (!input) return '';
    const d = new Date(input);
    return isNaN(d) ? input : d.toLocaleDateString('vi-VN');
}

// Chống XSS khi chèn dữ liệu từ API
function esc(str) {
    if (str == null) return '';
    const d = document.createElement('div');
    d.textContent = String(str);
    return d.innerHTML;
}

function toast(msg, type = 'success') {
    const el = document.getElementById('appToast');
    if (!el) return console.log('[toast]', msg);

    el.querySelector('.toast-body').textContent = msg;
    el.classList.remove('text-bg-success', 'text-bg-danger', 'text-bg-warning', 'text-bg-info');
    el.classList.add('text-bg-' + type);
    new bootstrap.Toast(el).show();
}

// Bật spinner cho button trong lúc chờ API
function busy(btn, on, label = 'Đang xử lý...') {
    if (!btn) return;
    if (on) {
        btn.dataset.html = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>' + label;
    } else {
        btn.disabled = false;
        if (btn.dataset.html) btn.innerHTML = btn.dataset.html;
    }
}


// ============================================================
// Chọn phòng (thay cho cart cũ)
// ============================================================

const ChonPhong = {

    init() {
        document.querySelectorAll('.btn-add-cart').forEach(btn => {
            btn.addEventListener('click', e => this.them(e.currentTarget));
        });
    },

    async them(btn) {
        const roomId = btn.dataset.id;
        if (!roomId) return;

        busy(btn, true);
        try {
            const res = await api('/cart/add', {
                method: 'POST',
                body: JSON.stringify({ roomId, userId: USER_ID }),
            });

            if (res && res.success) {
                this.capNhatSoLuong(res.totalItems);
                toast('Đã thêm phòng vào danh sách chọn');
            } else {
                toast(res?.message || 'Không thêm được phòng', 'warning');
            }
        } catch (err) {
            toast('Lỗi kết nối, thử lại sau', 'danger');
        } finally {
            busy(btn, false);
        }
    },

    capNhatSoLuong(n) {
        document.querySelectorAll('#cart-quantity, .cart-quantity').forEach(el => {
            el.textContent = n;
        });
    },
};


// ============================================================
// Đánh giá khách hàng
// ============================================================

const DanhGia = {
    list: [],
    i: 0,
    timer: null,

    init() {
        this.box = document.getElementById('testimonialBox');
        this.avatar = document.getElementById('avatar');
        this.nameEl = document.getElementById('name');
        this.textEl = document.getElementById('text');
        this.stars = document.getElementById('stars');
        this.form = document.getElementById('review-form');
        this.alert = document.getElementById('alert-message');

        if (!this.box) return;

        document.getElementById('btn-prev')?.addEventListener('click', () => {
            this.lui(); this.auto();
        });
        document.getElementById('btn-next')?.addEventListener('click', () => {
            this.toi(); this.auto();
        });
        this.form?.addEventListener('submit', e => this.gui(e));

        this.tai();
    },

    async tai() {
        try {
            const data = await api('/reviews?public=true');

            // Backend trả field khác thì sửa ở đây
            this.list = (data || []).map(r => ({
                name: r.customerName || 'Khách',
                stars: r.rating || 5,
                text: r.content || '',
                avatar: r.avatar || null,
            }));

            this.ve();
            this.auto();
        } catch (err) {
            this.box.innerHTML = '<p class="text-muted py-4 m-0">' +
                '<i class="bi bi-chat-dots me-2"></i>Chưa có đánh giá nào.</p>';
        }
    },

    ve() {
        if (!this.list.length) {
            this.box.innerHTML = '<p class="text-muted py-4 m-0">' +
                '<i class="bi bi-chat-dots me-2"></i>Chưa có phản hồi công khai.</p>';
            return;
        }

        const r = this.list[this.i];
        this.nameEl.textContent = r.name;
        this.textEl.textContent = r.text;
        this.avatar.src = r.avatar ||
            'https://ui-avatars.com/api/?name=' + encodeURIComponent(r.name) + '&background=0F2942&color=fff';

        let html = '';
        for (let k = 0; k < 5; k++) {
            html += k < r.stars
                ? '<i class="bi bi-star-fill me-1 text-warning"></i>'
                : '<i class="bi bi-star text-muted opacity-25 me-1"></i>';
        }
        this.stars.innerHTML = html;
    },

    toi() {
        if (this.list.length < 2) return;
        this.i = (this.i + 1) % this.list.length;
        this.ve();
    },

    lui() {
        if (this.list.length < 2) return;
        this.i = (this.i - 1 + this.list.length) % this.list.length;
        this.ve();
    },

    auto() {
        clearInterval(this.timer);
        this.timer = setInterval(() => this.toi(), REVIEW_DELAY);
    },

    async gui(e) {
        e.preventDefault();
        const f = e.currentTarget;

        const data = {
            customerName: f.querySelector('#reviewer-name')?.value.trim(),
            rating: parseInt(f.querySelector('#review-stars')?.value || '5', 10),
            content: f.querySelector('#review-content')?.value.trim(),
            userId: USER_ID,
        };

        if (!data.customerName || !data.content) {
            return this.thongBao('Nhập đủ họ tên và nội dung đánh giá', 'warning');
        }

        const btn = f.querySelector('button[type="submit"]');
        busy(btn, true, 'Đang gửi...');

        try {
            await api('/reviews', { method: 'POST', body: JSON.stringify(data) });
            f.reset();
            this.thongBao('Cảm ơn bạn, đánh giá đã gửi thành công', 'success');
            await this.tai();
        } catch (err) {
            this.thongBao('Không gửi được đánh giá, thử lại sau', 'danger');
        } finally {
            busy(btn, false);
        }
    },

    thongBao(msg, type) {
        if (!this.alert) return;
        this.alert.textContent = msg;
        this.alert.className = 'alert alert-' + type + ' border-0 shadow-sm text-center mb-4';
        this.alert.classList.remove('d-none');
        setTimeout(() => this.alert.classList.add('d-none'), 5000);
    },
};


// ============================================================
// Form tìm phòng - validate ngày
// ============================================================

const TimPhong = {

    init() {
        const ci = document.getElementById('sbCheckinDate');
        const co = document.getElementById('sbCheckoutDate');
        if (!ci || !co) return;

        // Điền sẵn từ query string
        const p = new URLSearchParams(location.search);
        ['sbCheckinDate', 'sbCheckoutDate', 'sbAdults', 'sbChildren'].forEach(id => {
            const v = p.get(id.replace('sb', '').toLowerCase());
            const el = document.getElementById(id);
            if (el && v) el.value = v;
        });

        const today = new Date().toISOString().slice(0, 10);
        ci.min = today;

        const capNhat = () => {
            if (!ci.value) return;
            const d = new Date(ci.value);
            d.setDate(d.getDate() + 1);
            const next = d.toISOString().slice(0, 10);
            co.min = next;
            if (co.value && co.value <= ci.value) co.value = next;
        };

        capNhat();
        ci.addEventListener('change', capNhat);
    },
};


// ============================================================
// Bảng đơn đặt phòng (trang admin / lễ tân)
// ============================================================

const BangDatPhong = {
    data: [],

    init() {
        this.tbody = document.getElementById('table-body');
        if (!this.tbody) return;

        this.search = document.getElementById('booking-search');
        this.filter = document.getElementById('booking-status-filter');

        this.search?.addEventListener('input', () => this.ve());
        this.filter?.addEventListener('change', () => this.ve());

        this.tai();
    },

    async tai() {
        this.tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4">' +
            '<div class="spinner-border text-warning"></div>' +
            '<p class="text-muted mt-2 mb-0">Đang tải...</p></td></tr>';

        try {
            this.data = (await api('/bookings')) || [];
            this.ve();
        } catch (err) {
            this.tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">' +
                '<i class="bi bi-exclamation-triangle me-2"></i>Không tải được danh sách</td></tr>';
        }
    },

    loc() {
        const kw = (this.search?.value || '').toLowerCase().trim();
        const st = this.filter?.value || '';

        return this.data.filter(b => {
            const ok1 = !kw
                || (b.bookingCode || '').toLowerCase().includes(kw)
                || (b.customerName || '').toLowerCase().includes(kw)
                || (b.roomName || '').toLowerCase().includes(kw);
            const ok2 = !st || b.status === st;
            return ok1 && ok2;
        });
    },

    ve() {
        const rows = this.loc();

        if (!rows.length) {
            this.tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">' +
                'Không có đơn nào.</td></tr>';
            return;
        }

        this.tbody.innerHTML = rows.map(b => `
      <tr>
        <td class="text-center fw-bold">${esc(b.bookingCode || b.id)}</td>
        <td class="text-center">${esc(b.customerName)}</td>
        <td class="text-center fw-bold text-navy">${esc(b.roomName)}</td>
        <td class="text-center">${formatDate(b.checkIn)}</td>
        <td class="text-center">${formatDate(b.checkOut)}</td>
        <td class="text-center">${this.nhanTrangThai(b.status)}</td>
        <td class="text-center">${this.nhanRuiRo(b)}</td>
        <td class="text-center">
          <a href="/admin/bookings/${b.id}" class="btn btn-sm btn-info text-white" title="Chi tiết">
            <i class="bi bi-eye"></i></a>
          <a href="/admin/bookings/${b.id}/edit" class="btn btn-sm btn-warning text-white mx-1" title="Sửa">
            <i class="bi bi-pencil"></i></a>
        </td>
      </tr>
    `).join('');
    },

    nhanTrangThai(s) {
        const map = {
            pending: ['Chờ nhận phòng', 'bg-warning text-dark'],
            checked_in: ['Đã nhận phòng', 'bg-success'],
            checked_out: ['Đã trả phòng', 'bg-secondary'],
            cancelled: ['Đã hủy', 'bg-danger'],
        };
        const [txt, cls] = map[s] || [s || '—', 'bg-secondary'];
        return `<span class="badge ${cls}">${txt}</span>`;
    },

    nhanRuiRo(b) {
        const p = b.noShowProbability;
        if (p == null) return '<span class="text-muted">—</span>';

        const cls = p >= 70 ? 'bg-danger' : p >= 40 ? 'bg-warning text-dark' : 'bg-success';
        const txt = p >= 70 ? 'Cao' : p >= 40 ? 'Vừa' : 'Thấp';
        return `<span class="badge ${cls} fs-6">${txt} (${p}%)</span>`;
    },
};


// ============================================================
// Modal đặt phòng trực tiếp (lễ tân)
// ============================================================

const DatTrucTiep = {

    init() {
        const modal = document.getElementById('directBookingModal');
        if (!modal) return;

        // Đẩy modal ra body cho khỏi bị backdrop che
        document.body.appendChild(modal);

        this.els = {
            searchBtn: document.getElementById('btn-search-rooms'),
            results: document.getElementById('db-search-results'),
            checkBtn: document.getElementById('btn-check-customer'),
            nfcBtn: document.getElementById('btn-nfc-scan'),
            cccd: document.getElementById('db-cccd'),
            fullname: document.getElementById('db-fullname'),
            phone: document.getElementById('db-phone'),
            roomSel: document.getElementById('db-selected-room'),
        };

        this.els.searchBtn?.addEventListener('click', () => this.timPhong());
        this.els.checkBtn?.addEventListener('click', () => this.traKhach());
        this.els.nfcBtn?.addEventListener('click', () => this.quetNfc());
    },

    async timPhong() {
        const ci = document.getElementById('db-checkin')?.value;
        const co = document.getElementById('db-checkout')?.value;
        if (!ci || !co) return alert('Chọn ngày nhận và trả phòng trước');

        try {
            const rooms = await api('/rooms/available?checkIn=' + ci + '&checkOut=' + co);

            this.els.roomSel.innerHTML = rooms?.length
                ? rooms.map(r => `<option value="${r.id}">Phòng ${esc(r.name)} - ${r.price?.toLocaleString('vi-VN')} đ</option>`).join('')
                : '<option value="">Không có phòng trống</option>';

            this.els.results.classList.remove('d-none');
        } catch (err) {
            toast('Không tìm được phòng trống', 'danger');
        }
    },

    async traKhach() {
        const cccd = this.els.cccd.value.trim();
        if (!cccd) return alert('Nhập CCCD trước');

        try {
            const c = await api('/customers/lookup?cccd=' + encodeURIComponent(cccd));

            if (c) {
                this.els.fullname.value = c.fullName || '';
                this.els.phone.value = c.phone || '';
                toast('Tìm thấy khách cũ');
            } else {
                this.els.fullname.value = '';
                this.els.phone.value = '';
                toast('Khách mới, nhập thông tin', 'warning');
            }
        } catch (err) {
            toast('Lỗi tra cứu CCCD', 'danger');
        }
    },

    async quetNfc() {
        if (!('NDEFReader' in window)) {
            return alert('Trình duyệt không hỗ trợ NFC');
        }

        try {
            const ndef = new NDEFReader();
            await ndef.scan();
            toast('Đưa thẻ CCCD lại gần máy', 'info');

            ndef.onreading = ev => {
                const dec = new TextDecoder();
                for (const rec of ev.message.records) {
                    if (rec.recordType === 'text') {
                        const txt = dec.decode(rec.data);
                        const [id, name] = txt.split('|');
                        if (id) this.els.cccd.value = id.trim();
                        if (name) this.els.fullname.value = name.trim();
                        break;
                    }
                }
                toast('Đọc thẻ xong');
            };
        } catch (err) {
            toast('Không kết nối được NFC', 'danger');
        }
    },
};


// ============================================================
// Chatbot
// ============================================================

const Chatbot = {

    init() {
        this.circle = document.getElementById('chat-circle');
        this.box = document.getElementById('chat-box');
        this.logs = document.getElementById('chat-logs');
        this.input = document.getElementById('chat-input');
        if (!this.circle || !this.box) return;

        this.circle.addEventListener('click', () => {
            this.circle.style.display = 'none';
            this.box.style.display = 'flex';
        });

        document.getElementById('chat-box-close')?.addEventListener('click', () => {
            this.box.style.display = 'none';
            this.circle.style.display = 'flex';
        });

        document.getElementById('chat-submit')?.addEventListener('click', () => this.gui());
        this.input?.addEventListener('keypress', e => {
            if (e.key === 'Enter') this.gui();
        });
    },

    async gui() {
        const msg = this.input.value.trim();
        if (!msg) return;

        this.them(msg, 'user');
        this.input.value = '';

        const endpoint = (typeof CHAT_API !== 'undefined' && CHAT_API) ? CHAT_API : '/api/v1/ai/chat';
        const currentUserId = (typeof USER_ID !== 'undefined' && USER_ID) ? USER_ID : (window.USER_ID || 'guest');

        const id = 'loading-' + Date.now();
        this.logs.insertAdjacentHTML('beforeend',
            `<div id="${id}" class="msg-bot text-muted fst-italic">Đang trả lời...</div>`);
        this.cuonXuong();

        try {
            const res = await api(endpoint, {
                method: 'POST',
                body: JSON.stringify({ message: msg, prompt: msg, userId: currentUserId, user_id: currentUserId }),
            });

            document.getElementById(id)?.remove();
            const rawText = res?.reply || res?.answer || res?.response || res?.message || 'Xin lỗi, tôi chưa hiểu.';
            const reply = String(rawText);
            this.them(reply, 'bot', true);
        } catch (err) {
            console.error('Lỗi Chatbot Frontend:', err);
            document.getElementById(id)?.remove();
            this.them('Không kết nối được trợ lý AI', 'bot', true);
        }
    },

    them(content, type, allowFormatting = false) {
        if (!this.logs) return;
        const node = document.createElement('div');
        node.className = type === 'user' ? 'msg-user' : 'msg-bot';
        if (allowFormatting) {
            const safe = esc(content)
                .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
                .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g,
                    '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
                .replace(/\n/g, '<br>');
            node.innerHTML = safe;
        } else {
            node.textContent = content;
        }
        this.logs.appendChild(node);
        this.cuonXuong();
    },

    cuonXuong() {
        if (this.logs) this.logs.scrollTop = this.logs.scrollHeight;
    },
};

document.addEventListener('DOMContentLoaded', () => {
    ChonPhong.init();
    DanhGia.init();
    TimPhong.init();
    BangDatPhong.init();
    DatTrucTiep.init();
    Chatbot.init();
});
