const API_URL = 'http://localhost:8080/api/categories';

document.addEventListener('DOMContentLoaded', () => {
    // Lấy ID từ query parameter
    const urlParams = new URLSearchParams(window.location.search);
    const categoryId = urlParams.get('id');

    if (!categoryId) {
        showAlert('Không tìm thấy mã số loại phòng.', 'danger');
        return;
    }

    document.getElementById('display-id').textContent = `#${categoryId}`;
    document.getElementById('maLoai').value = categoryId;

    // Load current data
    fetchCategoryData(categoryId);

    const form = document.getElementById('edit-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const nameInput = document.getElementById('name').value;
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalBtnHtml = submitBtn.innerHTML;
        
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> ĐANG LƯU...';

        try {
            // Có thể dùng PUT hoặc POST tùy backend C# của bạn
            const response = await fetch(`${API_URL}/${categoryId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ 
                    maLoai: categoryId,
                    name: nameInput 
                })
            });

            if (response.ok) {
                showAlert('Cập nhật loại phòng thành công!', 'success');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 1500);
            } else {
                throw new Error('Có lỗi xảy ra khi cập nhật');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Lỗi kết nối. Không thể cập nhật.', 'danger');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnHtml;
        }
    });
});

async function fetchCategoryData(id) {
    try {
        const response = await fetch(`${API_URL}/${id}`);
        if (!response.ok) throw new Error('Không thể tải dữ liệu');
        
        const category = await response.json();
        document.getElementById('name').value = category.name;
    } catch (error) {
        console.error('Error:', error);
        showAlert('Lỗi tải dữ liệu. Vui lòng kiểm tra ID hoặc Backend.', 'danger');
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
