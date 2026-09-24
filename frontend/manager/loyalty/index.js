document.addEventListener('DOMContentLoaded', () => {
    // Sửa lỗi backdrop che modal
    const modalElement = document.getElementById('loyaltyModal');
    if (modalElement) {
        document.body.appendChild(modalElement);
        modalElement.addEventListener('hidden.bs.modal', () => {
            document.getElementById('loyaltyForm').reset();
            document.getElementById('loyaltyModalTitle').innerText = 'Thêm Hạng Thành Viên';
        });
    }

    const mockData = [
        { tier: 'Hạng Đồng (Bronze)', points: '0 - 999', perks: 'Tích điểm cơ bản', status: 'Áp dụng', badgeColor: 'bg-secondary' },
        { tier: 'Hạng Bạc (Silver)', points: '1000 - 4999', perks: 'Giảm 5% hóa đơn phòng, Nước uống Welcome', status: 'Áp dụng', badgeColor: 'bg-info' },
        { tier: 'Hạng Vàng (Gold)', points: '5000 - 9999', perks: 'Giảm 10%, Trả phòng muộn 14:00', status: 'Áp dụng', badgeColor: 'bg-warning text-dark' },
        { tier: 'Hạng Kim Cương (Diamond)', points: '10000+', perks: 'Giảm 15%, Đưa đón sân bay miễn phí', status: 'Áp dụng', badgeColor: 'bg-primary' }
    ];
    
    const tableBody = document.getElementById('table-body');
    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold"><span class="badge ${item.badgeColor} fs-6 shadow-sm">${item.tier}</span></td>
                <td class="text-center fw-bold text-danger">${item.points}</td>
                <td class="text-start">${item.perks}</td>
                <td class="text-center"><span class="badge bg-success px-3 py-2 rounded-pill shadow-sm">${item.status}</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-warning text-white shadow-sm mx-1 btn-edit-loyalty" data-tier="${item.tier}" title="Chỉnh sửa"><i class="bi bi-pencil"></i></button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
        bindEvents();
    }
    renderData(mockData);

    const searchLoyalty = document.getElementById('searchLoyalty');
    const thFilterLoyaltyName = document.getElementById('thFilterLoyaltyName');

    if (thFilterLoyaltyName) {
        const uniqueTiers = [...new Set(mockData.map(item => item.tier))];
        thFilterLoyaltyName.innerHTML = uniqueTiers.map((tier, idx) => `
            <li>
                <div class="form-check mb-1 ms-1">
                    <input class="form-check-input th-cb-tier" type="checkbox" value="${tier}" id="th_loyalty_${idx}">
                    <label class="form-check-label text-truncate" style="max-width: 170px;" title="${tier}" for="th_loyalty_${idx}">${tier}</label>
                </div>
            </li>
        `).join('');

        document.querySelectorAll('.th-cb-tier').forEach(cb => {
            cb.addEventListener('change', applyFilters);
        });
    }

    function applyFilters() {
        const keyword = (searchLoyalty ? searchLoyalty.value : '').toLowerCase().trim();
        const selectedTiers = Array.from(document.querySelectorAll('.th-cb-tier:checked')).map(cb => cb.value);

        const filtered = mockData.filter(item => {
            const matchKeyword = item.tier.toLowerCase().includes(keyword);
            const matchTier = selectedTiers.length === 0 || selectedTiers.includes(item.tier);
            return matchKeyword && matchTier;
        });
        renderData(filtered);
    }

    if (searchLoyalty) {
        searchLoyalty.addEventListener('input', applyFilters);
    }

    // Xử lý nút Sửa
    function bindEvents() {
        document.querySelectorAll('.btn-edit-loyalty').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tierName = e.currentTarget.dataset.tier;
                const item = mockData.find(m => m.tier === tierName);
                if(item) {
                    document.getElementById('loyaltyModalTitle').innerText = 'Cập Nhật Hạng Thành Viên';
                    document.getElementById('form-loyalty-name').value = item.tier;
                    document.getElementById('form-loyalty-points').value = item.points;
                    document.getElementById('form-loyalty-perks').value = item.perks;
                    document.getElementById('form-loyalty-color').value = item.badgeColor;
                    document.getElementById('form-loyalty-status').value = item.status;
                    new bootstrap.Modal(document.getElementById('loyaltyModal')).show();
                }
            });
        });
    }
});
