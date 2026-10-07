document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('create-form');
    const API_URL = 'http://localhost:8080/admin/api/rooms/add';

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const btnSave = document.getElementById('btn-save');
        btnSave.disabled = true;
        btnSave.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Đang lưu...';

        const payload = {
            name: document.getElementById('name').value,
            price: parseFloat(document.getElementById('price').value),
            imageUrl: document.getElementById('imageUrl').value,
            maTrangThai: parseInt(document.getElementById('maTrangThai').value),
            detail: document.getElementById('detail').value,
            ghiChu: document.getElementById('ghiChu').value,
            soGiuongPhuToiDa: parseInt(document.getElementById('soGiuongPhuToiDa').value) || 0,
            maLoai: parseInt(document.getElementById('maLoai').value) || 1
        };

        try {
            const res = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();

            if(res.ok) {
                alert("Thêm phòng thành công!");
                window.location.href = 'index.html';
            } else {
                alert("Lỗi: " + (data.message || "Không thể thêm phòng"));
                btnSave.disabled = false;
                btnSave.innerHTML = '<i class="bi bi-floppy me-1"></i> Lưu Lại';
            }
        } catch(err) {
            alert("Lỗi kết nối tới server!");
            btnSave.disabled = false;
            btnSave.innerHTML = '<i class="bi bi-floppy me-1"></i> Lưu Lại';
        }
    });
});
