const API_URL = 'http://localhost:8080/api/invoices';

document.addEventListener('DOMContentLoaded', () => {
    // Sửa lỗi backdrop che modal
    const modalElement = document.getElementById('createInvoiceModal');
    if (modalElement) {
        document.body.appendChild(modalElement);
        
        // Reset form khi đóng
        modalElement.addEventListener('hidden.bs.modal', () => {
            document.getElementById('createInvoiceForm').reset();
        });
    }

    const btnSubmit = document.getElementById('btn-submit-invoice');
    if(btnSubmit) {
        btnSubmit.addEventListener('click', () => {
            const form = document.getElementById('createInvoiceForm');
            if(form.checkValidity()) {
                alert('Tạo đơn đặt phòng thành công!');
                bootstrap.Modal.getInstance(modalElement).hide();
            } else {
                form.reportValidity();
            }
        });
    }

    fetchInvoices();
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

async function fetchInvoices() {
    const tbody = document.getElementById('invoice-table-body');
    const mockInvoices = [
        { id: 1, hoTen: 'Nguyễn Văn A', sdt: '0901234567', ngayDat: '2026-06-15', ngayCheckIn: '2026-06-18', ngayCheckOut: '2026-06-20', totalPrice: 3600000 },
        { id: 2, hoTen: 'Trần Thị B', sdt: '0987654321', ngayDat: '2026-06-16', ngayCheckIn: '2026-06-17', ngayCheckOut: '2026-06-19', totalPrice: 2400000 },
        { id: 3, hoTen: 'Lê Hoàng C', sdt: '0912345678', ngayDat: '2026-06-10', ngayCheckIn: '2026-06-12', ngayCheckOut: '2026-06-14', totalPrice: 5400000 }
    ];

    try {
        const invoices = mockInvoices;
        
        if (!invoices || invoices.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-5">
                        <i class="bi bi-clipboard-x text-muted" style="font-size: 2rem;"></i>
                        <p class="text-muted mt-2 mb-0">Chưa có dữ liệu đơn đặt phòng nào.</p>
                    </td>
                </tr>
            `;
            return;
        }

        const today = new Date();

        tbody.innerHTML = invoices.map(item => {
            const priceFormatted = new Intl.NumberFormat('vi-VN').format(item.totalPrice || 0) + ' đ';
            const ngayCheckOutDate = new Date(item.ngayCheckOut);
            
            let statusHtml = '';
            if (ngayCheckOutDate < today) {
                statusHtml = '<span class="status-badge status-completed">Đã trả phòng</span>';
            } else {
                statusHtml = '<span class="status-badge status-active">Đang lưu trú</span>';
            }

            return `
            <tr>
                <td class="text-center fw-bold">#${item.id}</td>
                <td class="text-center">
                    <div class="d-flex flex-column align-items-center">
                        <span class="fw-bold text-navy">${item.hoTen}</span>
                        <small class="text-muted">
                            <i class="bi bi-telephone-fill me-1" style="font-size: 0.7rem"></i>
                            ${item.sdt || ''}
                        </small>
                    </div>
                </td>
                <td class="text-center text-muted small">${item.ngayDat || ''}</td>
                <td class="text-center">
                    <div class="d-flex flex-column align-items-center small">
                        <span><i class="bi bi-box-arrow-in-right text-success me-1"></i> ${item.ngayCheckIn || ''}</span>
                        <span><i class="bi bi-box-arrow-left text-danger me-1"></i> ${item.ngayCheckOut || ''}</span>
                    </div>
                </td>
                <td class="text-center fw-bold fs-6 text-gold">${priceFormatted}</td>
                <td class="text-center">${statusHtml}</td>
                <td class="text-center">
                    <div class="btn-group" role="group">
                        <a href="details.html?id=${item.id}" class="btn btn-sm btn-info text-white shadow-sm" title="Xem chi tiết">
                            <i class="bi bi-eye"></i>
                        </a>
                        <a href="print.html?id=${item.id}" target="_blank" class="btn btn-sm btn-success text-white shadow-sm mx-1" title="In hóa đơn">
                            <i class="bi bi-printer"></i>
                        </a>
                        <a href="delete.html?id=${item.id}" class="btn btn-sm btn-danger shadow-sm ms-1" title="Xóa vĩnh viễn">
                            <i class="bi bi-trash"></i>
                        </a>
                    </div>
                </td>
            </tr>
            `;
        }).join('');

    } catch (error) {
        console.error('Error fetching invoices:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5 text-danger">
                    <i class="bi bi-exclamation-triangle fs-2 d-block mb-2 opacity-50"></i>
                    <p>Lỗi kết nối API. Vui lòng kiểm tra lại Backend.</p>
                </td>
            </tr>
        `;
    }
}
