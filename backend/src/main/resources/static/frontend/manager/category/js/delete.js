const API_URL = 'http://localhost:8080/api/categories';
let categoryId = null;

document.addEventListener('DOMContentLoaded', () => {
    // Lấy ID từ query parameter
    const urlParams = new URLSearchParams(window.location.search);
    categoryId = urlParams.get('id');

    if (!categoryId) {
        showAlert('Không tìm thấy mã số loại phòng.', 'danger');
        document.getElementById('btn-delete').disabled = true;
        return;
    }

    // Load thông tin để hiển thị cho người dùng xác nhận
    fetchCategoryData(categoryId);

    // Bắt sự kiện click nút xóa
    const deleteBtn = document.getElementById('btn-delete');
    deleteBtn.addEventListener('click', async () => {
        const originalHtml = deleteBtn.innerHTML;
        deleteBtn.disabled = true;
        deleteBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> ĐANG XÓA...';

        try {
            const response = await fetch(`${API_URL}/${categoryId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                showAlert('Đã xóa danh mục thành công!', 'success');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 1500);
            } else {
                throw new Error('Xóa thất bại');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Lỗi: Không thể xóa loại phòng. Có thể do ràng buộc dữ liệu.', 'danger');
            deleteBtn.disabled = false;
            deleteBtn.innerHTML = originalHtml;
        }
    });
});

async function fetchCategoryData(id) {
    try {
        const response = await fetch(`${API_URL}/${id}`);
        if (!response.ok) throw new Error('Không thể tải dữ liệu');
        
        const category = await response.json();
        document.getElementById('category-id').textContent = `Mã số: #${id}`;
        document.getElementById('category-name').textContent = category.name;
    } catch (error) {
        console.error('Error:', error);
        showAlert('Lỗi tải dữ liệu.', 'danger');
        document.getElementById('category-name').textContent = 'Lỗi dữ liệu';
        document.getElementById('category-id').textContent = `Mã số: #${id}`;
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
