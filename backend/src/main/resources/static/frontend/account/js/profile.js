document.addEventListener('DOMContentLoaded', () => {
    fetchProfileData();

    document.getElementById('profile-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        await updateProfile();
    });
});

function showAlert(message, isSuccess) {
    const alertBox = document.getElementById('alert-message');
    const alertIcon = document.getElementById('alert-icon');
    const alertText = document.getElementById('alert-text');

    alertBox.className = `alert alert-dismissible fade show shadow-sm alert-${isSuccess ? 'success' : 'danger'}`;
    alertIcon.className = `bi ${isSuccess ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'} me-2`;
    alertText.textContent = message;
    alertBox.style.display = 'block';

    if (isSuccess && typeof showToast === 'function') {
        showToast('Cập nhật hồ sơ thành công!', 'success');
    }
}

async function fetchProfileData() {
    // Giả lập dữ liệu hồ sơ
    const mockUser = {
        idTaiKhoan: 1,
        tenDangNhap: 'admin',
        hoTen: 'Quản Trị Viên',
        soDienThoai: '0909999999',
        quocTich: 'Việt Nam',
        diaChi: 'Hà Nội',
        roleID: 1
    };

    // Populate UI
    document.getElementById('display-name').textContent = mockUser.hoTen || 'Họ và tên';
    
    let roleName = 'VIP MEMBER';
    if (mockUser.roleID === 1) roleName = 'QUẢN TRỊ VIÊN';
    else if (mockUser.roleID === 2) roleName = 'NHÂN VIÊN';
    document.getElementById('display-role').textContent = roleName;

    // Populate Form
    document.getElementById('idTaiKhoan').value = mockUser.idTaiKhoan || '';
    document.getElementById('tenDangNhap').value = mockUser.tenDangNhap || '';
    document.getElementById('hoTen').value = mockUser.hoTen || '';
    document.getElementById('soDienThoai').value = mockUser.soDienThoai || '';
    document.getElementById('quocTich').value = mockUser.quocTich || '';
    document.getElementById('diaChi').value = mockUser.diaChi || '';
}

async function updateProfile() {
    const btn = document.getElementById('btn-update');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang lưu...';

    const payload = {
        hoTen: document.getElementById('hoTen').value.trim(),
    };

    // Giả lập lưu dữ liệu thành công
    setTimeout(() => {
        showAlert('Thông tin đã được cập nhật thành công.', true);
        
        // Cập nhật lại tên hiển thị bên cột trái
        document.getElementById('display-name').textContent = payload.hoTen;

        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-save2-fill me-2"></i>Lưu thông tin';
    }, 800);
}
