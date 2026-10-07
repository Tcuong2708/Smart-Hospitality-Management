document.addEventListener('DOMContentLoaded', () => {
    
    // Mock data
    let mockBackups = [
        { id: 1, filename: 'backup_db_20261005_2359.gz', date: '05/10/2026 23:59', creator: 'System Auto', size: '16.2 MB' },
        { id: 2, filename: 'backup_db_20260920_2359.gz', date: '20/09/2026 23:59', creator: 'System Auto', size: '15.4 MB' },
        { id: 3, filename: 'backup_db_20260915_1530.gz', date: '15/09/2026 15:30', creator: 'Admin', size: '14.8 MB' },
        { id: 4, filename: 'backup_db_20260910_2359.gz', date: '10/09/2026 23:59', creator: 'System Auto', size: '13.2 MB' }
    ];

    const tableBody = document.getElementById('backup-table-body');
    let restoreModalInstance = null;
    let selectedFile = '';

    function showToastMsg(msg, type = 'success') {
        if (window.showToast) {
            window.showToast(msg, type);
        } else {
            alert(msg);
        }
    }

    function renderBackups() {
        if (!tableBody) return;
        tableBody.innerHTML = '';

        if (mockBackups.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">Chưa có bản sao lưu nào.</td></tr>`;
            return;
        }

        mockBackups.forEach(b => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="ps-4 fw-bold text-navy"><i class="bi bi-file-earmark-zip-fill me-2 text-gold"></i>${b.filename}</td>
                <td class="text-center">${b.date}</td>
                <td class="text-center"><span class="badge ${b.creator === 'Admin' ? 'bg-primary' : 'bg-secondary'}">${b.creator}</span></td>
                <td class="text-center text-muted">${b.size}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-primary me-1 btn-download-file" data-filename="${b.filename}" title="Tải xuống"><i class="bi bi-download"></i> Tải về</button>
                    <button class="btn btn-sm btn-danger me-1 btn-restore" data-filename="${b.filename}"><i class="bi bi-arrow-counterclockwise"></i> Phục hồi</button>
                    <button class="btn btn-sm btn-outline-danger btn-delete-backup" data-id="${b.id}" title="Xóa bản sao lưu"><i class="bi bi-trash"></i></button>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        // Bắt sự kiện tải xuống
        document.querySelectorAll('.btn-download-file').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const fname = e.currentTarget.dataset.filename;
                showToastMsg(`Đang tải file ${fname} về máy tính...`, 'success');
            });
        });

        // Bắt sự kiện xóa
        document.querySelectorAll('.btn-delete-backup').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = parseInt(e.currentTarget.dataset.id);
                if (confirm('Bạn có chắc chắn muốn xóa bản sao lưu này?')) {
                    mockBackups = mockBackups.filter(b => b.id !== id);
                    renderBackups();
                    showToastMsg('Xóa bản sao lưu thành công!', 'success');
                }
            });
        });

        // Bắt sự kiện phục hồi
        document.querySelectorAll('.btn-restore').forEach(btn => {
            btn.addEventListener('click', (e) => {
                selectedFile = e.currentTarget.dataset.filename;
                document.getElementById('restore-file-name').textContent = selectedFile;
                document.getElementById('admin-password').value = '';
                document.getElementById('restore-progress-container').classList.add('d-none');
                document.getElementById('restore-progress-bar').style.width = '0%';
                document.getElementById('restore-progress-bar').textContent = '0%';
                document.getElementById('btn-confirm-restore').disabled = false;

                if (!restoreModalInstance) {
                    restoreModalInstance = new bootstrap.Modal(document.getElementById('restoreModal'));
                }
                restoreModalInstance.show();
            });
        });
    }

    renderBackups();

    // Xử lý tạo sao lưu mới
    document.getElementById('btn-backup-now').addEventListener('click', () => {
        const btn = document.getElementById('btn-backup-now');
        const originalText = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Đang nén dữ liệu...';
        btn.disabled = true;

        setTimeout(() => {
            const now = new Date();
            const dateStr = now.toLocaleDateString('vi-VN') + ' ' + now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
            const nameStr = `backup_db_${now.getFullYear()}${(now.getMonth() + 1).toString().padStart(2, '0')}${now.getDate().toString().padStart(2, '0')}_${now.getHours()}${now.getMinutes()}.gz`;

            mockBackups.unshift({
                id: Date.now(),
                filename: nameStr,
                date: dateStr,
                creator: 'Admin',
                size: '16.5 MB'
            });

            renderBackups();
            btn.innerHTML = originalText;
            btn.disabled = false;

            showToastMsg(`Tạo bản sao lưu ${nameStr} thành công!`, 'success');
        }, 1500);
    });

    // Xử lý xác nhận khôi phục với Progress Bar
    document.getElementById('btn-confirm-restore').addEventListener('click', () => {
        const pwd = document.getElementById('admin-password').value;
        if (!pwd) {
            alert('Vui lòng nhập mật khẩu quản trị viên!');
            return;
        }

        const progressContainer = document.getElementById('restore-progress-container');
        const progressBar = document.getElementById('restore-progress-bar');
        const btnConfirm = document.getElementById('btn-confirm-restore');
        const btnCancel = document.getElementById('btn-cancel-restore');

        progressContainer.classList.remove('d-none');
        btnConfirm.disabled = true;
        btnCancel.disabled = true;

        let progress = 0;
        const interval = setInterval(() => {
            progress += 20;
            progressBar.style.width = `${progress}%`;
            progressBar.textContent = `${progress}%`;

            if (progress >= 100) {
                clearInterval(interval);
                setTimeout(() => {
                    restoreModalInstance.hide();
                    btnConfirm.disabled = false;
                    btnCancel.disabled = false;
                    showToastMsg(`Khôi phục thành công từ file ${selectedFile}! Hệ thống hoạt động bình thường.`, 'success');
                }, 500);
            }
        }, 300);
    });

    // Xử lý lưu cấu hình Lên lịch tự động
    const btnSaveSchedule = document.getElementById('btn-save-schedule');
    if (btnSaveSchedule) {
        btnSaveSchedule.addEventListener('click', () => {
            const isEnabled = document.getElementById('auto-backup-toggle').checked;
            const freq = document.getElementById('backup-frequency').value;
            const time = document.getElementById('backup-time').value;
            if (isEnabled) {
                showToastMsg(`Đã bật sao lưu tự động (${freq} lúc ${time}).`, 'success');
            } else {
                showToastMsg('Đã tắt tính năng sao lưu tự động.', 'warning');
            }
        });
    }

    // Xử lý Tải file sao lưu từ máy tính
    const btnSubmitUpload = document.getElementById('btn-submit-upload');
    if (btnSubmitUpload) {
        btnSubmitUpload.addEventListener('click', () => {
            const fileInput = document.getElementById('backup-file-input');
            if (!fileInput || !fileInput.files.length) {
                alert('Vui lòng chọn một file sao lưu hợp lệ!');
                return;
            }

            const uploadedFile = fileInput.files[0];
            const uploadModalEl = document.getElementById('uploadModal');
            const uploadModalInstance = bootstrap.Modal.getInstance(uploadModalEl);
            if (uploadModalInstance) uploadModalInstance.hide();

            const now = new Date();
            const dateStr = now.toLocaleDateString('vi-VN') + ' ' + now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

            mockBackups.unshift({
                id: Date.now(),
                filename: uploadedFile.name,
                date: dateStr,
                creator: 'Admin (Upload)',
                size: (uploadedFile.size / (1024 * 1024)).toFixed(1) + ' MB'
            });

            renderBackups();
            showToastMsg(`Đã tải lên và lưu file sao lưu: ${uploadedFile.name}`, 'success');
        });
    }

});
