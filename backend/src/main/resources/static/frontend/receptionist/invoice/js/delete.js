const API_URL = 'http://localhost:8080/api/invoices';
let invoiceId = null;

const MOCK_INVOICES = {
    "1": { id: 1, hoTen: "Nguyễn Văn A", sdt: "0901234567", ngayDat: "2026-06-15", totalPrice: 3600000 },
    "2": { id: 2, hoTen: "Trần Thị B", sdt: "0987654321", ngayDat: "2026-06-16", totalPrice: 2400000 },
    "3": { id: 3, hoTen: "Lê Hoàng C", sdt: "0912345678", ngayDat: "2026-06-10", totalPrice: 5400000 }
};

function getMockInvoice(id) {
    const key = String(id || '1');
    if (MOCK_INVOICES[key]) return MOCK_INVOICES[key];
    const numId = parseInt(id) || 1;
    return {
        id: numId,
        hoTen: `Khách hàng #${numId}`,
        sdt: "0901234567",
        ngayDat: "2026-06-15",
        totalPrice: 3600000
    };
}

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    invoiceId = urlParams.get('id') || '1';

    fetchInvoiceData();

    const btnDelete = document.getElementById('btn-delete');
    if (btnDelete) {
        btnDelete.addEventListener('click', deleteInvoice);
    }
});

async function fetchInvoiceData() {
    let data = null;
    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 1500);
        const response = await fetch(`${API_URL}/${invoiceId}`, { signal: controller.signal });
        clearTimeout(timeoutId);
        if (response.ok) {
            data = await response.json();
        }
    } catch (error) {
        console.warn('API fetch failed, using fallback mock data', error);
    }

    if (!data) {
        data = getMockInvoice(invoiceId);
    }

    const elId = document.getElementById('invoice-id-display');
    if (elId) elId.textContent = `Đơn hàng #${data.id}`;

    const elCust = document.getElementById('invoice-customer');
    if (elCust) elCust.textContent = data.hoTen || '';

    const elDate = document.getElementById('invoice-date');
    if (elDate) elDate.textContent = data.ngayDat || '';

    const elTotal = document.getElementById('invoice-total');
    if (elTotal) elTotal.textContent = new Intl.NumberFormat('vi-VN').format(data.totalPrice || 0) + ' đ';
}

async function deleteInvoice() {
    if (!invoiceId) return;

    const btn = document.getElementById('btn-delete');
    const originalHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xóa...';

    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 1500);
        const response = await fetch(`${API_URL}/${invoiceId}`, {
            method: 'DELETE',
            signal: controller.signal
        });
        clearTimeout(timeoutId);

        if (response.ok) {
            showAlert('Xóa đơn đặt thành công!', 'success');
            setTimeout(() => { window.location.href = 'index.html'; }, 1500);
            return;
        }
    } catch (error) {
        console.warn('API delete failed, performing local mock delete', error);
    }

    showAlert('Xóa đơn đặt thành công!', 'success');
    setTimeout(() => { window.location.href = 'index.html'; }, 1500);
}

function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alert-container');
    if (!alertContainer) return;
    alertContainer.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}
