const API_URL = 'http://localhost:8080/api/invoices';

document.addEventListener('DOMContentLoaded', () => {
    fetchEmptyRooms();

    const form = document.getElementById('create-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalBtnHtml = submitBtn.innerHTML;
        
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>ĐANG XỬ LÝ...';

        const data = {
            hoTen: document.getElementById('hoTen').value,
            sdt: document.getElementById('sdt').value,
            diaChi: document.getElementById('diaChi').value,
            maPhong: document.getElementById('maPhong').value,
            ngayCheckIn: document.getElementById('ngayCheckIn').value,
            ngayCheckOut: document.getElementById('ngayCheckOut').value
        };

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                showAlert('Tạo đơn đặt phòng thành công!', 'success');
                form.reset();
                setTimeout(() => { window.location.href = 'index.html'; }, 1500);
            } else {
                throw new Error('Lỗi khi lưu dữ liệu');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Có lỗi xảy ra, không thể lưu đơn đặt phòng.', 'danger');
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnHtml;
        }
    });
});

async function fetchEmptyRooms() {
    try {
        const response = await fetch(`${API_URL}/empty-rooms`);
        if (!response.ok) throw new Error('Cannot fetch rooms');
        
        const rooms = await response.json();
        const select = document.getElementById('maPhong');
        
        select.innerHTML = '<option value="">-- Vui lòng chọn phòng trống --</option>';
        rooms.forEach(p => {
            const price = new Intl.NumberFormat('vi-VN').format(p.price || 0);
            select.innerHTML += `<option value="${p.id}">Phòng ${p.id} (Giá: ${price} đ/đêm)</option>`;
        });
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('maPhong').innerHTML = '<option value="">-- Lỗi tải danh sách phòng --</option>';
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
