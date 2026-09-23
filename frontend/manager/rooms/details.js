document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const roomId = urlParams.get('id');

    if(!roomId) {
        alert("Lỗi: Không có ID phòng!");
        window.location.href = 'index.html';
        return;
    }

    const API_URL = `http://localhost:8080/admin/api/rooms/${roomId}`;
    const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount) + ' đ';

    const renderStatus = (maTrangThai) => {
        switch(maTrangThai) {
            case 1: return `<span class="status-badge status-1">Đang Trống</span>`;
            case 2: return `<span class="status-badge status-2">Đã Đặt</span>`;
            case 3: return `<span class="status-badge status-3">Bảo Trì</span>`;
            default: return `<span class="status-badge bg-secondary text-white">Unknown</span>`;
        }
    };

    fetch(API_URL)
        .then(res => res.json())
        .then(data => {
            if(data.message && data.message.includes("Không tìm thấy")) throw new Error(data.message);
            
            document.getElementById('dt-id').textContent = data.id;
            document.getElementById('dt-name').textContent = data.name;
            document.getElementById('dt-price').textContent = formatCurrency(data.price);
            document.getElementById('dt-status').innerHTML = renderStatus(data.maTrangThai);
            document.getElementById('dt-type').textContent = data.maLoai || '1';
            document.getElementById('dt-bed').textContent = data.soGiuongPhuToiDa || 0;
            document.getElementById('dt-desc').textContent = data.detail || 'Không có mô tả';
            document.getElementById('dt-note').textContent = data.ghiChu || 'Không có';

            let imgUrl = data.imageUrl ? (data.imageUrl.startsWith("http") ? data.imageUrl : `../../images/${data.imageUrl}`) : 'https://via.placeholder.com/400x300';
            document.getElementById('dt-image').src = imgUrl;

            document.getElementById('btn-edit-link').href = `edit.html?id=${data.id}`;

            document.getElementById('loading').classList.add('d-none');
            document.getElementById('detail-content').classList.remove('d-none');
        })
        .catch(err => {
            alert("Lỗi tải dữ liệu: " + err.message);
            window.location.href = 'index.html';
        });
});
