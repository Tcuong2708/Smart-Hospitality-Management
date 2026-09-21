document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('verify-otp-form');
    const resendForm = document.getElementById('resend-otp-form');
    const errorBox = document.getElementById('error-message');
    const errorText = errorBox.querySelector('span');

    // Lấy keyword từ URL (email hoặc SDT đã nhập ở bước trước)
    const urlParams = new URLSearchParams(window.location.search);
    const keyword = urlParams.get('keyword');

    if (!keyword) {
        errorText.textContent = "Không tìm thấy thông tin tài khoản. Vui lòng quay lại bước tìm tài khoản.";
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
            /* Tạm ẩn gọi API thực tế
            const API_URL = 'http://localhost:8080/api/auth/verify_otp';
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ keyword: keyword, otp: otp })
            });

            if (!response.ok) {
                throw new Error('OTP không hợp lệ hoặc đã hết hạn');
            }
            */

            // Delay giả lập mạng
            await new Promise(r => setTimeout(r, 800));

            // Xác thực thành công, chuyển sang màn hình nhập mật khẩu mới
            window.location.href = `forgot_password.html?keyword=${encodeURIComponent(keyword)}`;

        } catch (error) {
            console.error('Verify OTP error:', error);
            errorText.textContent = error.message;
            errorBox.style.display = 'block';
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = 'XÁC NHẬN MÃ OTP <i class="bi bi-check-lg ms-1"></i>';
        }
    });

    resendForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        if (!confirm('Hệ thống sẽ tạo và gửi lại mã OTP mới cho bạn?')) {
            return;
        }

        const resendBtn = document.getElementById('btn-resend');
        resendBtn.disabled = true;
        resendBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Đang gửi...';

        try {
            const API_URL = 'http://localhost:8080/api/auth/find_account'; 
            
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ keyword: keyword })
            });

            if (!response.ok) {
                throw new Error('Gửi lại OTP thất bại.');
            }

            if(typeof showToast === 'function') {
                showToast('Mã OTP mới đã được gửi.', 'success');
            } else {
                alert('Mã OTP mới đã được gửi.');
            }

        } catch (error) {
            console.error('Resend OTP error:', error);
            errorText.textContent = 'Không thể gửi lại OTP. Vui lòng thử lại sau.';
            errorBox.style.display = 'block';
        } finally {
            resendBtn.disabled = false;
            resendBtn.innerHTML = '<i class="bi bi-arrow-clockwise"></i> Gửi lại mã';
        }
    });
});
