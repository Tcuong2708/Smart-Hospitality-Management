const API_URL = 'http://localhost:8080/api/services';

document.addEventListener('DOMContentLoaded', () => {
    fetchServices();
});

function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alert-container');
    alertContainer.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            <i class="bi bi-${type === 'success' ? 'check-circle' : 'exclamation-triangle'} me-2"></i>${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}

async function fetchServices() {
    const tbody = document.getElementById('service-table-body');
    const mockServices = [
        { maDV: 1, tenDV: 'Giặt ủi', giaTien: 50000, donVi: 'Bộ' },
        { maDV: 2, tenDV: 'Buffet Sáng', giaTien: 150000, donVi: 'Người' },
        { maDV: 3, tenDV: 'Massage & Spa', giaTien: 500000, donVi: 'Lần' },
        { maDV: 4, tenDV: 'Thuê xe máy', giaTien: 150000, donVi: 'Ngày' },
        { maDV: 5, tenDV: 'Dọn phòng thêm', giaTien: 100000, donVi: 'Lần' }
    ];

    try {
        const services = mockServices;
        
        if (!services || services.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-4 text-muted">
                        <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                        Chưa có dịch vụ nào trong hệ thống.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = services.map(item => {
            const priceFormatted = new Intl.NumberFormat('vi-VN').format(item.giaTien || 0) + ' đ';

            return `
            <tr>
                <td class="text-center fw-bold text-navy">${item.tenDV}</td>
                <td class="text-center fw-bold text-warning">${priceFormatted}</td>
                <td class="text-center">
                    <span class="badge bg-light text-dark border">${item.donVi}</span>
                </td>
                <td class="text-center">
                    <a href="edit.html?id=${item.maDV}" class="btn btn-sm btn-warning text-white shadow-sm mx-1" title="Sửa">
                        <i class="bi bi-pencil"></i>
                    </a>
                    <a href="delete.html?id=${item.maDV}" class="btn btn-sm btn-danger shadow-sm ms-1" title="Xóa">
                        <i class="bi bi-trash"></i>
                    </a>
                </td>
            </tr>
            `;
        }).join('');

    } catch (error) {
        console.error('Error fetching services:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center py-5 text-danger">
                    <i class="bi bi-exclamation-triangle fs-2 d-block mb-2 opacity-50"></i>
                    <p>Lỗi kết nối API. Vui lòng kiểm tra lại Backend.</p>
                </td>
            </tr>
        `;
    }
}
