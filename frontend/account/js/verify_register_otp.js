document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('verify-otp-form');
    const resendForm = document.getElementById('resend-otp-form');
    const errorBox = document.getElementById('error-message');
    const errorText = errorBox.querySelector('span');

    // Extract email from URL query params
    const urlParams = new URLSearchParams(window.location.search);
    const email = urlParams.get('email');

    if (!email) {
        errorText.textContent = "Không tìm thấy thông tin email. Vui lòng thử lại quá trình đăng ký.";
        errorBox.style.display = 'block';
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorBox.style.display = 'none';

        const otp = document.getElementById('otp').value.trim();

        if (otp.length !== 6) {
            errorText.textContent = "Mã OTP phải bao gồm 6 chữ số.";
            errorBox.style.display = 'block';
            return;
        }

        const submitBtn = document.getElementById('btn-submit');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang xử lý...';

        try {
            const API_URL = 'http://localhost:8080/api/auth/verify_register_otp';
            
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email: email, otp: otp })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Xác thực thất bại. OTP không đúng hoặc đã hết hạn.' }));
                throw new Error(errorData.message || 'OTP không hợp lệ');
            }

            // Kích hoạt thành công, redirect tới trang đăng nhập
            window.location.href = `login.html?success=${encodeURIComponent('Tài khoản đã được kích hoạt thành công! Vui lòng đăng nhập.')}`;

        } catch (error) {
            console.error('OTP Verification error:', error);
            errorText.textContent = error.message;
            errorBox.style.display = 'block';
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = 'KÍCH HOẠT TÀI KHOẢN <i class="bi bi-shield-check ms-1"></i>';
        }
    });

    resendForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        if (!confirm('Hệ thống sẽ tạo và gửi lại mã OTP kích hoạt mới về Email của bạn?')) {
            return;
        }

        const resendBtn = document.getElementById('btn-resend');
        resendBtn.disabled = true;
        resendBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Đang gửi...';

        try {
            const API_URL = 'http://localhost:8080/api/auth/resend_otp'; // Cần điều chỉnh API gửi lại OTP thực tế
            
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email })
            });

            if (!response.ok) {
                throw new Error('Gửi lại OTP thất bại.');
            }

            // Gọi hàm showToast từ app.js để thông báo thành công
            if(typeof showToast === 'function') {
                showToast('Mã OTP mới đã được gửi đến email của bạn.', 'success');
            } else {
                alert('Mã OTP mới đã được gửi đến email của bạn.');
            }

        } catch (error) {
            console.error('Resend OTP error:', error);
            errorText.textContent = 'Không thể gửi lại OTP. Vui lòng thử lại sau.';
            errorBox.style.display = 'block';
        } finally {
            resendBtn.disabled = false;
            resendBtn.innerHTML = '<i class="bi bi-arrow-clockwise"></i> Gửi lại mã mới';
        }
    });
});
