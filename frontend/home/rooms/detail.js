document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const roomId = urlParams.get('id');

    const API_URL = `http://localhost:8080/api/rooms/${roomId}`;
    const API_CATEGORIES = 'http://localhost:8080/api/categories';

    const inputSoLuong = document.getElementById('soLuong');
    const txtTamTinh = document.getElementById('tamTinh');
    const btnSubmit = document.getElementById('btn-submit-booking');
    const btnPlus = document.getElementById('btn-plus');
    const btnMinus = document.getElementById('btn-minus');

    let basePrice = 0;
    let categoryMap = {};

    const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount);

    const renderRoomDetail = (room) => {
        document.getElementById('loading').classList.add('d-none');
        document.getElementById('room-detail-content').classList.remove('d-none');

        let imgSrc = room.imageUrl ? `../../images/${room.imageUrl}` : 'https://via.placeholder.com/600x450?text=No+Image';
        if (room.imageUrl && room.imageUrl.startsWith("http")) {
            imgSrc = room.imageUrl;
        }

        document.getElementById('room-image').src = imgSrc;
        document.getElementById('breadcrumb-name').textContent = room.name;
        document.getElementById('room-name').textContent = room.name;

        basePrice = room.price;
        document.getElementById('room-price').textContent = formatCurrency(basePrice);

        const statusBadge = document.getElementById('room-status-badge');
        if (room.maTrangThai === 1) {
            statusBadge.className = 'status-badge text-success border-success bg-success bg-opacity-10';
            statusBadge.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i>Còn phòng trống';
            btnSubmit.disabled = false;
        } else {
            statusBadge.className = 'status-badge text-danger border-danger bg-danger bg-opacity-10';
            statusBadge.innerHTML = '<i class="bi bi-x-circle-fill me-1"></i>Hết phòng';
            btnSubmit.disabled = true;
        }

        document.getElementById('room-category').textContent = categoryMap[room.maLoai] || 'Phòng Tiêu Chuẩn';
        document.getElementById('room-short-desc').textContent = room.detail;

        document.getElementById('room-detail-text').textContent = room.detail;
        document.getElementById('room-note-text').textContent = room.ghiChu || 'Phòng được trang bị đầy đủ tiện nghi hiện đại chuẩn 5 sao quốc tế.';

        const extraBedEl = document.getElementById('max-extra-bed');
        if (room.soGiuongPhuToiDa != null && room.soGiuongPhuToiDa > 0) {
            extraBedEl.innerHTML = `Số giường phụ tối đa: <strong>${room.soGiuongPhuToiDa}</strong>`;
            extraBedEl.style.display = 'list-item';
        } else {
            extraBedEl.style.display = 'none';
        }

        document.getElementById('room_id').value = room.id;
        updateTamTinh();
    };

    const updateTamTinh = () => {
        let sl = parseInt(inputSoLuong.value) || 1;
        if (sl < 1) sl = 1;
        inputSoLuong.value = sl;
        let tong = basePrice * sl;
        txtTamTinh.textContent = formatCurrency(tong);
    };

    btnPlus.addEventListener('click', () => {
        inputSoLuong.value = parseInt(inputSoLuong.value) + 1;
        updateTamTinh();
    });

    btnMinus.addEventListener('click', () => {
        let current = parseInt(inputSoLuong.value);
        if (current > 1) {
            inputSoLuong.value = current - 1;
            updateTamTinh();
        }
    });

    inputSoLuong.addEventListener('input', updateTamTinh);

    document.getElementById('booking-form').addEventListener('submit', (e) => {
        e.preventDefault();
        const sl = inputSoLuong.value;
        // Chuyển hướng sang trang checkout ở luồng MỚI, truyền tham số phòng
        window.location.href = `../booking/checkout.html?room_id=${roomId}&qty=${sl}`;
    });

    const mockCategories = [
        { maLoai: 'L01', name: 'Phòng Standard (Tiêu Chuẩn)' },
        { maLoai: 'L02', name: 'Phòng Superior (Cao Cấp)' },
        { maLoai: 'L03', name: 'Phòng Deluxe (Sang Trọng)' },
        { maLoai: 'L04', name: 'Phòng Suite (Thượng Gia)' }
    ];

    const mockRooms = [
        { id: 1, maLoai: 'L01', name: 'Phòng Đơn (Single Room)', price: 500000, imageUrl: 'd1.jpg', maTrangThai: 1, detail: 'Phòng tiêu chuẩn thoải mái', soGiuongPhuToiDa: 1 },
        { id: 2, maLoai: 'L01', name: 'Phòng Đơn (Single Room)', price: 500000, imageUrl: 'd3.jpg', maTrangThai: 1, detail: 'Phòng tiêu chuẩn tiện nghi', soGiuongPhuToiDa: 0 },
        { id: 3, maLoai: 'L02', name: 'Phòng Đôi (Double Room)', price: 800000, imageUrl: 'd4.jpg', maTrangThai: 1, detail: 'Phòng cao cấp view đẹp', soGiuongPhuToiDa: 1 },
        { id: 4, maLoai: 'L02', name: 'Phòng Đôi (Double Room)', price: 800000, imageUrl: 'd5.jpg', maTrangThai: 0, detail: 'Phòng cao cấp sang trọng', soGiuongPhuToiDa: 2 },
        { id: 5, maLoai: 'L03', name: 'Phòng Gia Đình (Family Room)', price: 1500000, imageUrl: 'gd1.jpg', maTrangThai: 1, detail: 'Phòng deluxe rộng rãi', soGiuongPhuToiDa: 2 },
        { id: 6, maLoai: 'L04', name: 'Phòng Suite (Thượng Gia)', price: 3500000, imageUrl: 'su1.jpg', maTrangThai: 1, detail: 'Phòng tổng thống đẳng cấp nhất', soGiuongPhuToiDa: 3 }
    ];

    const init = () => {
        if (!roomId) {
            document.getElementById('loading').innerHTML = '<p class="text-danger fw-bold fs-5 text-center mt-5">Không tìm thấy mã phòng. Vui lòng quay lại trang danh sách!</p>';
            return;
        }

        try {
            mockCategories.forEach(c => { categoryMap[c.maLoai] = c.name; });

            const roomData = mockRooms.find(r => r.id == roomId);
            if (!roomData) throw new Error('Not found');

            renderRoomDetail(roomData);
        } catch (error) {
            console.error(error);
            document.getElementById('loading').innerHTML = '<p class="text-danger fw-bold text-center mt-5">Không tìm thấy phòng trong dữ liệu mẫu.</p>';
        }
    };

    init();
});
