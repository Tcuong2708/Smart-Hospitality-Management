const API_URL = 'http://localhost:8080/api/invoices';

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const invoiceId = urlParams.get('id');

    if (invoiceId) {
        fetchInvoiceDetails(invoiceId);
    } else {
        showAlert('Không tìm thấy ID hóa đơn.', 'danger');
        document.getElementById('loading-spinner').style.display = 'none';
    }
});

async function fetchInvoiceDetails(id) {
    try {
        const response = await fetch(`${API_URL}/${id}`);
        if (!response.ok) throw new Error('Không thể tải dữ liệu hóa đơn');
        
        const invoice = await response.json();
        
        document.getElementById('loading-spinner').style.display = 'none';
        document.getElementById('details-content').style.display = 'flex';

        document.getElementById('invoice-id').textContent = invoice.id;
        document.getElementById('invoice-date').textContent = invoice.ngayDat || '';
        document.getElementById('customer-name').textContent = invoice.hoTen;
        document.getElementById('customer-phone').textContent = invoice.sdt || '';
        document.getElementById('customer-address').textContent = invoice.diaChi || 'Chưa cập nhật';

        const roomName = invoice.phong ? invoice.phong.name : `Mã phòng: #${invoice.maPhong}`;
        document.getElementById('room-name').textContent = roomName;
        document.getElementById('time-range').textContent = `${invoice.ngayCheckIn} đến ${invoice.ngayCheckOut}`;
        
        const roomPrice = invoice.phong ? new Intl.NumberFormat('vi-VN').format(invoice.phong.price) + ' đ' : '-';
        document.getElementById('room-price').textContent = roomPrice;
        
        const basePrice = invoice.totalPrice || 0;
        let finalPrice = basePrice;
        let discount = 0;
        
        const formatMoney = (val) => new Intl.NumberFormat('vi-VN').format(val) + ' đ';

        document.getElementById('room-total').textContent = formatMoney(basePrice);
        document.getElementById('invoice-total').textContent = formatMoney(finalPrice);

        // Tích lũy điểm (100.000 VNĐ = 1 điểm)
        const earnedPoints = Math.floor(finalPrice / 100000);
        document.getElementById('earned-points').textContent = `+ ${earnedPoints} Điểm`;

        // Logic đổi điểm
        const pointsInput = document.getElementById('points-to-use');
        const previewDiscount = document.getElementById('discount-preview-amount');
        const discountPreviewBox = document.getElementById('discount-preview');
        const btnApply = document.getElementById('btn-apply-points');
        const maxPoints = 5420;

        pointsInput.addEventListener('input', (e) => {
            let val = parseInt(e.target.value) || 0;
            if(val > maxPoints) val = maxPoints;
            if(val < 0) val = 0;
            e.target.value = val;
            
            const tempDiscount = val * 1000;
            previewDiscount.textContent = `- ${formatMoney(tempDiscount)}`;
            discountPreviewBox.classList.remove('d-none');
        });

        btnApply.addEventListener('click', () => {
            const usedPoints = parseInt(pointsInput.value) || 0;
            discount = usedPoints * 1000;
            finalPrice = basePrice - discount;
            if(finalPrice < 0) finalPrice = 0;
            
            document.getElementById('discount-amount').textContent = `- ${formatMoney(discount)}`;
            document.getElementById('invoice-total').textContent = formatMoney(finalPrice);
            
            // Cập nhật lại điểm nhận được
            const newEarnedPoints = Math.floor(finalPrice / 100000);
            document.getElementById('earned-points').textContent = `+ ${newEarnedPoints} Điểm`;
            
            showAlert(`Áp dụng thành công ${usedPoints} điểm để giảm giá!`, 'success');
            
            // Nếu khách đã nhập tiền rồi, cần tính lại tiền thối
            calculateChange();
        });

        // Xử lý tiền khách đưa (UC25)
        const cashInput = document.getElementById('cash-received');
        const changeAmountDisp = document.getElementById('change-amount');

        function calculateChange() {
            const cash = parseInt(cashInput.value) || 0;
            if (cash === 0) {
                changeAmountDisp.textContent = '0 đ';
                changeAmountDisp.className = 'fw-bold text-muted fs-5';
                return;
            }
            
            const change = cash - finalPrice;
            if (change < 0) {
                changeAmountDisp.textContent = `Thiếu ${formatMoney(Math.abs(change))}`;
                changeAmountDisp.className = 'fw-bold text-danger fs-5';
            } else {
                changeAmountDisp.textContent = formatMoney(change);
                changeAmountDisp.className = 'fw-bold text-success fs-5';
            }
        }

        cashInput.addEventListener('input', calculateChange);

    } catch (error) {
        console.error('Error:', error);
        document.getElementById('loading-spinner').style.display = 'none';
        showAlert('Có lỗi xảy ra khi tải chi tiết đơn hàng.', 'danger');
    }
}

function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alert-container');
    alertContainer.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}
