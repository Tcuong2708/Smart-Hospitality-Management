document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const roomId = urlParams.get('id');

    if(!roomId) {
        alert("Lỗi: Không có ID phòng!");
        window.location.href = 'index.html';
        return;
    }

    const API_URL = `http://localhost:8080/admin/api/rooms/${roomId}`;
    const btnDelete = document.getElementById('btn-confirm-delete');

    // Fetch name để hiển thị cho thân thiện
    fetch(API_URL)
        .then(res => res.json())
        .then(data => {
            if(data.name) {
                document.getElementById('room-name').textContent = `[${data.name}]`;
            }
        })
        .catch(err => console.log(err));

    btnDelete.addEventListener('click', async () => {
        btnDelete.disabled = true;
        btnDelete.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Đang xóa...';

        try {
            const res = await fetch(API_URL, {
                method: 'DELETE'
            });
            const data = await res.json();

            if(res.ok) {
                alert("Xóa phòng thành công!");
                window.location.href = 'index.html';
            } else {
                alert("Lỗi: " + (data.message || "Không thể xóa phòng do có ràng buộc dữ liệu!"));
                btnDelete.disabled = false;
                btnDelete.innerHTML = '<i class="bi bi-trash-fill me-1"></i> Xóa Vĩnh Viễn';
            }
        } catch(err) {
            alert("Lỗi kết nối tới server!");
            btnDelete.disabled = false;
            btnDelete.innerHTML = '<i class="bi bi-trash-fill me-1"></i> Xóa Vĩnh Viễn';
        }
    });
});
