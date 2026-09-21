document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('find-account-form');
    const errorBox = document.getElementById('error-message');
    const errorText = errorBox.querySelector('span');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorBox.style.display = 'none';

        const keyword = document.getElementById('keyword').value.trim();

        const submitBtn = document.getElementById('btn-submit');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang tìm kiếm...';

        try {
            const API_URL = 'http://localhost:8080/api/auth/find_account';
            
            // Tùy theo API bạn viết ở Backend, có thể gửi JSON hoặc x-www-form-urlencoded
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ keyword: keyword })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Không tìm thấy tài khoản phù hợp với thông tin này.' }));
                throw new Error(errorData.message || 'Không tìm thấy tài khoản');
            }

            // Gọi API thành công, hệ thống đã gửi OTP đến email
            // Redirect tới trang verify_otp
            window.location.href = `verify_otp.html?keyword=${encodeURIComponent(keyword)}`;

        } catch (error) {
            console.error('Find account error:', error);
            errorText.textContent = error.message;
            errorBox.style.display = 'block';
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="bi bi-search me-2"></i> TÌM KIẾM';
        }
    });
});
