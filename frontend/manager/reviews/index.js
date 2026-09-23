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
                statusBadge = '<span class="badge bg-warning text-dark px-3 py-2 rounded-pill">Chờ duyệt</span>';
                actionHtml = `
                    <button class="btn btn-sm btn-success text-white shadow-sm fw-bold approve-btn mb-1 w-100" data-id="${r.id}">
                        <i class="bi bi-check-circle"></i> Duyệt
                    </button>
                    <button class="btn btn-sm btn-outline-secondary shadow-sm hide-btn mb-1 w-100" data-id="${r.id}">
                        <i class="bi bi-eye-slash"></i> Ẩn
                    </button>
                    <button class="btn btn-sm btn-outline-navy shadow-sm reply-btn w-100" data-id="${r.id}" data-content="${r.noiDung}">
                        <i class="bi bi-reply"></i> Phản hồi
                    </button>
                `;
            } else if (r.trangThai === 1) {
                statusBadge = '<span class="badge bg-success px-3 py-2 rounded-pill">Công khai</span>';
                actionHtml = `
                    <button class="btn btn-sm btn-outline-secondary shadow-sm hide-btn mb-1 w-100" data-id="${r.id}">
                        <i class="bi bi-eye-slash"></i> Ẩn
                    </button>
                    <button class="btn btn-sm btn-outline-navy shadow-sm reply-btn w-100" data-id="${r.id}" data-content="${r.noiDung}">
                        <i class="bi bi-reply"></i> Phản hồi
                    </button>
                `;
            } else {
                statusBadge = '<span class="badge bg-secondary px-3 py-2 rounded-pill">Nội bộ (Đã ẩn)</span>';
                actionHtml = `
                    <button class="btn btn-sm btn-success text-white shadow-sm fw-bold approve-btn mb-1 w-100" data-id="${r.id}">
                        <i class="bi bi-eye"></i> Hiện lại
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
                    <div class="d-flex flex-column align-items-center">
                        ${actionHtml}
                    </div>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        // Event listeners
        document.querySelectorAll('.approve-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                if(confirm('Chuyển trạng thái hiển thị công khai cho bình luận này?')) {
                    if(window.showToast) window.showToast('Đã cấp phép hiển thị thành công (Mock)', 'success');
                    else alert('Đã cấp phép hiển thị công khai.');
                }
            });
        });

        document.querySelectorAll('.hide-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                if(confirm('Bạn có chắc chắn muốn ẩn bài viết này? (Chỉ hiển thị lưu trữ nội bộ)')) {
                    if(window.showToast) window.showToast('Đã ẩn bài viết. Dữ liệu được bảo lưu nội bộ.', 'success');
                    else alert('Đã ẩn bài viết.');
                }
            });
        });

        let replyModalInstance = null;
        document.querySelectorAll('.reply-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const content = e.currentTarget.dataset.content;
                document.getElementById('reply-review-content').textContent = '"' + content + '"';
                document.getElementById('reply-content').value = '';
                
                if(!replyModalInstance) {
                    replyModalInstance = new bootstrap.Modal(document.getElementById('replyModal'));
                }
                replyModalInstance.show();
            });
        });

        document.getElementById('btn-submit-reply')?.addEventListener('click', () => {
            const reply = document.getElementById('reply-content').value.trim();
            if(!reply) {
                alert('Vui lòng nhập nội dung phản hồi.');
                return;
            }
            if(replyModalInstance) replyModalInstance.hide();
            if(window.showToast) window.showToast('Phản hồi đã được gửi đến khách hàng thành công!', 'success');
            else alert('Phản hồi đã được gửi đến khách hàng thành công!');
        });
    }

    if (tableBody) {
        renderReviews(mockReviews);
    }
});
