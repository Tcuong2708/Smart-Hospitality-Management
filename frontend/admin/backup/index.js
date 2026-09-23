document.addEventListener('DOMContentLoaded', () => {
    
    // Mock data
    let mockBackups = [
        { id: 1, filename: 'backup_db_20260920_2359.gz', date: '20/09/2026 23:59', creator: 'System Auto', size: '15.4 MB' },
        { id: 2, filename: 'backup_db_20260915_1530.gz', date: '15/09/2026 15:30', creator: 'Admin', size: '14.8 MB' },
        { id: 3, filename: 'backup_db_20260910_2359.gz', date: '10/09/2026 23:59', creator: 'System Auto', size: '13.2 MB' }
    ];

    const tableBody = document.getElementById('backup-table-body');
    let restoreModalInstance = null;
    let selectedFile = '';

    function renderBackups() {
        if (!tableBody) return;
        tableBody.innerHTML = '';

        if(mockBackups.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">Chưa có bản sao lưu nào.</td></tr>`;
            return;
        }

        mockBackups.forEach(b => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="ps-4 fw-bold text-navy"><i class="bi bi-file-earmark-zip me-2 text-gold"></i>${b.filename}</td>
                <td class="text-center">${b.date}</td>
                <td class="text-center"><span class="badge ${b.creator === 'Admin' ? 'bg-primary' : 'bg-secondary'}">${b.creator}</span></td>
                <td class="text-center text-muted">${b.size}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-navy me-1" title="Tải xuống"><i class="bi bi-download"></i></button>
                    <button class="btn btn-sm btn-danger btn-restore" data-filename="${b.filename}"><i class="bi bi-arrow-counterclockwise"></i> Phục hồi</button>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        // Add event listeners for restore
        document.querySelectorAll('.btn-restore').forEach(btn => {
            btn.addEventListener('click', (e) => {
                selectedFile = e.currentTarget.dataset.filename;
                document.getElementById('restore-file-name').textContent = selectedFile;
                document.getElementById('admin-password').value = '';
                
                if(!restoreModalInstance) {
                    restoreModalInstance = new bootstrap.Modal(document.getElementById('restoreModal'));
                }
                restoreModalInstance.show();
            });
        });
    }

    renderBackups();

    // Xử lý tạo sao lưu mới
    document.getElementById('btn-backup-now').addEventListener('click', () => {
        // Giả lập load
        const btn = document.getElementById('btn-backup-now');
        const originalText = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Đang nén...';
        btn.disabled = true;

        setTimeout(() => {
            const now = new Date();
            const dateStr = now.toLocaleDateString('vi-VN') + ' ' + now.toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'});
            const nameStr = `backup_db_${now.getFullYear()}${(now.getMonth()+1).toString().padStart(2,'0')}${now.getDate().toString().padStart(2,'0')}_${now.getHours()}${now.getMinutes()}.gz`;
            
            mockBackups.unshift({
                id: Date.now(),
                filename: nameStr,
                date: dateStr,
                creator: 'Admin',
                size: '15.6 MB'
            });

            renderBackups();
            btn.innerHTML = originalText;
            btn.disabled = false;
            
            // Dùng hàm showToast từ layout (nếu có) hoặc alert
            if(window.showToast) {
                window.showToast(`Tạo bản sao lưu ${nameStr} thành công!`, 'success');
            } else {
                alert(`Tạo bản sao lưu ${nameStr} thành công!`);
            }
        }, 1500);
    });

    // Xử lý phục hồi
    document.getElementById('btn-confirm-restore').addEventListener('click', () => {
        const pwd = document.getElementById('admin-password').value;
        if(!pwd) {
            alert('Vui lòng nhập mật khẩu quản trị!');
            return;
        }
        
        restoreModalInstance.hide();
        
        // Dùng hàm showToast từ layout (nếu có) hoặc alert
        if(window.showToast) {
            window.showToast(`Khôi phục thành công từ file ${selectedFile}! Hệ thống đang khởi động lại...`, 'success');
        } else {
            alert(`Khôi phục thành công từ file ${selectedFile}!`);
        }
    });

    // Xử lý auto backup
    document.getElementById('btn-auto-backup').addEventListener('click', () => {
        alert('Chức năng Lên lịch tự động sao lưu đang được bật (Mặc định 00:00 hàng ngày).');
    });

});
