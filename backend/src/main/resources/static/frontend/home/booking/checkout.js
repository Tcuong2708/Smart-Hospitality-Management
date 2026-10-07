document.addEventListener('DOMContentLoaded', () => {
    // 1. Lấy thông tin từ URL
    const urlParams = new URLSearchParams(window.location.search);
    const roomId = urlParams.get('room_id');
    const urlQty = urlParams.get('qty') || 1;

    if(!roomId) {
        alert("Lỗi: Không tìm thấy phòng để đặt! Vui lòng chọn lại.");
        window.location.href = '../rooms/list.html';
        return;
    }

    // API
    const API_ROOM = `http://localhost:8080/api/rooms/${roomId}`;
    const API_CATEGORIES = `http://localhost:8080/api/categories`;
    const API_SERVICES = `http://localhost:8080/api/booking/services`;
    const API_CHECKOUT = `http://localhost:8080/api/booking/checkout`;

    // DOM Elements
    const checkinInput = document.getElementById('checkin-date');
    const checkoutInput = document.getElementById('checkout-date');
    const qtyInput = document.getElementById('room-quantity');
    const servicesContainer = document.getElementById('services-container');
    const btnSubmit = document.getElementById('btn-submit-booking');
    const noteInput = document.getElementById('booking-note');

    // State
    let roomData = null;
    let servicesData = [];
    let selectedServices = [];
    let categoryMap = {};
    const userMaxPoints = 5420;
    let redeemedPoints = 0;
    let redeemedDiscount = 0;

    // DOM Points elements
    const pointsSwitch = document.getElementById('use-points-switch');
    const pointsBox = document.getElementById('points-input-box');
    const pointsInput = document.getElementById('input-points-to-use');
    const pointsDiscountText = document.getElementById('points-discount-text');
    const sumDiscountRow = document.getElementById('sum-discount-row');
    const sumDiscountTotal = document.getElementById('sum-discount-total');

    // Helper formatter
    const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount);

    // Bắt sự kiện Đổi điểm
    if (pointsSwitch) {
        pointsSwitch.addEventListener('change', () => {
            if (pointsSwitch.checked) {
                pointsBox.classList.remove('d-none');
                if (!pointsInput.value || parseInt(pointsInput.value) <= 0) {
                    pointsInput.value = Math.min(500, userMaxPoints);
                }
            } else {
                pointsBox.classList.add('d-none');
                pointsInput.value = 0;
            }
            updateSummary();
        });
    }

    if (pointsInput) {
        pointsInput.addEventListener('input', () => {
            let val = parseInt(pointsInput.value) || 0;
            if (val > userMaxPoints) val = userMaxPoints;
            if (val < 0) val = 0;
            pointsInput.value = val;
            updateSummary();
        });
    }

    // 2. Khởi tạo ngày mặc định (Hôm nay và Ngày mai)
    const initDates = () => {
        const today = new Date();
        const tmr = new Date(today);
        tmr.setDate(tmr.getDate() + 1);
        
        checkinInput.value = today.toISOString().split('T')[0];
        checkinInput.min = checkinInput.value;
        
        checkoutInput.value = tmr.toISOString().split('T')[0];
        checkoutInput.min = tmr.toISOString().split('T')[0];
        
        qtyInput.value = urlQty;
    };
    initDates();

    // 3. Hàm tính toán và cập nhật Giao diện Tóm tắt
    const updateSummary = () => {
        if(!roomData) return;

        const checkin = new Date(checkinInput.value);
        const checkout = new Date(checkoutInput.value);
        
        let nights = 0;
        if(checkout > checkin) {
            nights = Math.ceil((checkout - checkin) / (1000 * 60 * 60 * 24));
        }

        let qty = parseInt(qtyInput.value);
        if(isNaN(qty) || qty < 1) {
            qty = 1;
            qtyInput.value = 1;
        }
        
        document.getElementById('sum-nights').textContent = `${nights} đêm`;
        document.getElementById('sum-rooms').textContent = `${qty} phòng`;

        // Tiền phòng cơ bản
        const baseTotal = roomData.price * nights * qty;
        document.getElementById('sum-base-total').textContent = formatCurrency(baseTotal) + ' đ';

        // Tiền dịch vụ
        let servicesTotal = 0;
        selectedServices.forEach(id => {
            const svc = servicesData.find(s => s.id === id);
            if(svc) servicesTotal += svc.giaTien;
        });

        document.getElementById('sum-services-total').textContent = formatCurrency(servicesTotal) + ' đ';

        // Đổi điểm giảm giá
        const subTotal = baseTotal + servicesTotal;
        redeemedPoints = (pointsSwitch && pointsSwitch.checked) ? (parseInt(pointsInput.value) || 0) : 0;
        redeemedDiscount = redeemedPoints * 1000;
        if (redeemedDiscount > subTotal) {
            redeemedDiscount = subTotal;
        }

        if (pointsDiscountText) pointsDiscountText.textContent = `- ${formatCurrency(redeemedDiscount)} đ`;

        if (redeemedDiscount > 0) {
            if (sumDiscountRow) sumDiscountRow.classList.remove('d-none');
            if (sumDiscountTotal) sumDiscountTotal.textContent = `- ${formatCurrency(redeemedDiscount)} đ`;
        } else {
            if (sumDiscountRow) sumDiscountRow.classList.add('d-none');
        }

        // Tổng cộng cuối cùng
        const finalTotal = subTotal - redeemedDiscount;
        document.getElementById('sum-final-total').textContent = formatCurrency(finalTotal) + ' đ';

        // Kiểm tra hợp lệ
        if(nights <= 0) {
            btnSubmit.disabled = true;
            document.getElementById('sum-final-total').textContent = 'Lỗi chọn ngày';
        } else {
            btnSubmit.disabled = false;
        }
    };

    // Bắt sự kiện thay đổi ngày / số lượng
    checkinInput.addEventListener('change', () => {
        const ci = new Date(checkinInput.value);
        const co = new Date(checkoutInput.value);
        if(ci >= co) {
            const newCo = new Date(ci);
            newCo.setDate(newCo.getDate() + 1);
            checkoutInput.value = newCo.toISOString().split('T')[0];
        }
        checkoutInput.min = checkinInput.value;
        updateSummary();
    });
    checkoutInput.addEventListener('change', updateSummary);
    qtyInput.addEventListener('input', updateSummary);

    // Xử lý Checkbox Dịch vụ
    window.toggleService = (checkbox, id) => {
        if(checkbox.checked) {
            selectedServices.push(id);
        } else {
            selectedServices = selectedServices.filter(s => s !== id);
        }
        updateSummary();
    };

    // 4. Lấy dữ liệu API
    const loadData = async () => {
        try {
            // Lấy danh mục phòng
            const catRes = await fetch(API_CATEGORIES);
            if(catRes.ok) {
                const cats = await catRes.json();
                cats.forEach(c => categoryMap[c.maLoai] = c.name);
            }

            // Lấy dịch vụ
            const svcRes = await fetch(API_SERVICES);
            if(svcRes.ok) {
                servicesData = await svcRes.json();
                let html = '';
                servicesData.forEach(s => {
                    html += `
                        <div class="col-md-6">
                            <label class="service-item d-flex align-items-center w-100 m-0">
                                <input type="checkbox" class="service-checkbox" onchange="toggleService(this, ${s.id})">
                                <div>
                                    <h6 class="m-0 text-navy fw-bold" style="font-size: 0.9rem;">${s.tenDV}</h6>
                                    <small class="text-danger fw-bold">${formatCurrency(s.giaTien)} đ</small>
                                </div>
                            </label>
                        </div>
                    `;
                });
                servicesContainer.innerHTML = html;
            }

            // Lấy thông tin phòng đang chọn
            const roomRes = await fetch(API_ROOM);
            if(!roomRes.ok) throw new Error("Không lấy được dữ liệu phòng.");
            roomData = await roomRes.json();

            // Hiển thị thông tin lên cột Tóm tắt
            document.getElementById('summary-loading').classList.add('d-none');
            document.getElementById('summary-content').classList.remove('d-none');

            let imgSrc = roomData.imageUrl ? `../../images/${roomData.imageUrl}` : 'https://via.placeholder.com/150';
            if(roomData.imageUrl && roomData.imageUrl.startsWith("http")) imgSrc = roomData.imageUrl;
            
            document.getElementById('sum-room-img').src = imgSrc;
            document.getElementById('sum-room-name').textContent = roomData.name;
            document.getElementById('sum-room-cat').textContent = categoryMap[roomData.maLoai] || 'Phòng tiêu chuẩn';
            document.getElementById('sum-room-price').textContent = formatCurrency(roomData.price);

            updateSummary();

        } catch(error) {
            console.error(error);
            document.getElementById('summary-loading').innerHTML = '<p class="text-danger fw-bold">Lỗi kết nối máy chủ!</p>';
        }
    };
    loadData();

    // 5. Submit Form Đặt Phòng
    btnSubmit.addEventListener('click', async () => {
        const checkin = checkinInput.value;
        const checkout = checkoutInput.value;
        const qty = parseInt(qtyInput.value);
        const note = noteInput.value;
        const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;

        const payload = {
            roomId: parseInt(roomId),
            soLuong: qty,
            ngayNhan: checkin,
            ngayTra: checkout,
            serviceIds: selectedServices,
            phuongThucThanhToan: paymentMethod,
            ghiChu: note,
            diemDoi: redeemedPoints,
            giamGiaDiem: redeemedDiscount
        };

        // UI Loading
        btnSubmit.disabled = true;
        const originText = btnSubmit.innerHTML;
        btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Đang xử lý...';

        try {
            const res = await fetch(API_CHECKOUT, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await res.json();
            
            if(!res.ok) {
                alert("Lỗi: " + (result.error || "Không thể đặt phòng lúc này."));
                btnSubmit.disabled = false;
                btnSubmit.innerHTML = originText;
                return;
            }

            // Đặt thành công -> Chuyển hướng
            if(result.paymentMethod === "ONLINE") {
                window.location.href = `payment_gate.html?invoice_id=${result.invoiceId}`;
            } else {
                window.location.href = `success.html`;
            }

        } catch(error) {
            console.error("Lỗi khi fetch", error);
            alert("Đã xảy ra lỗi mạng. Hãy thử lại!");
            btnSubmit.disabled = false;
            btnSubmit.innerHTML = originText;
        }
    });

});
