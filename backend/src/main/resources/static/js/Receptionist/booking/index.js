document.addEventListener('DOMContentLoaded', () => {
    'use strict';

    window.ADM_PAGE_MODULE = 'booking';

    /* =========================================================
       RENDER BẢNG
       ========================================================= */
    const tableBody = document.getElementById('table-body');
    const bookingCountEl = document.getElementById('bookingCount');

    const formatDate = (dateInput) => {
        if (!dateInput) return '-';
        const d = new Date(dateInput);
        if (Number.isNaN(d.getTime())) return dateInput;
        const day = String(d.getDate()).padStart(2, '0');
        const month = String(d.getMonth() + 1).padStart(2, '0');
        return `${day}/${month}/${d.getFullYear()}`;
    };

    function getStatusClass(status) {
        const s = (status || '').toLowerCase();
        if (s.includes('đã nhận') || s.includes('in use') || s.includes('checked') || s.includes('đang ở') || s.includes('completed'))
            return 'adm-badge adm-badge-success';
        if (s.includes('hủy') || s.includes('cancel'))
            return 'adm-badge adm-badge-danger';
        if (s.includes('chờ') || s.includes('pending') || s.includes('chưa') || s.includes('reserved'))
            return 'adm-badge adm-badge-warning';
        return 'adm-badge adm-badge-info';
    }

    function formatStatus(status) {
        if (!status) return 'Chờ nhận phòng';
        const s = status.toLowerCase();
        if (s === 'pending' || s === 'reserved') return 'Chờ nhận phòng';
        if (s === 'in use' || s === 'in_use') return 'Đang lưu trú';
        if (s === 'completed' || s === 'checked out') return 'Đã trả phòng';
        if (s === 'cancelled' || s === 'cancel') return 'Đã hủy';
        return status;
    }

    function getRiskClass(level) {
        const l = (level || '').toLowerCase();
        if (l.includes('cao')) return 'adm-badge adm-badge-danger';
        if (l.includes('trung bình')) return 'adm-badge adm-badge-warning';
        if (l.includes('thấp')) return 'adm-badge adm-badge-success';
        return 'adm-badge adm-badge-neutral';
    }

    function canEdit(status) {
        if (!status || !status.trim()) return true;
        const s = status.toLowerCase();
        return s.includes('pending') || s.includes('reserved')
            || s.includes('đã đặt') || s.includes('chờ nhận');
    }

    function renderActions(item) {
        const id = item.id;
        const editable = canEdit(item.status);

        let html = '<div class="adm-actions">';
        html += `<button type="button" class="adm-action view" data-action="view" data-booking-id="${id}" title="Xem chi tiết"><i class="bi bi-eye"></i></button>`;

        if (editable) {
            html += `<button type="button" class="adm-action edit" data-action="edit" data-booking-id="${id}" title="Sửa"><i class="bi bi-pencil"></i></button>`;
            html += `<button type="button" class="adm-action delete" data-action="cancel" data-booking-id="${id}" title="Hủy"><i class="bi bi-x-circle"></i></button>`;
        }

        html += '</div>';
        return html;
    }

    const rawBookings = Array.isArray(window.mockBookings) ? window.mockBookings : [];

    const bookings = rawBookings.map(b => {
        const order = b.order || {};
        const id = b.id != null ? b.id : (order.id != null ? order.id : '');
        const checkinMs = b.expectedInMs != null ? b.expectedInMs
            : (b.expectedIn ? new Date(b.expectedIn).getTime()
                : (order.expectedIn ? new Date(order.expectedIn).getTime() : null));
        const checkoutMs = b.expectedOutMs != null ? b.expectedOutMs
            : (b.expectedOut ? new Date(b.expectedOut).getTime()
                : (order.expectedOut ? new Date(order.expectedOut).getTime() : null));
        const rawStatus = b.status || order.status || 'Chờ nhận phòng';
        const riskLevel = b.noShowRiskLevel || order.noShowRiskLevel || 'Thấp';
        const riskPercent = b.noShowRiskPercentage != null ? b.noShowRiskPercentage
            : (order.noShowRiskPercentage != null ? order.noShowRiskPercentage : 15.0);

        return {
            id: id,
            customer: b.customerName || 'Khách vãng lai',
            room: b.roomName || 'Chưa gán',
            checkin: formatDate(checkinMs),
            checkout: formatDate(checkoutMs),
            status: rawStatus,
            riskLevel: riskLevel,
            riskPercent: riskPercent
        };
    });

    function renderTable(list) {
        if (!tableBody) return;
        tableBody.innerHTML = '';

        if (list.length === 0) {
            tableBody.innerHTML = `
                <tr><td colspan="8">
                    <div class="adm-empty">
                        <i class="bi bi-calendar-x"></i>
                        <h4>Chưa có đơn đặt phòng</h4>
                        <p>Bấm "Thêm đặt phòng" để tạo đơn mới tại quầy lễ tân.</p>
                    </div>
                </td></tr>`;
            if (bookingCountEl) bookingCountEl.textContent = 0;
            return;
        }

        list.forEach(item => {
            const tr = document.createElement('tr');
            const riskPercent = item.riskPercent != null ? ` (${item.riskPercent}%)` : '';

            tr.innerHTML = `
                <td class="col-center cell-id">#${item.id}</td>
                <td>${escapeHtml(item.customer)}</td>
                <td class="col-center fw-bold">${escapeHtml(item.room)}</td>
                <td class="col-center">${item.checkin}</td>
                <td class="col-center">${item.checkout}</td>
                <td class="col-center">
                    <span class="${getStatusClass(item.status)}">
                        <span class="dot"></span>${escapeHtml(formatStatus(item.status))}
                    </span>
                </td>
                <td class="col-center">
                    <span class="${getRiskClass(item.riskLevel)}">
                        <i class="bi bi-robot"></i>
                        ${escapeHtml(item.riskLevel)}${riskPercent}
                    </span>
                </td>
                <td class="col-center">${renderActions(item)}</td>
            `;
            tableBody.appendChild(tr);
        });

        if (bookingCountEl) bookingCountEl.textContent = list.length;
    }

    renderTable(bookings);

    /* =========================================================
       SEARCH
       ========================================================= */
    const searchInput = document.getElementById('bookingSearch');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            const kw = this.value.trim().toLowerCase();
            const filtered = !kw ? bookings.slice() : bookings.filter(item => {
                const text = [item.id, item.customer, item.room, item.status, item.riskLevel]
                    .join(' ').toLowerCase();
                return text.includes(kw);
            });
            renderTable(filtered);
        });
    }

    /* =========================================================
       ACTION BUTTONS — Xem / Sửa / Hủy
       ========================================================= */
    document.addEventListener('click', function (e) {
        const btn = e.target.closest('[data-action][data-booking-id]');
        if (!btn) return;

        const action = btn.dataset.action;
        const id = btn.dataset.bookingId;

        if (action === 'view') openViewModal(id);
        if (action === 'edit') openEditModal(id);
        if (action === 'cancel') cancelBooking(id);
    });

    async function fetchDetail(id) {
        const res = await fetch(`/receptionist/booking/detail/${id}`);
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();
        if (data.error) throw new Error(data.error);
        return data;
    }

    async function openViewModal(id) {
        try {
            const data = await fetchDetail(id);

            document.getElementById('view-booking-id').textContent = '#' + data.id;
            document.getElementById('view-booking-status').textContent = formatStatus(data.status);

            document.getElementById('view-customer').innerHTML = `
                ${factRow('Họ tên', data.customerName || '—')}
                ${factRow('SĐT', data.customerPhone || '—')}
                ${factRow('Email', data.customerEmail || '—')}
                ${factRow('Điểm tích lũy', data.customerPoints != null ? data.customerPoints + ' điểm' : '—')}
            `;

            const checkIn = formatDate(data.expectedIn);
            const checkOut = formatDate(data.expectedOut);
            let nights = '—';
            if (data.expectedIn && data.expectedOut) {
                const n = Math.round((new Date(data.expectedOut) - new Date(data.expectedIn)) / 86400000);
                nights = n + ' đêm';
            }

            document.getElementById('view-stay').innerHTML = `
                ${factRow('Phòng', data.roomName || 'Chưa gán')}
                ${factRow('Loại phòng', data.roomTypeName || '—')}
                ${factRow('Check-in', checkIn)}
                ${factRow('Check-out', checkOut)}
                ${factRow('Số đêm', nights)}
                ${factRow('Số khách', data.guestCount != null ? data.guestCount + ' khách' : '—')}
            `;

            const money = (v) => v != null ? Number(v).toLocaleString('vi-VN') + ' đ' : '—';
            document.getElementById('view-finance').innerHTML = `
                ${factRow('Đơn giá', money(data.roomPrice))}
                ${factRow('Tổng tiền', money(data.totalAmount))}
                ${factRow('Đã thanh toán', data.isPaid ? '✓ Có' : '✗ Chưa')}
            `;

            const risk = data.noShowRiskPercentage;
            let riskHtml = '—';
            if (risk != null) {
                let color = '#1A7A3F';
                if (risk > 70) color = '#C0392B';
                else if (risk > 30) color = '#A05A00';
                riskHtml = `<strong style="color:${color};">${risk}%</strong> <span style="font-size:12px;color:var(--muted);">(${data.noShowRiskLevel || '—'})</span>`;
            }
            document.getElementById('view-ai').innerHTML = `
                ${factRowHtml('Rủi ro No-show', riskHtml)}
                ${factRow('Tự động hủy lúc', data.autoCancelTime ? formatDate(data.autoCancelTime) : '—')}
            `;

            const notesEl = document.getElementById('view-notes');
            const notesSection = document.getElementById('view-notes-section');
            if (data.notes && data.notes.trim()) {
                notesEl.textContent = data.notes;
                notesSection.style.display = '';
            } else {
                notesSection.style.display = 'none';
            }

            if (typeof AdmModal !== 'undefined') AdmModal.open('viewBookingModal');
        } catch (err) {
            console.error('[booking] view error:', err);
            if (typeof toast === 'function') toast(err.message || 'Không tải được chi tiết đơn', 'danger');
        }
    }

    function factRow(label, value) {
        return `<div class="adm-view-fact">
            <span class="adm-view-fact-label">${label}</span>
            <span class="adm-view-fact-value">${escapeHtml(value)}</span>
        </div>`;
    }

    function factRowHtml(label, valueHtml) {
        return `<div class="adm-view-fact">
            <span class="adm-view-fact-label">${label}</span>
            <span class="adm-view-fact-value">${valueHtml}</span>
        </div>`;
    }

    async function openEditModal(id) {
        try {
            const data = await fetchDetail(id);

            document.getElementById('edit-booking-code').value = '#' + data.id;
            document.getElementById('edit-checkin').value = formatDate(data.expectedIn);

            if (data.expectedOut) {
                const d = new Date(data.expectedOut);
                const yyyy = d.getFullYear();
                const mm = String(d.getMonth() + 1).padStart(2, '0');
                const dd = String(d.getDate()).padStart(2, '0');
                document.getElementById('edit-checkout').value = `${yyyy}-${mm}-${dd}`;
            } else {
                document.getElementById('edit-checkout').value = '';
            }

            if (data.expectedIn) {
                const d = new Date(data.expectedIn);
                d.setDate(d.getDate() + 1);
                const yyyy = d.getFullYear();
                const mm = String(d.getMonth() + 1).padStart(2, '0');
                const dd = String(d.getDate()).padStart(2, '0');
                document.getElementById('edit-checkout').min = `${yyyy}-${mm}-${dd}`;
            }

            document.getElementById('edit-guest-count').value = data.guestCount || 1;
            document.getElementById('edit-guest-count').max = data.maxOccupancy || 100;
            document.getElementById('edit-phone').value = data.customerPhone || '';
            document.getElementById('edit-notes').value = data.notes || '';

            document.getElementById('editBookingForm').action = `/receptionist/booking/edit/${data.id}`;

            if (typeof AdmModal !== 'undefined') AdmModal.open('editBookingModal');
        } catch (err) {
            console.error('[booking] edit error:', err);
            if (typeof toast === 'function') toast(err.message || 'Không tải được thông tin đơn', 'danger');
        }
    }

    function cancelBooking(id) {
        const reason = prompt('Lý do hủy đơn #' + id + ' (có thể bỏ trống):');
        if (reason === null) return;

        const meta = document.querySelector('meta[name="_csrf"]');
        const csrfToken = meta ? meta.content : '';
        const csrfName = document.querySelector('meta[name="_csrf_header"]')?.content || '_csrf';

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/receptionist/booking/cancel/${id}`;

        if (csrfToken) {
            const csrfField = document.createElement('input');
            csrfField.type = 'hidden';
            csrfField.name = csrfName;
            csrfField.value = csrfToken;
            form.appendChild(csrfField);
        }

        const reasonField = document.createElement('input');
        reasonField.type = 'hidden';
        reasonField.name = 'reason';
        reasonField.value = reason || '';
        form.appendChild(reasonField);

        document.body.appendChild(form);
        form.submit();
    }

    /* =========================================================
       MODAL: TÌM PHÒNG TRỐNG
       ========================================================= */
    const btnSearchRooms = document.getElementById('btn-search-rooms');
    const searchResults = document.getElementById('db-search-results');

    if (btnSearchRooms) {
        btnSearchRooms.addEventListener('click', async () => {
            const checkin = document.getElementById('db-checkin').value;
            const checkout = document.getElementById('db-checkout').value;

            if (!checkin || !checkout) {
                if (typeof toast === 'function') toast('Vui lòng chọn ngày Check-in và Check-out', 'warning');
                return;
            }
            if (checkin >= checkout) {
                if (typeof toast === 'function') toast('Check-out phải sau Check-in', 'warning');
                return;
            }

            const roomSelect = document.getElementById('db-selected-room');
            roomSelect.innerHTML = '<option value="">Đang tải...</option>';

            try {
                const res = await fetch(
                    `/receptionist/booking/api/available-rooms?checkin=${checkin}&checkout=${checkout}`
                );
                if (!res.ok) throw new Error('HTTP ' + res.status);

                const rooms = await res.json();
                renderRoomOptions(roomSelect, rooms);

            } catch (err) {
                console.warn('[booking] API lỗi, fallback mockVacantRooms:', err);
                const rooms = Array.isArray(window.mockVacantRooms) ? window.mockVacantRooms : [];
                renderRoomOptions(roomSelect, rooms);
            }

            searchResults.style.display = '';
        });
    }

    function renderRoomOptions(select, rooms) {
        if (!rooms || rooms.length === 0) {
            select.innerHTML = '<option value="">(Không có phòng trống trong khoảng ngày này)</option>';
            return;
        }
        select.innerHTML = '<option value="">— Chọn phòng —</option>' +
            rooms.map(r => {
                const name = r.name || ('Phòng ' + r.id);
                const price = r.price
                    ? ' — ' + Number(r.price).toLocaleString('vi-VN') + ' đ'
                    : '';
                return `<option value="${r.id}">${name}${price}</option>`;
            }).join('');
    }

    /* =========================================================
       MODAL: TRA CỨU KHÁCH CCCD
       ========================================================= */
    const btnCheckCustomer = document.getElementById('btn-check-customer');
    const inputCccd = document.getElementById('db-cccd');
    const inputFullname = document.getElementById('db-fullname');
    const inputPhone = document.getElementById('db-phone');

    if (btnCheckCustomer) {
        btnCheckCustomer.addEventListener('click', async () => {
            const cccd = inputCccd.value.trim();
            if (!cccd) {
                if (typeof toast === 'function') toast('Vui lòng nhập CCCD', 'warning');
                return;
            }
            try {
                const res = await fetch(`/api/customer/by-cccd?cccd=${encodeURIComponent(cccd)}`);
                const data = res.ok ? await res.json() : null;

                if (data && data.fullName) {
                    inputFullname.value = data.fullName;
                    inputPhone.value = data.phone || '';
                    if (typeof toast === 'function') toast('Đã tìm thấy khách hàng cũ');
                } else {
                    inputFullname.value = '';
                    inputPhone.value = '';
                    if (typeof toast === 'function') toast('Khách hàng mới — vui lòng nhập thông tin', 'info');
                }
            } catch (err) {
                if (cccd === '0123456789' || cccd === '079099123456') {
                    inputFullname.value = 'Nguyễn Văn Khách Cũ';
                    inputPhone.value = '0901234567';
                    if (typeof toast === 'function') toast('Đã tìm thấy khách hàng cũ (demo)');
                } else {
                    inputFullname.value = '';
                    inputPhone.value = '';
                    if (typeof toast === 'function') toast('Khách hàng mới — vui lòng nhập thông tin', 'info');
                }
            }
        });
    }

    /* =========================================================
       MODAL: NFC (demo)
       ========================================================= */
    const btnNfcScan = document.getElementById('btn-nfc-scan');
    if (btnNfcScan) {
        btnNfcScan.addEventListener('click', () => {
            if (typeof toast === 'function') toast('Đang chờ thiết bị đọc NFC...', 'info');
            setTimeout(() => {
                inputCccd.value = '079099123456';
                inputFullname.value = 'Nguyễn Văn Khách Cũ (NFC)';
                inputPhone.value = '0901234567';
                if (typeof toast === 'function') toast('Đã đọc thẻ CCCD thành công');
            }, 800);
        });
    }

    /* =========================================================
       HELPER
       ========================================================= */
    function escapeHtml(s) {
        if (s == null) return '';
        const d = document.createElement('div');
        d.textContent = String(s);
        return d.innerHTML;
    }
});