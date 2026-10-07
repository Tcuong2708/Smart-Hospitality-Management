document.addEventListener('DOMContentLoaded', () => {
    
    // Mock user points history data
    const pointsData = {
        currentPoints: 5420,
        membershipTier: "Hạng Vàng",
        history: [
            { id: "HD092", title: "Thanh toán hóa đơn phòng VIP #101", date: "02/10/2026", type: "EARN", points: +420, note: "Tích lũy 5% từ hóa đơn 8.400.000đ" },
            { id: "HD085", title: "Sử dụng điểm đổi voucher giảm giá", date: "15/09/2026", type: "REDEEM", points: -1000, note: "Giảm 1.000.000đ hóa đơn đặt phòng trực tuyến" },
            { id: "HD071", title: "Thanh toán hóa đơn phòng Deluxe #202", date: "28/08/2026", type: "EARN", points: +600, note: "Tích lũy 5% từ hóa đơn 12.000.000đ" },
            { id: "HD050", title: "Thưởng sinh nhật khách hàng VIP", date: "10/08/2026", type: "EARN", points: +500, note: "Quà tặng sinh nhật thành viên Hạng Vàng" },
            { id: "HD032", title: "Thanh toán hóa đơn dịch vụ Spa & Buffet", date: "05/07/2026", type: "EARN", points: +300, note: "Tích lũy dịch vụ đi kèm" },
            { id: "HD012", title: "Điểm thưởng chào mừng tân binh", date: "01/01/2026", type: "EARN", points: +4600, note: "Tạo tài khoản và xác thực thành viên" }
        ]
    };

    const tbody = document.getElementById('points-history-body');
    const userPointsEl = document.getElementById('user-points');
    const userVndEl = document.getElementById('user-vnd');
    const filterBtns = document.querySelectorAll('.filter-btn');
    const searchInput = document.getElementById('search-history');

    let currentFilter = 'all';

    // Format currency
    const formatNumber = (num) => new Intl.NumberFormat('vi-VN').format(num);

    // Update Header Stats
    if (userPointsEl) userPointsEl.innerHTML = `${formatNumber(pointsData.currentPoints)} <span class="fs-4 text-white">Điểm</span>`;
    if (userVndEl) userVndEl.innerHTML = `${formatNumber(pointsData.currentPoints * 1000)} VNĐ`;

    function renderHistory() {
        if (!tbody) return;
        const searchTerm = (searchInput ? searchInput.value : '').toLowerCase().trim();

        const filtered = pointsData.history.filter(item => {
            const matchFilter = currentFilter === 'all' || item.type === currentFilter;
            const matchSearch = item.id.toLowerCase().includes(searchTerm) || item.title.toLowerCase().includes(searchTerm) || item.note.toLowerCase().includes(searchTerm);
            return matchFilter && matchSearch;
        });

        if (filtered.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-5 text-muted">
                        <i class="bi bi-inbox fs-2 d-block mb-2"></i>
                        Không tìm thấy lịch sử giao dịch phù hợp.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = filtered.map(item => {
            const isEarn = item.type === 'EARN';
            const badgeClass = isEarn ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-danger-subtle text-danger border border-danger-subtle';
            const badgeText = isEarn ? 'Tích điểm' : 'Đổi điểm';
            const sign = isEarn ? '+' : '';
            const pointClass = isEarn ? 'text-success' : 'text-danger';

            return `
                <tr>
                    <td class="ps-4 py-3">
                        <div class="fw-bold text-navy">${item.title}</div>
                        <div class="small text-muted"><i class="bi bi-hash me-1"></i>${item.id} - ${item.note}</div>
                    </td>
                    <td class="text-center text-muted small">${item.date}</td>
                    <td class="text-center">
                        <span class="badge ${badgeClass} px-3 py-2 rounded-pill fw-bold">${badgeText}</span>
                    </td>
                    <td class="text-end pe-4 fw-bold fs-6 ${pointClass}">
                        ${sign}${formatNumber(item.points)}
                    </td>
                </tr>
            `;
        }).join('');
    }

    renderHistory();

    // Event Listeners for Filters
    filterBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            filterBtns.forEach(b => b.classList.remove('active'));
            e.currentTarget.classList.add('active');
            currentFilter = e.currentTarget.dataset.filter;
            renderHistory();
        });
    });

    if (searchInput) {
        searchInput.addEventListener('input', renderHistory);
    }
});
