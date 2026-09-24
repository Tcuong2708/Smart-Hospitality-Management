const API_URL = 'http://localhost:8080/api/categories';
const mockCategories = [
    { id: 'L01', maLoai: 'L01', name: 'Phòng Standard (Tiêu Chuẩn)', phongs: [1, 2] },
    { id: 'L02', maLoai: 'L02', name: 'Phòng Superior (Cao Cấp)', phongs: [3, 4] },
    { id: 'L03', maLoai: 'L03', name: 'Phòng Deluxe (Sang Trọng)', phongs: [5] },
    { id: 'L04', maLoai: 'L04', name: 'Phòng Suite (Thượng Gia)', phongs: [6] }
];

document.addEventListener('DOMContentLoaded', () => {
    renderCategories(mockCategories);

    // Sửa lỗi backdrop che modal
    const modalElement = document.getElementById('categoryModal');
    if (modalElement) {
        document.body.appendChild(modalElement);
    }

    if (modalElement) {
        modalElement.addEventListener('hidden.bs.modal', () => {
            document.getElementById('categoryForm').reset();
            document.getElementById('categoryModalTitle').innerText = 'Thêm Loại Phòng Mới';
        });
    }

    const searchCategory = document.getElementById('searchCategory');
    const thFilterCategoryName = document.getElementById('thFilterCategoryName');

    if (thFilterCategoryName) {
        const uniqueNames = [...new Set(mockCategories.map(c => c.name))];
        thFilterCategoryName.innerHTML = uniqueNames.map((name, idx) => `
            <li>
                <div class="form-check mb-1 ms-1">
                    <input class="form-check-input th-cb-name" type="checkbox" value="${name}" id="th_cat_${idx}">
                    <label class="form-check-label text-truncate" style="max-width: 170px;" title="${name}" for="th_cat_${idx}">${name}</label>
                </div>
            </li>
        `).join('');

        document.querySelectorAll('.th-cb-name').forEach(cb => {
            cb.addEventListener('change', applyFilters);
        });
    }

    function applyFilters() {
        const keyword = (searchCategory ? searchCategory.value : '').toLowerCase().trim();
        const selectedNames = Array.from(document.querySelectorAll('.th-cb-name:checked')).map(cb => cb.value);

        const filtered = mockCategories.filter(c => {
            const matchKeyword = c.name.toLowerCase().includes(keyword) || (c.maLoai || c.id).toLowerCase().includes(keyword);
            const matchName = selectedNames.length === 0 || selectedNames.includes(c.name);
            return matchKeyword && matchName;
        });
        renderCategories(filtered);
    }

    if (searchCategory) {
        searchCategory.addEventListener('input', applyFilters);
    }
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

function renderCategories(categories) {
    const tbody = document.getElementById('category-table-body');

    try {
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
                    <button class="btn btn-sm btn-warning text-white shadow-sm mx-1 btn-edit-category" 
                            data-id="${item.maLoai || item.id}" title="Chỉnh sửa">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-danger shadow-sm ms-1 btn-delete-category" 
                            data-id="${item.maLoai || item.id}" title="Xóa">
                        <i class="bi bi-trash"></i>
                    </button>
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

    // Gắn sự kiện cho nút Sửa
    document.querySelectorAll('.btn-edit-category').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = e.currentTarget.dataset.id;
            const cat = mockCategories.find(c => (c.maLoai || c.id) == id);
            
            if (cat) {
                document.getElementById('categoryModalTitle').innerText = 'Cập Nhật Loại Phòng';
                document.getElementById('form-cat-name').value = cat.name;
                document.getElementById('form-cat-count').value = cat.phongs ? cat.phongs.length : 0;
                
                const modal = new bootstrap.Modal(document.getElementById('categoryModal'));
                modal.show();
            }
        });
    });
}
