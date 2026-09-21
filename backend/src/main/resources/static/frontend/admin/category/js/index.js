const API_URL = 'http://localhost:8080/api/categories';

document.addEventListener('DOMContentLoaded', () => {
    fetchCategories();
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

async function fetchCategories() {
    const tbody = document.getElementById('category-table-body');
    const mockCategories = [
        { id: 'L01', maLoai: 'L01', name: 'Phòng Standard (Tiêu Chuẩn)', phongs: [1, 2] },
        { id: 'L02', maLoai: 'L02', name: 'Phòng Superior (Cao Cấp)', phongs: [3, 4] },
        { id: 'L03', maLoai: 'L03', name: 'Phòng Deluxe (Sang Trọng)', phongs: [5] },
        { id: 'L04', maLoai: 'L04', name: 'Phòng Suite (Thượng Gia)', phongs: [6] }
    ];

    try {
        const categories = mockCategories;
        
        if (!categories || categories.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-5 text-muted">
                        <i class="bi bi-inbox fs-1 d-block mb-2 opacity-25"></i>
                        <p>Chưa có loại phòng nào trong hệ thống.</p>
                        <a href="create.html" class="btn btn-sm btn-outline-secondary mt-2">Thêm ngay</a>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = categories.map(item => `
            <tr>
                <td class="text-center fw-bold text-muted">#${item.maLoai || item.id}</td>
                <td class="text-center fw-bold" style="color: var(--navy-color);">
                    <i class="bi bi-tag-fill me-2" style="color: var(--gold-color); opacity: 0.7;"></i>
                    ${item.name}
                </td>
                <td class="text-center">
                        <span class="badge badge-count rounded-pill px-3 py-2">
                            ${item.phongs ? item.phongs.length : 0} phòng
                        </span>
                </td>
                <td class="text-center">
                    <a href="edit.html?id=${item.maLoai || item.id}"
                       class="btn btn-sm btn-warning text-white shadow-sm mx-1"
                       title="Chỉnh sửa">
                        <i class="bi bi-pencil"></i>
                    </a>
                    <a href="delete.html?id=${item.maLoai || item.id}"
                       class="btn btn-sm btn-danger shadow-sm ms-1" title="Xóa">
                        <i class="bi bi-trash"></i>
                    </a>
                </td>
            </tr>
        `).join('');

    } catch (error) {
        console.error('Error fetching categories:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center py-5 text-danger">
                    <i class="bi bi-exclamation-triangle fs-1 d-block mb-2 opacity-50"></i>
                    <p>Lỗi kết nối đến máy chủ. Vui lòng kiểm tra lại Backend.</p>
                </td>
            </tr>
        `;
    }
}
