const API_URL = 'http://localhost:8080/api/services';
const mockServices = [
    { maDV: 1, tenDV: 'Giặt ủi', giaTien: 50000, donVi: 'Bộ' },
    { maDV: 2, tenDV: 'Buffet Sáng', giaTien: 150000, donVi: 'Người' },
    { maDV: 3, tenDV: 'Massage & Spa', giaTien: 500000, donVi: 'Lần' },
    { maDV: 4, tenDV: 'Thuê xe máy', giaTien: 150000, donVi: 'Ngày' },
    { maDV: 5, tenDV: 'Dọn phòng thêm', giaTien: 100000, donVi: 'Lần' }
];

document.addEventListener('DOMContentLoaded', () => {
    renderServices(mockServices);

    // Sửa lỗi backdrop che modal
    const modalElement = document.getElementById('serviceModal');
    if (modalElement) {
        document.body.appendChild(modalElement);
    }

    if (modalElement) {
        modalElement.addEventListener('hidden.bs.modal', () => {
            document.getElementById('serviceForm').reset();
            document.getElementById('serviceModalTitle').innerText = 'Thêm Dịch Vụ Mới';
        });
    }

    const searchService = document.getElementById('searchService');
    const thFilterServiceName = document.getElementById('thFilterServiceName');

    if (thFilterServiceName) {
        const uniqueNames = [...new Set(mockServices.map(s => s.tenDV))];
        thFilterServiceName.innerHTML = uniqueNames.map((name, idx) => `
            <li>
                <div class="form-check mb-1 ms-1">
                    <input class="form-check-input th-cb-name" type="checkbox" value="${name}" id="th_srv_${idx}">
                    <label class="form-check-label text-truncate" style="max-width: 170px;" title="${name}" for="th_srv_${idx}">${name}</label>
                </div>
            </li>
        `).join('');

        document.querySelectorAll('.th-cb-name').forEach(cb => {
            cb.addEventListener('change', applyFilters);
        });
    }

    function applyFilters() {
        const keyword = (searchService ? searchService.value : '').toLowerCase().trim();
        const selectedNames = Array.from(document.querySelectorAll('.th-cb-name:checked')).map(cb => cb.value);

        const filtered = mockServices.filter(s => {
            const matchKeyword = s.tenDV.toLowerCase().includes(keyword);
            const matchName = selectedNames.length === 0 || selectedNames.includes(s.tenDV);
            return matchKeyword && matchName;
        });
        renderServices(filtered);
    }

    if (searchService) {
        searchService.addEventListener('input', applyFilters);
    }
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

function renderServices(services) {
    const tbody = document.getElementById('service-table-body');

    try {
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
                    <button class="btn btn-sm btn-warning text-white shadow-sm mx-1 btn-edit-service" data-id="${item.maDV}" title="Sửa">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-danger shadow-sm ms-1 btn-delete-service" data-id="${item.maDV}" title="Xóa">
                        <i class="bi bi-trash"></i>
                    </button>
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

    // Gắn sự kiện cho nút Sửa
    document.querySelectorAll('.btn-edit-service').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = e.currentTarget.dataset.id;
            const srv = mockServices.find(s => s.maDV == id);
            
            if (srv) {
                document.getElementById('serviceModalTitle').innerText = 'Cập Nhật Dịch Vụ';
                document.getElementById('form-srv-name').value = srv.tenDV;
                document.getElementById('form-srv-price').value = srv.giaTien;
                document.getElementById('form-srv-unit').value = srv.donVi;
                
                const modal = new bootstrap.Modal(document.getElementById('serviceModal'));
                modal.show();
            }
        });
    });
}
