const API_URL = 'http://localhost:8080/api/checkout';

document.addEventListener('DOMContentLoaded', () => {
    fetchCheckoutData();
});

function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alert-container');
    alertContainer.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}

async function fetchCheckoutData() {
    const tbody = document.getElementById('checkout-table-body');
    const mockCheckout = [
        { id: 1, hoTen: 'Nguyễn Văn A', maPhong: 101, ngayCheckIn: '2026-06-15', ngayCheckOut: '2026-06-18', ghiChu: 'Khách VIP' },
        { id: 2, hoTen: 'Trần Thị B', maPhong: 202, ngayCheckIn: '2026-06-16', ngayCheckOut: '2026-06-18', ghiChu: 'Đặt qua CHATBOT' }
    ];

    try {
        const invoices = mockCheckout;
        
        if (!invoices || invoices.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center py-5 text-muted">
                        <i class="bi bi-door-open fs-2 d-block mb-2 opacity-50"></i>
                        Hiện tại không có phòng nào đang trong trạng thái lưu trú cần làm thủ tục trả phòng.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = invoices.map(item => {
            const isBot = item.ghiChu && item.ghiChu.includes('CHATBOT');

            return `
            <tr>
                <td class="ps-4 fw-bold text-navy">#${item.id}</td>
                <td class="fw-bold text-secondary">
                    <span>${item.hoTen}</span>
                    ${isBot ? '<span class="badge bg-light text-success border small ms-1" style="font-size: 0.65rem;">Đặt qua Bot</span>' : ''}
                </td>
                <td class="text-center">
                    <span class="badge bg-danger px-3 py-2 rounded-pill fw-bold">Phòng ${item.maPhong}</span>
                </td>
                <td>
                    <small class="d-block text-muted">Từ ngày: <span class="text-dark fw-bold">${item.ngayCheckIn || ''}</span></small>
                    <small class="d-block text-muted">Đến ngày: <span class="text-dark fw-bold">${item.ngayCheckOut || ''}</span></small>
                </td>
                <td class="text-center">
                    <form onsubmit="executeCheckout(event, ${item.id})" class="d-flex gap-2 align-items-center justify-content-center p-2 rounded bg-light border mx-auto" style="max-width: 300px;">
                        <div class="input-group input-group-sm" style="width: 130px;">
                            <input type="number" name="phuThu" class="form-control text-center text-danger fw-bold"
                                   value="0" min="0" step="10000" placeholder="0 đ" required />
                            <span class="input-group-text bg-white text-muted small">đ</span>
                        </div>
                        <button type="submit" class="btn btn-navy-action btn-sm rounded-pill fw-bold text-nowrap py-1">
                            <i class="bi bi-calculator me-1"></i> Trả phòng
                        </button>
                    </form>
                </td>
            </tr>
            `;
        }).join('');

    } catch (error) {
        console.error('Error fetching data:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center py-5 text-danger">
                    <i class="bi bi-exclamation-triangle fs-2 d-block mb-2 opacity-50"></i>
                    <p>Lỗi kết nối API. Hãy đảm bảo API ${API_URL}/invoices đang hoạt động.</p>
                </td>
            </tr>
        `;
    }
}

async function executeCheckout(event, id) {
    event.preventDefault();
    if (!confirm('Xác nhận thu tiền phụ thu, cập nhật ngày ở thực tế và giải phóng phòng về trạng thái TRỐNG?')) {
        return;
    }

    const form = event.target;
    const btn = form.querySelector('button[type="submit"]');
    const originalHtml = btn.innerHTML;

    const phuThu = form.querySelector('input[name="phuThu"]').value;

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xử lý...';

    try {
        const response = await fetch(`${API_URL}/execute`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ maHD: id, phuThu: phuThu })
        });

        if (response.ok) {
            showAlert('Trả phòng và kết toán thành công!', 'success');
            fetchCheckoutData(); // Reload list
        } else {
            throw new Error('Thao tác thất bại');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Có lỗi xảy ra khi Check-out', 'danger');
        btn.disabled = false;
        btn.innerHTML = originalHtml;
    }
}
