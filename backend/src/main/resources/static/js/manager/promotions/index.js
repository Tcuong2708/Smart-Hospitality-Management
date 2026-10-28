document.addEventListener('DOMContentLoaded', () => {
    const tableBody = document.getElementById('table-body');
    const search = document.getElementById('searchPromotion');
    const form = document.getElementById('promotionForm');
    let promotions = [];
    let editingId = null;

    const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
    const statusText = p => ({
        ACTIVE: 'Đang diễn ra', SCHEDULED: 'Sắp diễn ra', EXPIRED: 'Hết hạn',
        DISABLED: 'Tạm ngưng', DRAFT: 'Bản nháp'
    }[p.status] || p.status || 'Bản nháp');
    const apiStatus = value => ({
        'Đang diễn ra': 'ACTIVE',
        'Sắp diễn ra': 'SCHEDULED',
        'Hết hạn': 'EXPIRED',
        'Tạm ngưng': 'DISABLED',
        'Bản nháp': 'DRAFT'
    }[value] || value || 'DRAFT');
    const discountText = p => p.discountType === 'AMOUNT'
        ? `${Number(p.discountAmount || 0).toLocaleString('vi-VN')} đ`
        : `${p.discountPercent || 0}%`;

    async function load() {
        const response = await fetch('/manager/promotions/api');
        if (!response.ok) throw new Error('Không thể tải danh sách khuyến mãi.');
        promotions = await response.json();
        render();
    }

    function render() {
        const keyword = (search?.value || '').toLowerCase().trim();
        const items = promotions.filter(p => `${p.code || ''} ${p.name || ''}`.toLowerCase().includes(keyword));
        tableBody.innerHTML = items.length ? items.map(p => `
            <tr>
              <td class="text-center fw-bold text-warning">${esc(p.code || p.id)}</td>
              <td class="text-center fw-bold">${esc(p.name)}</td>
              <td class="text-center fw-bold text-danger">${esc(discountText(p))}</td>
              <td class="text-center">${p.startDate ? new Date(p.startDate).toLocaleDateString('vi-VN') : '—'}
                - ${p.endDate ? new Date(p.endDate).toLocaleDateString('vi-VN') : '—'}</td>
              <td class="text-center"><span class="badge bg-secondary px-3 py-2 rounded-pill">${esc(statusText(p))}</span></td>
              <td class="text-center">
                <button class="btn btn-sm btn-warning btn-edit" data-id="${p.id}"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-danger btn-disable" data-id="${p.id}"><i class="bi bi-pause"></i></button>
              </td>
            </tr>`).join('')
            : '<tr><td colspan="6" class="text-center py-5 text-muted">Không có khuyến mãi.</td></tr>';
        document.querySelectorAll('.btn-edit').forEach(b => b.onclick = () => openEdit(Number(b.dataset.id)));
        document.querySelectorAll('.btn-disable').forEach(b => b.onclick = () => disable(Number(b.dataset.id)));
    }

    function openEdit(id) {
        const p = promotions.find(item => item.id === id);
        if (!p) return;
        editingId = id;
        document.getElementById('form-promo-code').value = p.code || '';
        document.getElementById('form-promo-name').value = p.name || '';
        document.getElementById('form-promo-discount').value = p.discountPercent || p.discountAmount || 0;
        document.getElementById('form-promo-type').value = p.type || 'SPECIAL_DATE';
        document.getElementById('form-promo-start').value = p.startDate ? p.startDate.substring(0, 10) : '';
        document.getElementById('form-promo-end').value = p.endDate ? p.endDate.substring(0, 10) : '';
        document.getElementById('form-promo-min-rooms').value = p.minimumRooms || '';
        document.getElementById('form-promo-min-nights').value = p.minimumNights || '';
        document.getElementById('form-promo-advance-days').value = p.advanceDays || '';
        document.getElementById('form-promo-status').value = apiStatus(p.status);
        bootstrap.Modal.getOrCreateInstance(document.getElementById('promotionModal')).show();
    }

    async function disable(id) {
        if (!confirm('Bạn có chắc muốn vô hiệu hóa chương trình này?')) return;
        const response = await fetch(`/manager/promotions/api/${id}/disable`, { method: 'POST' });
        if (!response.ok) return alert('Không thể vô hiệu hóa khuyến mãi.');
        await load();
    }

    search?.addEventListener('input', render);
    form?.addEventListener('submit', async event => {
        event.preventDefault();
        const payload = {
            code: document.getElementById('form-promo-code').value.trim(),
            name: document.getElementById('form-promo-name').value.trim(),
            type: document.getElementById('form-promo-type').value,
            discountType: 'PERCENT',
            discountPercent: Number.parseFloat(document.getElementById('form-promo-discount').value) || 0,
            startDate: document.getElementById('form-promo-start').value
                ? `${document.getElementById('form-promo-start').value}T00:00:00.000Z` : null,
            endDate: document.getElementById('form-promo-end').value
                ? `${document.getElementById('form-promo-end').value}T23:59:59.999Z` : null,
            minimumRooms: Number.parseInt(document.getElementById('form-promo-min-rooms').value) || null,
            minimumNights: Number.parseInt(document.getElementById('form-promo-min-nights').value) || null,
            advanceDays: Number.parseInt(document.getElementById('form-promo-advance-days').value) || null,
            status: apiStatus(document.getElementById('form-promo-status').value)
        };
        if (!payload.name) return alert('Tên chương trình là bắt buộc.');
        if (payload.type === 'VOUCHER' && !payload.code) {
            return alert('Voucher phải có mã khuyến mãi.');
        }
        const url = editingId ? `/manager/promotions/api/${editingId}` : '/manager/promotions/api';
        const response = await fetch(url, {
            method: editingId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const result = await response.json().catch(() => ({}));
        if (!response.ok) return alert(result.message || 'Dữ liệu không hợp lệ.');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('promotionModal')).hide();
        editingId = null;
        await load();
    });

    load().catch(error => {
        tableBody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-5">${esc(error.message)}</td></tr>`;
    });
});
