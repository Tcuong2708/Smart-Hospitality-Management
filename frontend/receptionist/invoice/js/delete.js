const API_URL = 'http://localhost:8080/api/invoices';
let invoiceId = null;

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    invoiceId = urlParams.get('id');

    if (invoiceId) {
        fetchInvoiceData();
    } else {
        showAlert('Không tìm thấy ID hóa đơn.', 'danger');
    }

    document.getElementById('btn-delete').addEventListener('click', deleteInvoice);
});

async function fetchInvoiceData() {
    try {
        const response = await fetch(`${API_URL}/${invoiceId}`);
        if (!response.ok) throw new Error('Không thể lấy dữ liệu hóa đơn');
        
        const data = await response.json();
        
        document.getElementById('invoice-id-display').textContent = `Đơn hàng #${data.id}`;
        document.getElementById('invoice-customer').textContent = data.hoTen;
        document.getElementById('invoice-date').textContent = data.ngayDat || '';
        document.getElementById('invoice-total').textContent = new Intl.NumberFormat('vi-VN').format(data.totalPrice || 0) + ' đ';

    } catch (error) {
        console.error('Error:', error);
        showAlert('Lỗi khi tải thông tin đơn hàng.', 'danger');
    }
}

async function deleteInvoice() {
    if (!invoiceId) return;
    
    const btn = document.getElementById('btn-delete');
    const originalHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xóa...';

    try {
        const response = await fetch(`${API_URL}/${invoiceId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showAlert('Xóa đơn đặt thành công!', 'success');
            setTimeout(() => { window.location.href = 'index.html'; }, 1500);
        } else {
            throw new Error('Xóa thất bại');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Không thể xóa đơn đặt này. Có thể nó đang liên kết với dữ liệu khác.', 'danger');
        btn.disabled = false;
        btn.innerHTML = originalHtml;
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
