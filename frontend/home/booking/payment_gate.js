document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const invoiceId = urlParams.get('invoice_id');

    if(!invoiceId) {
        alert("Không tìm thấy mã hóa đơn để thanh toán!");
        window.location.href = '../rooms/list.html';
        return;
    }

    document.getElementById('invoice-id').textContent = '#' + invoiceId;

    const API_INVOICE = `http://localhost:8080/api/booking/invoice/${invoiceId}`;
    const API_PAYMENT = `http://localhost:8080/api/booking/payment/${invoiceId}`;

    const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount);

    // Fetch Invoice Total
    fetch(API_INVOICE)
        .then(res => res.json())
        .then(data => {
            if(data.totalPrice) {
                document.getElementById('invoice-total').textContent = formatCurrency(data.totalPrice) + ' đ';
            }
        })
        .catch(e => console.error("Lỗi lấy thông tin bill:", e));

    // Countdown Timer (10 phút)
    let time = 600; 
    const countdownEl = document.getElementById('countdown');
    const timer = setInterval(() => {
        let m = Math.floor(time / 60);
        let s = time % 60;
        countdownEl.textContent = `${m < 10 ? '0'+m : m}:${s < 10 ? '0'+s : s}`;
        time--;
        if(time < 0) {
            clearInterval(timer);
            alert("Đã hết thời gian thanh toán!");
            window.location.href = '../rooms/list.html';
        }
    }, 1000);

    // Simulate Payment (Mô phỏng user đã quét mã thành công)
    document.getElementById('btn-confirm-payment').addEventListener('click', async () => {
        const btn = document.getElementById('btn-confirm-payment');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Đang xác nhận giao dịch...';

        try {
            const res = await fetch(API_PAYMENT, { method: 'POST' });
            if(res.ok) {
                window.location.href = 'success.html';
            } else {
                alert("Lỗi xác nhận thanh toán từ hệ thống.");
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-check-circle-fill me-2"></i>Đã thanh toán (Mô phỏng)';
            }
        } catch(e) {
            alert("Lỗi kết nối tới máy chủ.");
        }
    });
});
