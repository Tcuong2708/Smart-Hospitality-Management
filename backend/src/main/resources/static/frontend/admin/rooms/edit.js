document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const roomId = urlParams.get('id');

    if(!roomId) {
        alert("Lỗi: Không có ID phòng!");
        window.location.href = 'index.html';
        return;
    }

    const API_URL = `http://localhost:8080/admin/api/rooms/${roomId}`;
    const form = document.getElementById('edit-form');

    // Fetch existing data
    fetch(API_URL)
        .then(res => res.json())
        .then(data => {
            if(data.message && data.message.includes("Không tìm thấy")) throw new Error(data.message);
            
            document.getElementById('name').value = data.name;
            document.getElementById('price').value = data.price;
            document.getElementById('imageUrl').value = data.imageUrl || '';
            document.getElementById('maTrangThai').value = data.maTrangThai || 1;
            document.getElementById('soGiuongPhuToiDa').value = data.soGiuongPhuToiDa || 0;
            document.getElementById('maLoai').value = data.maLoai || 1;
            document.getElementById('detail').value = data.detail || '';
            document.getElementById('ghiChu').value = data.ghiChu || '';

            document.getElementById('loading').classList.add('d-none');
            form.classList.remove('d-none');
        })
        .catch(err => {
            alert("Lỗi tải dữ liệu: " + err.message);
            window.location.href = 'index.html';
        });

    // Handle form submit
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
            soGiuongPhuToiDa: parseInt(document.getElementById('soGiuongPhuToiDa').value),
            maLoai: parseInt(document.getElementById('maLoai').value)
        };

        try {
            const res = await fetch(API_URL, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();

            if(res.ok) {
                alert("Cập nhật phòng thành công!");
                window.location.href = 'index.html';
            } else {
                alert("Lỗi: " + (data.message || "Không thể cập nhật"));
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
