document.addEventListener('DOMContentLoaded', () => {
    
    // Mock Data cho Quản lý Người dùng
    const mockUsers = [
        { idTaiKhoan: 1, tenDangNhap: 'admin_master', hoTen: 'Nguyễn Quản Trị', quocTich: 'Việt Nam', roleID: 1, trangThai: 1 },
        { idTaiKhoan: 2, tenDangNhap: 'staff_luna', hoTen: 'Trần Lễ Tân', quocTich: 'Việt Nam', roleID: 2, trangThai: 1 },
        { idTaiKhoan: 3, tenDangNhap: 'user_john', hoTen: 'John Doe', quocTich: 'Mỹ (USA)', roleID: 3, trangThai: 1 },
        { idTaiKhoan: 4, tenDangNhap: 'user_maria', hoTen: 'Maria Ozawa', quocTich: 'Nhật Bản', roleID: 3, trangThai: 0 },
        { idTaiKhoan: 5, tenDangNhap: 'nguyenvan_a', hoTen: 'Nguyễn Văn A', quocTich: 'Việt Nam', roleID: 3, trangThai: 1 }
    ];

    const tableBody = document.getElementById('user-table-body');

    // Hàm Render Bảng User
    function renderUsers(users) {
        if (!tableBody) return;
        tableBody.innerHTML = '';

        if (users.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-5 text-muted">
                        <i class="bi bi-inbox fs-1 d-block mb-2 opacity-25"></i>
                        Chưa có dữ liệu người dùng.
                    </td>
                </tr>
            `;
            return;
        }

        users.forEach(u => {
            
            // Xử lý huy hiệu vai trò
            let roleBadge = '';
            if (u.roleID === 1) {
                roleBadge = '<span class="badge rounded-pill bg-danger px-3 py-2 small">ADMIN</span>';
            } else if (u.roleID === 2) {
                roleBadge = '<span class="badge rounded-pill bg-primary px-3 py-2 small">Nhân viên</span>';
            } else {
                roleBadge = '<span class="badge rounded-pill bg-secondary px-3 py-2 small">Khách hàng</span>';
            }

            // Xử lý trạng thái
            let statusBadge = '';
            let lockIcon = '';
            let lockTitle = '';
            let lockBtnClass = '';
            if (u.trangThai === 1) {
                statusBadge = '<span class="badge bg-success rounded-pill px-3 py-2 text-white small fw-bold">Hoạt động</span>';
                lockIcon = 'bi-lock-fill';
                lockTitle = 'Khóa tài khoản';
                lockBtnClass = 'btn-dark';
            } else {
                statusBadge = '<span class="badge bg-secondary rounded-pill px-3 py-2 text-white small fw-bold">Bị khóa</span>';
                lockIcon = 'bi-unlock-fill';
                lockTitle = 'Mở khóa tài khoản';
                lockBtnClass = 'btn-success';
            }

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold" style="color: var(--navy-color, #0F2942);">
                    <i class="bi bi-person-badge me-2 text-warning opacity-75"></i>
                    <span>${u.tenDangNhap}</span>
                </td>
                <td class="text-center">${u.hoTen}</td>
                <td class="text-center"><span>${u.quocTich}</span></td>
                <td class="text-center">${roleBadge}</td>
                <td class="text-center">${statusBadge}</td>
                <td class="text-center">
                    <a href="details.html" class="btn btn-sm btn-info text-white shadow-sm" title="Xem chi tiết">
                        <i class="bi bi-eye"></i>
                    </a>
                    <a href="edit.html" class="btn btn-sm btn-warning text-white shadow-sm mx-1" title="Sửa thông tin">
                        <i class="bi bi-pencil"></i>
                    </a>
                    <button class="btn btn-sm ${lockBtnClass} shadow-sm toggle-status-btn" title="${lockTitle}">
                        <i class="bi ${lockIcon}"></i>
                    </button>
                    <button class="btn btn-sm btn-danger shadow-sm ms-1 delete-btn" title="Xóa">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        // Add event listeners for toggle and delete
        document.querySelectorAll('.toggle-status-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                if(confirm('Bạn có chắc chắn muốn thay đổi trạng thái hoạt động của tài khoản này không?')) {
                    alert('Trạng thái tài khoản đã được thay đổi (Mock).');
                    window.location.reload();
                }
            });
        });

        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                if(confirm('Bạn có chắc chắn muốn xóa tài khoản này không?')) {
                    alert('Đã xóa tài khoản (Mock).');
                    window.location.reload();
                }
            });
        });
    }

    // Call render
    if(tableBody) {
        renderUsers(mockUsers);
    }
});
