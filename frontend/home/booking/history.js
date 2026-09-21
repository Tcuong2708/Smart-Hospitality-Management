document.addEventListener('DOMContentLoaded', () => {
    const API_HISTORY = 'http://localhost:8080/api/booking/history';
    const tbody = document.getElementById('history-list');

    const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount);
    const formatDate = (dateStr) => {
        if(!dateStr) return '';
        const d = new Date(dateStr);
        return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth()+1).toString().padStart(2, '0')}/${d.getFullYear()}`;
    };

    const mockHistory = [
        { id: 101, ngayDat: '2026-06-10', ngayCheckIn: '2026-06-15', ngayCheckOut: '2026-06-18', totalPrice: 3600000, daThanhToan: true },
        { id: 102, ngayDat: '2026-06-12', ngayCheckIn: '2026-06-20', ngayCheckOut: '2026-06-22', totalPrice: 2400000, daThanhToan: false }
    ];

    try {
        const data = mockHistory;
        if(!data || data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-5 text-muted fw-bold">Bạn chưa có giao dịch đặt phòng nào.</td></tr>`;
            return;
        }

        let html = '';
        data.forEach(item => {
            const statusBadge = item.daThanhToan 
                ? `<span class="badge bg-success bg-opacity-10 text-success border border-success"><i class="bi bi-check2-circle me-1"></i>Đã thanh toán</span>` 
                : `<span class="badge bg-warning bg-opacity-10 text-warning border border-warning text-dark"><i class="bi bi-hourglass-split me-1"></i>Chờ thanh toán</span>`;

            html += `
                <tr>
                    <td class="px-4 fw-bold text-navy">#${item.id}</td>
                    <td class="px-4 text-muted small">${formatDate(item.ngayDat)}</td>
                    <td class="px-4 text-muted small">${formatDate(item.ngayCheckIn)} - ${formatDate(item.ngayCheckOut)}</td>
                    <td class="px-4 text-end fw-bold text-danger">${formatCurrency(item.totalPrice)} đ</td>
                    <td class="px-4 text-center">${statusBadge}</td>
                    <td class="px-4 text-center">
                        <a href="historydetail.html?id=${item.id}" class="btn btn-sm btn-outline-secondary">Chi tiết</a>
                    </td>
                </tr>
            `;
        });
        tbody.innerHTML = html;
    } catch(e) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-5 text-danger fw-bold">Lỗi hiển thị dữ liệu!</td></tr>`;
    }
});
