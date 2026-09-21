const API_URL = 'http://localhost:8080/api/invoices';

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const invoiceId = urlParams.get('id');

    if (invoiceId) {
        fetchInvoiceDetails(invoiceId);
    } else {
        showAlert('Không tìm thấy ID hóa đơn.', 'danger');
        document.getElementById('loading-spinner').style.display = 'none';
    }
});

async function fetchInvoiceDetails(id) {
    try {
        const response = await fetch(`${API_URL}/${id}`);
        if (!response.ok) throw new Error('Không thể tải dữ liệu hóa đơn');
        
        const invoice = await response.json();
        
        document.getElementById('loading-spinner').style.display = 'none';
        document.getElementById('details-content').style.display = 'flex';

        document.getElementById('invoice-id').textContent = invoice.id;
        document.getElementById('invoice-date').textContent = invoice.ngayDat || '';
        document.getElementById('customer-name').textContent = invoice.hoTen;
        document.getElementById('customer-phone').textContent = invoice.sdt || '';
        document.getElementById('customer-address').textContent = invoice.diaChi || 'Chưa cập nhật';

        const roomName = invoice.phong ? invoice.phong.name : `Mã phòng: #${invoice.maPhong}`;
        document.getElementById('room-name').textContent = roomName;
        document.getElementById('time-range').textContent = `${invoice.ngayCheckIn} đến ${invoice.ngayCheckOut}`;
        
        const roomPrice = invoice.phong ? new Intl.NumberFormat('vi-VN').format(invoice.phong.price) + ' đ' : '-';
        document.getElementById('room-price').textContent = roomPrice;
        
        const totalPrice = new Intl.NumberFormat('vi-VN').format(invoice.totalPrice || 0) + ' đ';
        document.getElementById('room-total').textContent = totalPrice;
        document.getElementById('invoice-total').textContent = totalPrice;

    } catch (error) {
        console.error('Error:', error);
        document.getElementById('loading-spinner').style.display = 'none';
        showAlert('Có lỗi xảy ra khi tải chi tiết đơn hàng.', 'danger');
    }
}

function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alert-container');
    alertContainer.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}
