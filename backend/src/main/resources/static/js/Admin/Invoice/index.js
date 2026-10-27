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
    const mockInvoices = Array.isArray(window.mockInvoices) ? window.mockInvoices : [];

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
            const ngayCheckOutDate = item.ngayCheckOut ? new Date(item.ngayCheckOut) : null;
            const formatDate = (dateInput) => {
                if (!dateInput) return '';
                const d = new Date(dateInput);
                if (Number.isNaN(d.getTime())) return dateInput;
                const day = String(d.getDate()).padStart(2, '0');
                const month = String(d.getMonth() + 1).padStart(2, '0');
                return `${day}-${month}-${d.getFullYear()}`;
            };
            
            let statusHtml = '';
            if (ngayCheckOutDate && ngayCheckOutDate < today) {
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
                <td class="text-center text-muted small">${formatDate(item.ngayDat)}</td>
                <td class="text-center">
                    <div class="d-flex flex-column align-items-center small">
                        <span><i class="bi bi-box-arrow-in-right text-success me-1"></i> ${formatDate(item.ngayCheckIn)}</span>
                        <span><i class="bi bi-box-arrow-left text-danger me-1"></i> ${formatDate(item.ngayCheckOut)}</span>
                    </div>
                </td>
                <td class="text-center fw-bold fs-6 text-gold">${priceFormatted}</td>
                <td class="text-center">${statusHtml}</td>
                <td class="text-center">
                    <div class="d-inline-flex justify-content-center" role="group">
                        <a href="/receptionist/invoices/details/${item.id}" class="btn btn-action btn-view" title="Xem chi tiết">
                            <i class="bi bi-eye"></i>
                        </a>
                        <a href="/receptionist/invoices/print/${item.id}" target="_blank" class="btn btn-action btn-state-active" title="In hóa đơn">
                            <i class="bi bi-printer"></i>
                        </a>
                        <a href="/receptionist/invoices/delete/${item.id}" class="btn btn-action btn-delete" title="Xóa vĩnh viễn" onclick="return confirm('Bạn có chắc chắn muốn xóa?');">
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
