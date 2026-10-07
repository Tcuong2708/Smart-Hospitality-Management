const API_URL = 'http://localhost:8080/api/services';
let serviceId = null;

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    serviceId = urlParams.get('id');

    if (serviceId) {
        fetchServiceData();
        document.getElementById('btn-delete').addEventListener('click', deleteService);
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
        document.getElementById('delete-content').style.display = 'flex';

        document.getElementById('display-id').textContent = `#${data.maDV}`;
        document.getElementById('display-name').textContent = data.tenDV;
        document.getElementById('display-price').textContent = new Intl.NumberFormat('vi-VN').format(data.giaTien || 0) + ' VNĐ';
        document.getElementById('display-unit').textContent = data.donVi;

    } catch (error) {
        console.error('Error:', error);
        document.getElementById('loading-spinner').style.display = 'none';
        showAlert('Lỗi khi tải thông tin dịch vụ.', 'danger');
    }
}

async function deleteService() {
    if (!serviceId) return;
    
    const btn = document.getElementById('btn-delete');
    const originalHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xóa...';

    try {
        const response = await fetch(`${API_URL}/${serviceId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showAlert('Xóa dịch vụ thành công!', 'success');
            setTimeout(() => { window.location.href = 'index.html'; }, 1500);
        } else {
            throw new Error('Xóa thất bại');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Không thể xóa dịch vụ này. Có thể nó đang được sử dụng ở nơi khác.', 'danger');
        btn.disabled = false;
        btn.innerHTML = originalHtml;
    }
}
