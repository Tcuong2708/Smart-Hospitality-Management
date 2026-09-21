const API_URL = 'http://localhost:8080/api/invoices';

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const invoiceId = urlParams.get('id');

    if (invoiceId) {
        fetchInvoicePrintData(invoiceId);
    } else {
        alert('Không tìm thấy ID hóa đơn.');
        document.getElementById('loading-spinner').style.display = 'none';
    }
});

async function fetchInvoicePrintData(id) {
    try {
        const response = await fetch(`${API_URL}/${id}`);
        if (!response.ok) throw new Error('Không thể tải dữ liệu hóa đơn');
        
        const invoice = await response.json();
        
        document.getElementById('loading-spinner').style.display = 'none';
        document.getElementById('controls').style.display = 'block';
        document.getElementById('invoice-content').style.display = 'block';

        const now = new Date();
        document.getElementById('print-date').textContent = `Ngày xuất: ${now.toLocaleDateString('vi-VN')} ${now.toLocaleTimeString('vi-VN')}`;
        
        document.getElementById('invoice-id').textContent = `#HD-${invoice.id}`;
        document.getElementById('customer-name').textContent = invoice.hoTen;
        document.getElementById('customer-phone').textContent = `SĐT: ${invoice.sdt || ''}`;
        document.getElementById('customer-address').textContent = invoice.diaChi ? `Địa chỉ: ${invoice.diaChi}` : 'Địa chỉ: Chưa cập nhật';

        document.getElementById('checkin-date').textContent = invoice.ngayCheckIn;
        document.getElementById('checkout-date').textContent = invoice.ngayCheckOut;

        // Calculate nights
        let nights = 1;
        if (invoice.ngayCheckIn && invoice.ngayCheckOut) {
            const checkin = new Date(invoice.ngayCheckIn);
            const checkout = new Date(invoice.ngayCheckOut);
            const diffTime = Math.abs(checkout - checkin);
            nights = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            if (nights === 0) nights = 1;
        }
        document.getElementById('nights').textContent = `(${nights} đêm)`;

        const roomName = invoice.phong ? invoice.phong.name : `Phòng khách lẻ`;
        document.getElementById('room-name').textContent = roomName;
        document.getElementById('room-nights').textContent = nights;
        
        const roomPrice = invoice.phong ? new Intl.NumberFormat('vi-VN').format(invoice.phong.price) + ' đ' : '-';
        document.getElementById('room-price').textContent = roomPrice;
        
        const totalPrice = new Intl.NumberFormat('vi-VN').format(invoice.totalPrice || 0) + ' đ';
        document.getElementById('room-total').textContent = totalPrice;
        document.getElementById('invoice-total').textContent = totalPrice;

        // Automatically trigger print dialog after small delay to render
        setTimeout(() => {
            window.print();
        }, 500);

    } catch (error) {
        console.error('Error:', error);
        document.getElementById('loading-spinner').style.display = 'none';
        alert('Lỗi tải hóa đơn từ máy chủ.');
    }
}
