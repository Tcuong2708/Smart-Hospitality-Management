document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('register-form');
    const errorBox = document.getElementById('error-message');
    const errorText = errorBox.querySelector('span');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorBox.style.display = 'none';

        const hoTen = document.getElementById('hoTen').value.trim();
        const soDienThoai = document.getElementById('soDienThoai').value.trim();
        const email = document.getElementById('email').value.trim();
        const matKhau = document.getElementById('matKhau').value;
        const xacNhanMatKhau = document.getElementById('xacNhanMatKhau').value;
        const captcha = document.getElementById('captcha').value.trim();

        if (matKhau !== xacNhanMatKhau) {
            errorText.textContent = 'Mật khẩu xác nhận không khớp.';
            errorBox.style.display = 'block';
            return;
        }

        if (captcha.toUpperCase() !== 'X7K9') {
            errorText.textContent = 'Mã Captcha không chính xác.';
            errorBox.style.display = 'block';
            return;
        }

        // Mock check tài khoản tồn tại (Email/Số điện thoại)
        if (email.toLowerCase() === 'admin@gmail.com' || soDienThoai === '0987654321') {
            errorText.textContent = 'Tài khoản đã tồn tại. Vui lòng đăng nhập hoặc sử dụng thông tin khác!';
            errorBox.style.display = 'block';
            return;
        }

        const payload = {
            hoTen,
            soDienThoai,
            email,
            matKhau
        };

        const submitBtn = document.getElementById('btn-submit');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang xử lý...';

        try {
            /* Code gọi API thực tế (Tạm ẩn để mock Frontend)
            const API_URL = 'http://localhost:8080/api/auth/register';
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.' }));
                throw new Error(errorData.message || 'Tài khoản đã tồn tại. Vui lòng đăng nhập hoặc sử dụng thông tin khác!');
            }
            */

            // Delay giả lập mạng
            await new Promise(r => setTimeout(r, 800));
            
            // Redirect sang trang nhập OTP, truyền email qua query param để lấy ở trang sau
            window.location.href = `verify_register_otp.html?email=${encodeURIComponent(payload.email)}`;

        } catch (error) {
            console.error('Register error:', error);
            errorText.textContent = error.message;
            errorBox.style.display = 'block';
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="bi bi-person-plus-fill me-2"></i> ĐĂNG KÝ NGAY';
        }
    });
});
