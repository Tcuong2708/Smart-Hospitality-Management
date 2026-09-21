document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('login-form');
    const errorBox = document.getElementById('error-message');
    const errorText = errorBox.querySelector('span');

    // Auto-fill success message if available
    const urlParams = new URLSearchParams(window.location.search);
    const successMsg = urlParams.get('success');
    if (successMsg) {
        errorBox.classList.remove('alert-danger');
        errorBox.classList.add('alert-success');
        errorBox.innerHTML = `<i class="bi bi-check-circle-fill me-2"></i><span>${successMsg}</span>`;
        errorBox.style.display = 'block';
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        errorBox.style.display = 'none';
        errorBox.classList.remove('alert-success');
        errorBox.classList.add('alert-danger');

        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        const submitBtn = document.getElementById('btn-submit');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang xử lý...';

        // Gọi API sau (Đã tạm comment để test mock)
        try {
            // Mock Login để kiểm tra phân quyền
            if (password === '123') {
                const validUsers = ['admin', 'director', 'manager', 'accountant', 'housekeeping', 'staff', 'guest'];
                if (validUsers.includes(username)) {
                    // Lưu thông tin người dùng vào localStorage
                    localStorage.setItem('userRole', username);
                    localStorage.setItem('username', username);

                    // Phân luồng điều hướng dựa trên role
                    if (username === 'admin') {
                        window.location.href = '../admin/users/index.html'; // Admin vào quản lý người dùng
                    } else if (username === 'director' || username === 'manager') {
                        window.location.href = '../admin/rooms/index.html'; // Quản lý vào quản lý phòng
                    } else if (username === 'accountant') {
                        window.location.href = '../admin/statistical/index.html'; // Kế toán vào thống kê
                    } else if (username === 'housekeeping') {
                        window.location.href = '../staff/room-status/index.html'; // Buồng phòng vào cập nhật trạng thái
                    } else if (username === 'staff') {
                        window.location.href = '../staff/room-map/index.html'; // Lễ tân vào sơ đồ phòng
                    } else {
                        window.location.href = '../index.html?success=Đăng nhập thành công'; // Khách thì về trang chủ
                    }
                    return;
                }
            }
            
            throw new Error('Thông tin Tài khoản hoặc Mật khẩu không chính xác. Vui lòng thử lại!');

            /* Code gọi API (Tạm ẩn)
            const API_URL = 'http://localhost:8080/api/auth/login';

            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    tenDangNhap: username,
                    matKhau: password
                })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.' }));
                throw new Error(errorData.message || 'Đăng nhập thất bại');
            }

            const data = await response.json();

            // Redirect sang trang chủ
            window.location.href = '../index.html?success=Đăng nhập thành công';
            */

        } catch (error) {
            console.error('Login error:', error);
            errorBox.innerHTML = `<i class="bi bi-exclamation-circle-fill me-2"></i><span>${error.message}</span>`;
            errorBox.style.display = 'block';
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="bi bi-box-arrow-in-right me-2"></i> ĐĂNG NHẬP';
        }
    });
});
