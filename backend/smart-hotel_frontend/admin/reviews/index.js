document.addEventListener('DOMContentLoaded', () => {
    
    // Giả lập dữ liệu đánh giá
    const mockReviews = [
        { id: 101, tenPhong: 'Phòng Deluxe VIP P302', soSao: 5, noiDung: 'Trải nghiệm tuyệt vời. Nhân viên phục vụ rất nhiệt tình!', trangThai: 0 },
        { id: 102, tenPhong: 'Phòng Standard P105', soSao: 4, noiDung: 'Phòng sạch sẽ gọn gàng, cách âm hơi kém một chút nhưng tạm ổn.', trangThai: 1 },
        { id: 103, tenPhong: 'Hồ bơi vô cực', soSao: 5, noiDung: 'Nước cực kỳ sạch, không gian sang trọng.', trangThai: 0 },
        { id: 104, tenPhong: 'Phòng Suite P501', soSao: 2, noiDung: 'Máy lạnh không lạnh, tivi bị mất tín hiệu lúc tối. Thất vọng.', trangThai: 0 },
        { id: 105, tenPhong: 'Phòng Gia đình P204', soSao: 5, noiDung: 'Phù hợp cho gia đình có trẻ em. Giường êm.', trangThai: 1 },
    ];

    const tableBody = document.getElementById('review-table-body');

    function renderReviews(reviews) {
        if (!tableBody) return;
        tableBody.innerHTML = '';

        if (reviews.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-4 text-muted">Hệ thống hiện tại chưa nhận được phản hồi đánh giá nào từ khách hàng.</td>
                </tr>
            `;
            return;
        }

        reviews.forEach(r => {
            // Render số sao
            let starsHtml = '';
            for(let i = 0; i < r.soSao; i++) {
                starsHtml += '<span class="text-warning"><i class="bi bi-star-fill"></i></span> ';
            }

            // Render trạng thái
            let statusBadge = '';
            let actionHtml = '';

            if (r.trangThai === 0) {
                statusBadge = '<span class="badge bg-warning text-dark px-3 py-2 rounded-pill">Chờ kiểm duyệt</span>';
                actionHtml = `
                    <button class="btn btn-sm btn-success text-white shadow-sm fw-bold approve-btn" data-id="${r.id}">
                        <i class="bi bi-check-circle"></i> Duyệt
                    </button>
                    <button class="btn btn-sm btn-danger shadow-sm delete-btn" data-id="${r.id}">
                        <i class="bi bi-trash"></i> Xóa
                    </button>
                `;
            } else {
                statusBadge = '<span class="badge bg-success px-3 py-2 rounded-pill">Đã duyệt hiển thị</span>';
                actionHtml = `
                    <button class="btn btn-sm btn-danger shadow-sm delete-btn" data-id="${r.id}">
                        <i class="bi bi-trash"></i> Xóa
                    </button>
                `;
            }

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold text-muted">#${r.id}</td>
                <td class="text-center fw-bold text-navy">${r.tenPhong}</td>
                <td class="text-center">${starsHtml}</td>
                <td class="text-center text-truncate" style="max-width: 350px;" title="${r.noiDung}">${r.noiDung}</td>
                <td class="text-center">${statusBadge}</td>
                <td class="text-center">
                    <div class="d-flex justify-content-center gap-1">
                        ${actionHtml}
                    </div>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        // Event listeners
        document.querySelectorAll('.approve-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                if(confirm('Duyệt hiển thị bình luận này công khai?')) {
                    alert('Bình luận đã được duyệt (Mock)');
                    window.location.reload();
                }
            });
        });

        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                if(confirm('Xóa bỏ vĩnh viễn bình luận này?')) {
                    alert('Bình luận đã bị xóa (Mock)');
                    window.location.reload();
                }
            });
        });
    }

    if (tableBody) {
        renderReviews(mockReviews);
    }
});
