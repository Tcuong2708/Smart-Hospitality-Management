const API_URL = 'http://localhost:8080/api/categories';

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('create-form');
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const nameInput = document.getElementById('name').value;
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalBtnHtml = submitBtn.innerHTML;
        
        // Disable button during request
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> ĐANG XỬ LÝ...';

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ name: nameInput })
            });

            if (response.ok) {
                showAlert('Thêm loại phòng thành công!', 'success');
                form.reset();
                // Tùy chọn: Chuyển hướng về trang danh sách sau khi thêm thành công
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 1500);
            } else {
                throw new Error('Có lỗi xảy ra khi thêm dữ liệu');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Lỗi kết nối. Không thể thêm loại phòng.', 'danger');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnHtml;
        }
    });
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
