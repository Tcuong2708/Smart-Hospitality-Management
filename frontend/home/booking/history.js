let mockHistory = [
    { id: 101, ngayDat: '2026-06-10', ngayCheckIn: '2026-06-15', ngayCheckOut: '2026-06-18', totalPrice: 3600000, daThanhToan: true, trangThai: 'Chờ nhận phòng' },
    { id: 102, ngayDat: '2026-06-12', ngayCheckIn: '2026-06-20', ngayCheckOut: '2026-06-22', totalPrice: 2400000, daThanhToan: false, trangThai: 'Chờ nhận phòng' },
    { id: 103, ngayDat: '2026-05-01', ngayCheckIn: '2026-05-05', ngayCheckOut: '2026-05-08', totalPrice: 1500000, daThanhToan: true, trangThai: 'Đã nhận phòng' }
];

document.addEventListener('DOMContentLoaded', () => {
    const API_HISTORY = 'http://localhost:8080/api/booking/history';
    
    // Đưa modal ra ngoài body để tránh lỗi backdrop
    const modalElement = document.getElementById('cancelWarningModal');
    if(modalElement) document.body.appendChild(modalElement);
    
    renderHistory();
});

const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount);
const formatDate = (dateStr) => {
    if(!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth()+1).toString().padStart(2, '0')}/${d.getFullYear()}`;
};

function renderHistory() {
    const tbody = document.getElementById('history-list');

    try {
        const data = mockHistory;
        if(!data || data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-5 text-muted fw-bold">Bạn chưa có giao dịch đặt phòng nào.</td></tr>`;
            return;
        }

        let html = '';
        data.forEach(item => {
            let statusBadge = item.daThanhToan 
                ? `<span class="badge bg-success bg-opacity-10 text-success border border-success"><i class="bi bi-check2-circle me-1"></i>Đã thanh toán</span>` 
                : `<span class="badge bg-warning bg-opacity-10 text-warning border border-warning text-dark"><i class="bi bi-hourglass-split me-1"></i>Chờ thanh toán</span>`;

            if (item.trangThai === 'Đã hủy') {
                statusBadge = `<span class="badge bg-danger bg-opacity-10 text-danger border border-danger"><i class="bi bi-x-circle me-1"></i>Đã hủy</span>`;
            }

            // Logic Disable Cancel (UC03)
            let isDisableCancel = item.trangThai === 'Đã nhận phòng' || item.trangThai === 'Không đến' || item.trangThai === 'Đã hủy';
            let cancelBtn = isDisableCancel 
                ? `<button class="btn btn-sm btn-outline-secondary shadow-sm" disabled title="Không thể hủy"><i class="bi bi-x-circle"></i> Hủy</button>`
                : `<button class="btn btn-sm btn-outline-danger btn-cancel shadow-sm fw-bold" data-id="${item.id}" data-price="${item.totalPrice}"><i class="bi bi-x-circle"></i> Hủy</button>`;

            html += `
                <tr>
                    <td class="px-4 fw-bold text-navy">#${item.id}</td>
                    <td class="px-4 text-muted small">${formatDate(item.ngayDat)}</td>
                    <td class="px-4 text-muted small">${formatDate(item.ngayCheckIn)} - ${formatDate(item.ngayCheckOut)}</td>
                    <td class="px-4 text-end fw-bold text-danger">${formatCurrency(item.totalPrice)} đ</td>
                    <td class="px-4 text-center">
                        ${statusBadge}
                        <div class="small mt-1 text-muted fw-medium">${item.trangThai}</div>
                    </td>
                    <td class="px-4 text-center">
                        <a href="historydetail.html?id=${item.id}" class="btn btn-sm btn-navy shadow-sm me-1"><i class="bi bi-eye"></i> Chi tiết</a>
                        ${cancelBtn}
                    </td>
                </tr>
            `;
        });
        tbody.innerHTML = html;
        
        // Setup Modal Logic
        let cancelModalInstance = bootstrap.Modal.getInstance(document.getElementById('cancelWarningModal'));
        if (!cancelModalInstance) {
            cancelModalInstance = new bootstrap.Modal(document.getElementById('cancelWarningModal'));
        }
        
        let currentCancelId = null;

        document.querySelectorAll('.btn-cancel').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = parseInt(e.currentTarget.dataset.id);
                const price = parseInt(e.currentTarget.dataset.price);
                currentCancelId = id;
                
                document.getElementById('cancel-invoice-id').textContent = '#' + id;
                
                // Giả lập logic tính phạt (ví dụ phạt 50% tiền)
                const penalty = price * 0.5;
                document.getElementById('cancel-penalty-fee').textContent = formatCurrency(penalty) + ' VNĐ';

                cancelModalInstance.show();
            });
        });

        // Hủy bỏ event cũ để tránh bị duplicate khi render lại
        const btnConfirm = document.getElementById('btn-confirm-cancel');
        const newBtnConfirm = btnConfirm.cloneNode(true);
        btnConfirm.parentNode.replaceChild(newBtnConfirm, btnConfirm);

        newBtnConfirm.addEventListener('click', () => {
            if(cancelModalInstance) cancelModalInstance.hide();
            
            // Cập nhật trạng thái
            if(currentCancelId) {
                const idx = mockHistory.findIndex(h => h.id === currentCancelId);
                if(idx > -1) {
                    mockHistory[idx].trangThai = 'Đã hủy';
                    renderHistory();
                    alert('Hủy phòng thành công. Hệ thống đã ghi nhận phí phạt tương ứng.');
                }
            }
        });

    } catch(e) {
        document.getElementById('history-list').innerHTML = `<tr><td colspan="6" class="text-center py-5 text-danger fw-bold">Lỗi hiển thị dữ liệu!</td></tr>`;
    }
}
