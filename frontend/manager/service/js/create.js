const API_URL = 'http://localhost:8080/api/services';

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('create-form');
    form.addEventListener('submit', handleCreateService);
});

function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alert-container');
    alertContainer.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            <i class="bi bi-${type === 'success' ? 'check-circle' : 'exclamation-triangle'} me-2"></i>${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function handleCreateService(event) {
    event.preventDefault();

    const submitBtn = event.target.querySelector('button[type="submit"]');
    const originalBtnHtml = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...';

    const serviceData = {
        tenDV: document.getElementById('tenDV').value.trim(),
        giaTien: parseFloat(document.getElementById('giaTien').value),
        donVi: document.getElementById('donVi').value.trim()
    };

    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(serviceData)
        });

        if (response.ok) {
            showAlert('Thêm dịch vụ thành công!', 'success');
            event.target.reset();
            setTimeout(() => { window.location.href = 'index.html'; }, 1500);
        } else {
            throw new Error('Lỗi khi lưu dữ liệu');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Có lỗi xảy ra, không thể lưu dịch vụ.', 'danger');
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalBtnHtml;
    }
}
