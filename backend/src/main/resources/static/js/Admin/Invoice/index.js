document.addEventListener('DOMContentLoaded', () => {
    'use strict';

    window.ADM_PAGE_MODULE = 'invoice';

    /* ===== Modal tạo hóa đơn ===== */
    const modalElement = document.getElementById('createInvoiceModal');
    if (modalElement) {
        modalElement.addEventListener('adm:modal-closed', () => {
            const form = document.getElementById('createInvoiceForm');
            if (form) form.reset();
        });
    }

    const btnSubmit = document.getElementById('btn-submit-invoice');
    if (btnSubmit) {
        btnSubmit.addEventListener('click', () => {
            const form = document.getElementById('createInvoiceForm');
            if (!form) return;
            if (!form.checkValidity()) {
                form.reportValidity();
                return;
            }
            if (typeof toast === 'function') toast('Tạo đơn đặt phòng thành công');
            if (typeof AdmModal !== 'undefined') AdmModal.close('createInvoiceModal');
        });
    }

    fetchInvoices();
});

function showAlert(message, type = 'success') {
    if (typeof toast === 'function') {
        toast(message, type === 'error' ? 'danger' : type);
    }
}

async function fetchInvoices() {
    const tbody = document.getElementById('invoice-table-body');
    if (!tbody) return;

    const invoices = Array.isArray(window.mockInvoices) ? window.mockInvoices : [];

    try {
        if (!invoices.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7">
                        <div class="adm-empty">
                            <i class="bi bi-clipboard-x"></i>
                            <h4>Chưa có đơn đặt phòng</h4>
                            <p>Đơn đặt phòng sẽ xuất hiện sau khi khách đặt phòng hoặc thanh toán.</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        const today = new Date();

        const formatDate = (dateInput) => {
            if (!dateInput) return '—';
            const d = new Date(dateInput);
            if (Number.isNaN(d.getTime())) return dateInput;
            const day = String(d.getDate()).padStart(2, '0');
            const month = String(d.getMonth() + 1).padStart(2, '0');
            return `${day}/${month}/${d.getFullYear()}`;
        };

        const money = (v) => new Intl.NumberFormat('vi-VN').format(v || 0) + ' đ';

        tbody.innerHTML = invoices.map((item) => {
            const checkOutDate = item.ngayCheckOut ? new Date(item.ngayCheckOut) : null;
            const isCheckedOut = checkOutDate && checkOutDate < today;

            const statusBadge = isCheckedOut
                ? '<span class="adm-badge adm-badge-neutral"><span class="dot"></span>Đã trả phòng</span>'
                : '<span class="adm-badge adm-badge-success"><span class="dot"></span>Đang lưu trú</span>';

            return `
            <tr>
                <td class="cell-id text-center">#${item.id || '—'}</td>

                <td>
                    <strong style="display:block;font-size:13.5px;font-weight:700;color:var(--navy);line-height:1.35;">
                        ${escapeHtml(item.hoTen || 'Khách')}
                    </strong>
                    <small class="text-muted" style="display:inline-flex;align-items:center;gap:4px;font-size:11.5px;margin-top:2px;">
                        <i class="bi bi-telephone-fill" style="font-size:10px;color:var(--gold-hover);"></i>
                        ${escapeHtml(item.sdt || '')}
                    </small>
                </td>

                <td class="text-center text-muted">${formatDate(item.ngayDat)}</td>

                <td class="text-center">
                    <div class="adm-date-pair" style="display:inline-flex;">
                        <span class="adm-date-line in">
                            <i class="bi bi-box-arrow-in-right"></i>
                            <strong>${formatDate(item.ngayCheckIn)}</strong>
                        </span>
                        <span class="adm-date-line out">
                            <i class="bi bi-box-arrow-left"></i>
                            <strong>${formatDate(item.ngayCheckOut)}</strong>
                        </span>
                    </div>
                </td>

                <td class="text-right cell-price">${money(item.totalPrice)}</td>

                <td class="text-center">${statusBadge}</td>

                <td class="text-center">
                    <div class="d-inline-flex justify-content-center" role="group">
                        <a href="/receptionist/invoices/details/${item.id}"
                           class="btn btn-action btn-view" title="Xem chi tiết">
                            <i class="bi bi-eye"></i>
                        </a>
                        <a href="/receptionist/invoices/print/${item.id}" target="_blank"
                           class="btn btn-action btn-state-active" title="In hóa đơn">
                            <i class="bi bi-printer"></i>
                        </a>
                        <a href="/receptionist/invoices/delete/${item.id}"
                           class="btn btn-action btn-delete" title="Xóa vĩnh viễn"
                           onclick="return confirm('Bạn có chắc chắn muốn xóa?');">
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
                <td colspan="7">
                    <div class="adm-empty">
                        <i class="bi bi-exclamation-triangle" style="color:var(--red);"></i>
                        <h4>Không tải được danh sách hóa đơn</h4>
                        <p>Vui lòng kiểm tra kết nối và thử lại.</p>
                    </div>
                </td>
            </tr>
        `;
    }
}

function escapeHtml(s) {
    if (s == null) return '';
    const d = document.createElement('div');
    d.textContent = String(s);
    return d.innerHTML;
}