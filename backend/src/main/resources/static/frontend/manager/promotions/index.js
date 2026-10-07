document.addEventListener('DOMContentLoaded', () => {
    // Sửa lỗi backdrop che modal
    const modalElement = document.getElementById('promotionModal');
    let editingPromoId = null;

    if (modalElement) {
        document.body.appendChild(modalElement);
        modalElement.addEventListener('hidden.bs.modal', () => {
            document.getElementById('promotionForm').reset();
            document.getElementById('promotionModalTitle').innerText = 'Thêm Khuyến Mãi Mới';
            document.getElementById('form-promo-code').readOnly = false;
            editingPromoId = null;
        });
    }

    let mockData = [
        { id: 'SUMMER2026', name: 'Chào Hè Rực Rỡ', discount: '15%', minBill: '1.500.000 đ', time: '01/06/2026 - 31/08/2026', status: 'Đang diễn ra' },
        { id: 'AUTUMN2026', name: 'Thu Sang Ưu Đãi Khủng', discount: '10%', minBill: '1.000.000 đ', time: '01/09/2026 - 30/11/2026', status: 'Đang diễn ra' },
        { id: 'TET2027', name: 'Tết Sum Vầy', discount: '20%', minBill: '2.000.000 đ', time: '15/01/2027 - 15/02/2027', status: 'Sắp diễn ra' },
        { id: 'VIPGUEST', name: 'Tri Ân Khách VIP', discount: '200.000 đ', minBill: '500.000 đ', time: 'Không giới hạn', status: 'Đang diễn ra' },
        { id: 'EXPIRED2025', name: 'Ưu Đãi Năm Cũ', discount: '30%', minBill: '3.000.000 đ', time: '01/01/2025 - 31/12/2025', status: 'Hết hạn' }
    ];

    const tableBody = document.getElementById('table-body');

    function showToast(msg, type = 'success') {
        if (window.showToast) {
            window.showToast(msg, type);
        } else {
            alert(msg);
        }
    }

    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';

        if (data.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Không tìm thấy khuyến mãi nào.</td></tr>`;
            return;
        }

        data.forEach(item => {
            let statusBadge = 'bg-secondary';
            if (item.status === 'Đang diễn ra') statusBadge = 'bg-success';
            else if (item.status === 'Sắp diễn ra') statusBadge = 'bg-warning text-dark';
            else if (item.status === 'Tạm ngưng') statusBadge = 'bg-danger';

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold" style="color: var(--accent-color);">${item.id}</td>
                <td class="text-center fw-bold" style="color: var(--navy-color);">${item.name}</td>
                <td class="text-center fw-bold text-danger">${item.discount}</td>
                <td class="text-center">${item.time}</td>
                <td class="text-center"><span class="badge ${statusBadge} px-3 py-2 rounded-pill shadow-sm">${item.status}</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-info text-white shadow-sm btn-view-promo me-1" data-id="${item.id}" title="Xem chi tiết"><i class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-warning text-white shadow-sm me-1 btn-edit-promo" data-id="${item.id}" title="Chỉnh sửa"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-danger text-white shadow-sm btn-delete-promo" data-id="${item.id}" title="Xóa"><i class="bi bi-trash"></i></button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
        bindEvents();
    }

    renderData(mockData);

    const searchPromotion = document.getElementById('searchPromotion');
    const thFilterStatus = document.getElementById('thFilterStatus');

    if (thFilterStatus) {
        const statuses = ['Đang diễn ra', 'Sắp diễn ra', 'Hết hạn', 'Tạm ngưng'];
        thFilterStatus.innerHTML = statuses.map((st, idx) => `
            <li>
                <div class="form-check mb-1 ms-1">
                    <input class="form-check-input th-cb-status" type="checkbox" value="${st}" id="th_st_${idx}">
                    <label class="form-check-label" for="th_st_${idx}">${st}</label>
                </div>
            </li>
        `).join('');

        document.querySelectorAll('.th-cb-status').forEach(cb => {
            cb.addEventListener('change', applyFilter);
        });
    }

    function applyFilter() {
        const keyword = (searchPromotion ? searchPromotion.value : '').toLowerCase().trim();
        const selectedStatuses = Array.from(document.querySelectorAll('.th-cb-status:checked')).map(cb => cb.value);

        const filtered = mockData.filter(item => {
            const matchKeyword = item.name.toLowerCase().includes(keyword) || item.id.toLowerCase().includes(keyword);
            const matchStatus = selectedStatuses.length === 0 || selectedStatuses.includes(item.status);
            return matchKeyword && matchStatus;
        });
        renderData(filtered);
    }

    if (searchPromotion) searchPromotion.addEventListener('input', applyFilter);

    // Bind events cho Sửa, Xem, Xóa
    function bindEvents() {
        // Xem chi tiết
        document.querySelectorAll('.btn-view-promo').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const promoId = e.currentTarget.dataset.id;
                const item = mockData.find(m => m.id === promoId);
                if (item) {
                    document.getElementById('view-promo-id').textContent = item.id;
                    document.getElementById('view-promo-name').textContent = item.name;
                    document.getElementById('view-promo-discount').textContent = item.discount;
                    document.getElementById('view-promo-min-bill').textContent = item.minBill || 'Không quy định';
                    document.getElementById('view-promo-time').textContent = item.time;
                    document.getElementById('view-promo-status').textContent = item.status;
                    new bootstrap.Modal(document.getElementById('viewPromotionModal')).show();
                }
            });
        });

        // Sửa
        document.querySelectorAll('.btn-edit-promo').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const promoId = e.currentTarget.dataset.id;
                const item = mockData.find(m => m.id === promoId);
                if (item) {
                    editingPromoId = item.id;
                    document.getElementById('promotionModalTitle').innerText = 'Cập Nhật Khuyến Mãi';
                    document.getElementById('form-promo-code').value = item.id;
                    document.getElementById('form-promo-code').readOnly = true;
                    document.getElementById('form-promo-name').value = item.name;
                    document.getElementById('form-promo-discount').value = item.discount;
                    document.getElementById('form-promo-min-bill').value = item.minBill || '';
                    document.getElementById('form-promo-time').value = item.time;
                    document.getElementById('form-promo-status').value = item.status;
                    new bootstrap.Modal(document.getElementById('promotionModal')).show();
                }
            });
        });

        // Xóa
        document.querySelectorAll('.btn-delete-promo').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const promoId = e.currentTarget.dataset.id;
                if (confirm(`Bạn có chắc chắn muốn xóa chương trình khuyến mãi "${promoId}" không?`)) {
                    mockData = mockData.filter(m => m.id !== promoId);
                    applyFilter();
                    showToast(`Đã xóa khuyến mãi ${promoId} thành công!`, 'success');
                }
            });
        });
    }

    // Lưu thông tin (Thêm mới / Cập nhật)
    const btnSave = document.getElementById('btn-save-promotion');
    if (btnSave) {
        btnSave.addEventListener('click', () => {
            const code = document.getElementById('form-promo-code').value.trim();
            const name = document.getElementById('form-promo-name').value.trim();
            const discount = document.getElementById('form-promo-discount').value.trim();
            const minBill = document.getElementById('form-promo-min-bill').value.trim() || 'Không quy định';
            const time = document.getElementById('form-promo-time').value.trim();
            const status = document.getElementById('form-promo-status').value;

            if (!code || !name || !discount || !time) {
                alert('Vui lòng điền đầy đủ các thông tin bắt buộc (*)');
                return;
            }

            if (editingPromoId) {
                // Cập nhật
                const index = mockData.findIndex(m => m.id === editingPromoId);
                if (index !== -1) {
                    mockData[index] = { id: code, name, discount, minBill, time, status };
                    showToast(`Cập nhật chương trình khuyến mãi ${code} thành công!`, 'success');
                }
            } else {
                // Thêm mới
                if (mockData.some(m => m.id === code)) {
                    alert('Mã khuyến mãi đã tồn tại. Vui lòng nhập mã khác!');
                    return;
                }
                mockData.unshift({ id: code, name, discount, minBill, time, status });
                showToast(`Thêm khuyến mãi mới ${code} thành công!`, 'success');
            }

            const modalInstance = bootstrap.Modal.getInstance(document.getElementById('promotionModal'));
            if (modalInstance) modalInstance.hide();

            applyFilter();
        });
    }
});
