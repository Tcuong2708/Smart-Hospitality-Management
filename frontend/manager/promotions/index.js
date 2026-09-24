document.addEventListener('DOMContentLoaded', () => {
    // Sửa lỗi backdrop che modal
    const modalElement = document.getElementById('promotionModal');
    if (modalElement) {
        document.body.appendChild(modalElement);
        modalElement.addEventListener('hidden.bs.modal', () => {
            document.getElementById('promotionForm').reset();
            document.getElementById('promotionModalTitle').innerText = 'Thêm Khuyến Mãi Mới';
        });
    }

    const mockData = [
        { id: 'SUMMER2026', name: 'Chào Hè Rực Rỡ', discount: '15%', time: '01/06/2026 - 31/08/2026', status: 'Hết hạn' },
        { id: 'TET2027', name: 'Tết Sum Vầy', discount: '20%', time: '15/01/2027 - 15/02/2027', status: 'Sắp diễn ra' },
        { id: 'VIPGUEST', name: 'Tri Ân Khách VIP', discount: '10%', time: 'Không giới hạn', status: 'Đang diễn ra' }
    ];
    
    const tableBody = document.getElementById('table-body');
    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        data.forEach(item => {
            let statusBadge = '';
            if (item.status === 'Đang diễn ra') statusBadge = 'bg-success';
            else if (item.status === 'Hết hạn') statusBadge = 'bg-secondary';
            else statusBadge = 'bg-warning text-dark';
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold" style="color: var(--accent-color);">${item.id}</td>
                <td class="text-center fw-bold" style="color: var(--navy-color);">${item.name}</td>
                <td class="text-center fw-bold text-danger">${item.discount}</td>
                <td class="text-center">${item.time}</td>
                <td class="text-center"><span class="badge ${statusBadge} px-3 py-2 rounded-pill shadow-sm">${item.status}</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-info text-white shadow-sm" title="Xem chi tiết"><i class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-warning text-white shadow-sm mx-1 btn-edit-promo" data-id="${item.id}" title="Chỉnh sửa"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-danger text-white shadow-sm" title="Xóa"><i class="bi bi-trash"></i></button>
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
        const statuses = ['Sắp diễn ra', 'Đang diễn ra', 'Hết hạn'];
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

    // Xử lý nút Sửa
    function bindEvents() {
        document.querySelectorAll('.btn-edit-promo').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const promoId = e.currentTarget.dataset.id;
                const item = mockData.find(m => m.id === promoId);
                if(item) {
                    document.getElementById('promotionModalTitle').innerText = 'Cập Nhật Khuyến Mãi';
                    document.getElementById('form-promo-code').value = item.id;
                    document.getElementById('form-promo-name').value = item.name;
                    document.getElementById('form-promo-discount').value = item.discount;
                    document.getElementById('form-promo-time').value = item.time;
                    document.getElementById('form-promo-status').value = item.status;
                    new bootstrap.Modal(document.getElementById('promotionModal')).show();
                }
            });
        });
    }
});
