document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const invoiceId = urlParams.get('id');

    if(!invoiceId) {
        window.location.href = 'history.html';
        return;
    }

    const API_INVOICE = `http://localhost:8080/api/booking/invoice/${invoiceId}`;
    
    const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount);
    const formatDate = (dateStr) => {
        if(!dateStr) return '';
        const d = new Date(dateStr);
        return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth()+1).toString().padStart(2, '0')}/${d.getFullYear()}`;
    };

    fetch(API_INVOICE)
        .then(res => res.json())
        .then(data => {
            if(data.error) throw new Error(data.error);

            document.getElementById('loading').classList.add('d-none');
            document.getElementById('detail-content').classList.remove('d-none');

            document.getElementById('dt-invoice-id').textContent = 'Mã giao dịch: #' + data.id;
            
            const statusHtml = data.daThanhToan 
                ? `<span class="badge bg-success py-2 px-3"><i class="bi bi-check2-circle me-1"></i>Đã thanh toán</span>` 
                : `<span class="badge bg-warning text-dark py-2 px-3"><i class="bi bi-hourglass-split me-1"></i>Chờ thanh toán tại quầy</span>`;
            document.getElementById('dt-status').innerHTML = statusHtml;

            document.getElementById('dt-name').textContent = data.hoTen;
            document.getElementById('dt-phone').textContent = data.sdt;
            document.getElementById('dt-checkin').textContent = formatDate(data.ngayCheckIn);
            document.getElementById('dt-checkout').textContent = formatDate(data.ngayCheckOut);
            
            document.getElementById('dt-note').textContent = data.ghiChu || 'Không có ghi chú thêm.';
            document.getElementById('dt-total').textContent = formatCurrency(data.totalPrice) + ' đ';
            document.getElementById('dt-method').textContent = data.phuongThucThanhToan;
        })
        .catch(e => {
            document.getElementById('loading').innerHTML = '<p class="text-danger fw-bold">Không tải được thông tin hóa đơn!</p>';
        });
});
