const API_URL = 'http://localhost:8080/api/services';
let serviceId = null;

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    serviceId = urlParams.get('id');

    if (serviceId) {
        fetchServiceData();
        const form = document.getElementById('edit-form');
        form.addEventListener('submit', handleUpdateService);
    } else {
        showAlert('Không tìm thấy ID dịch vụ.', 'danger');
        document.getElementById('loading-spinner').style.display = 'none';
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
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function fetchServiceData() {
    try {
        const response = await fetch(`${API_URL}/${serviceId}`);
        if (!response.ok) throw new Error('Không thể tải dữ liệu dịch vụ');
        
        const data = await response.json();
        
        document.getElementById('loading-spinner').style.display = 'none';
        document.getElementById('edit-content').style.display = 'flex';

        document.getElementById('display-id').textContent = `ID: #${data.maDV}`;
        document.getElementById('maDV').value = data.maDV;
        document.getElementById('tenDV').value = data.tenDV;
        document.getElementById('giaTien').value = data.giaTien;
        document.getElementById('donVi').value = data.donVi;

    } catch (error) {
        console.error('Error:', error);
        document.getElementById('loading-spinner').style.display = 'none';
        showAlert('Lỗi khi tải thông tin dịch vụ.', 'danger');
    }
}

async function handleUpdateService(event) {
    event.preventDefault();

    const submitBtn = event.target.querySelector('button[type="submit"]');
    const originalBtnHtml = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...';

    const serviceData = {
        maDV: document.getElementById('maDV').value,
        tenDV: document.getElementById('tenDV').value.trim(),
        giaTien: parseFloat(document.getElementById('giaTien').value),
        donVi: document.getElementById('donVi').value.trim()
    };

    try {
        const response = await fetch(`${API_URL}/${serviceId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(serviceData)
        });

        if (response.ok) {
            showAlert('Cập nhật dịch vụ thành công!', 'success');
            setTimeout(() => { window.location.href = 'index.html'; }, 1500);
        } else {
            throw new Error('Cập nhật thất bại');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Có lỗi xảy ra, không thể cập nhật dịch vụ.', 'danger');
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalBtnHtml;
    }
}
