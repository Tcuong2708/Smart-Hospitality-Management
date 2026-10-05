const API_URL = 'http://localhost:8080/api/invoices';

const MOCK_INVOICES = {
    "1": {
        id: 1,
        hoTen: "Nguyễn Văn A",
        sdt: "0901234567",
        diaChi: "123 Nguyễn Trãi, Phường 2, Quận 5, TP. Hồ Chí Minh",
        ngayDat: "2026-06-15",
        ngayCheckIn: "2026-06-18",
        ngayCheckOut: "2026-06-20",
        totalPrice: 3600000,
        phong: { name: "Phòng 101 - Deluxe Ocean View", price: 1800000, id: 101 }
    },
    "2": {
        id: 2,
        hoTen: "Trần Thị B",
        sdt: "0987654321",
        diaChi: "45 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
        ngayDat: "2026-06-16",
        ngayCheckIn: "2026-06-17",
        ngayCheckOut: "2026-06-19",
        totalPrice: 2400000,
        phong: { name: "Phòng 202 - Standard Double", price: 1200000, id: 202 }
    },
    "3": {
        id: 3,
        hoTen: "Lê Hoàng C",
        sdt: "0912345678",
        diaChi: "78 Điện Biên Phủ, Phường 15, Quận Bình Thạnh, TP. Hồ Chí Minh",
        ngayDat: "2026-06-10",
        ngayCheckIn: "2026-06-12",
        ngayCheckOut: "2026-06-14",
        totalPrice: 5400000,
        phong: { name: "Phòng 305 - Suite President", price: 2700000, id: 305 }
    }
};

function getMockInvoice(id) {
    const key = String(id || '1');
    if (MOCK_INVOICES[key]) return MOCK_INVOICES[key];
    const numId = parseInt(id) || 1;
    return {
        id: numId,
        hoTen: `Khách hàng #${numId}`,
        sdt: "0901234567",
        diaChi: "140 Lê Trọng Tấn, Q. Tân Phú, TP. HCM",
        ngayDat: "2026-06-15",
        ngayCheckIn: "2026-06-18",
        ngayCheckOut: "2026-06-20",
        totalPrice: 3600000,
        phong: { name: `Phòng ${100 + numId} - Deluxe View`, price: 1800000, id: 100 + numId }
    };
}

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const invoiceId = urlParams.get('id') || '1';
    fetchInvoiceDetails(invoiceId);
});

async function fetchInvoiceDetails(id) {
    let invoice = null;

    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 1500);
        const response = await fetch(`${API_URL}/${id}`, { signal: controller.signal });
        clearTimeout(timeoutId);
        if (response.ok) {
            invoice = await response.json();
        }
    } catch (error) {
        console.warn('API connection failed or timed out. Using mock data.', error);
    }

    if (!invoice) {
        invoice = getMockInvoice(id);
    }

    // Hide spinner & show details container
    const spinner = document.getElementById('loading-spinner');
    if (spinner) spinner.style.display = 'none';

    const detailsContent = document.getElementById('details-content');
    if (detailsContent) detailsContent.style.display = 'flex';

    // Populate data
    const formatMoney = (val) => new Intl.NumberFormat('vi-VN').format(val || 0) + ' đ';

    const elId = document.getElementById('invoice-id');
    if (elId) elId.textContent = invoice.id;

    const elDate = document.getElementById('invoice-date');
    if (elDate) elDate.textContent = invoice.ngayDat || '';

    const elCustName = document.getElementById('customer-name');
    if (elCustName) elCustName.textContent = invoice.hoTen || '';

    const elCustPhone = document.getElementById('customer-phone');
    if (elCustPhone) elCustPhone.textContent = invoice.sdt || '';

    const elCustAddr = document.getElementById('customer-address');
    if (elCustAddr) elCustAddr.textContent = invoice.diaChi || 'Chưa cập nhật';

    const roomName = invoice.phong ? invoice.phong.name : (invoice.maPhong ? `Mã phòng: #${invoice.maPhong}` : 'Phòng Deluxe');
    const elRoomName = document.getElementById('room-name');
    if (elRoomName) elRoomName.textContent = roomName;

    const elTimeRange = document.getElementById('time-range');
    if (elTimeRange) elTimeRange.textContent = `${invoice.ngayCheckIn || ''} đến ${invoice.ngayCheckOut || ''}`;

    const roomPriceVal = invoice.phong ? invoice.phong.price : (invoice.totalPrice ? invoice.totalPrice / 2 : 1800000);
    const elRoomPrice = document.getElementById('room-price');
    if (elRoomPrice) elRoomPrice.textContent = formatMoney(roomPriceVal);

    const basePrice = invoice.totalPrice || invoice.totalAmount || 3600000;
    let finalPrice = basePrice;
    let discount = 0;

    const elRoomTotal = document.getElementById('room-total');
    if (elRoomTotal) elRoomTotal.textContent = formatMoney(basePrice);

    const elInvTotal = document.getElementById('invoice-total');
    if (elInvTotal) elInvTotal.textContent = formatMoney(finalPrice);

    // Tích lũy điểm (100.000 VNĐ = 1 điểm)
    const earnedPoints = Math.floor(finalPrice / 100000);
    const elEarnedPoints = document.getElementById('earned-points');
    if (elEarnedPoints) elEarnedPoints.textContent = `+ ${earnedPoints} Điểm`;

    // Logic đổi điểm modal
    const pointsInput = document.getElementById('points-to-use');
    const previewDiscount = document.getElementById('discount-preview-amount');
    const discountPreviewBox = document.getElementById('discount-preview');
    const btnApply = document.getElementById('btn-apply-points');
    const maxPoints = 5420;

    if (pointsInput) {
        pointsInput.addEventListener('input', (e) => {
            let val = parseInt(e.target.value) || 0;
            if (val > maxPoints) val = maxPoints;
            if (val < 0) val = 0;
            e.target.value = val;

            const tempDiscount = val * 1000;
            if (previewDiscount) previewDiscount.textContent = `- ${formatMoney(tempDiscount)}`;
            if (discountPreviewBox) discountPreviewBox.classList.remove('d-none');
        });
    }

    if (btnApply) {
        btnApply.addEventListener('click', () => {
            const usedPoints = parseInt(pointsInput ? pointsInput.value : 0) || 0;
            discount = usedPoints * 1000;
            finalPrice = basePrice - discount;
            if (finalPrice < 0) finalPrice = 0;

            const elDiscountAmount = document.getElementById('discount-amount');
            if (elDiscountAmount) elDiscountAmount.textContent = `- ${formatMoney(discount)}`;

            if (elInvTotal) elInvTotal.textContent = formatMoney(finalPrice);

            const newEarnedPoints = Math.floor(finalPrice / 100000);
            if (elEarnedPoints) elEarnedPoints.textContent = `+ ${newEarnedPoints} Điểm`;

            showAlert(`Áp dụng thành công ${usedPoints} điểm để giảm giá!`, 'success');
            calculateChange();
        });
    }

    // Xử lý tiền khách đưa (UC25)
    const cashInput = document.getElementById('cash-received');
    const changeAmountDisp = document.getElementById('change-amount');

    function calculateChange() {
        if (!cashInput || !changeAmountDisp) return;
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

    if (cashInput) {
        cashInput.addEventListener('input', calculateChange);
    }
}

function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alert-container');
    if (!alertContainer) return;
    alertContainer.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}
