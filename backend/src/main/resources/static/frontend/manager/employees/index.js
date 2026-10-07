document.addEventListener('DOMContentLoaded', () => {
    const tbody = document.getElementById('table-body');
    const searchInput = document.getElementById('search-input');
    const thFilterDept = document.getElementById('thFilterDept');
    
    const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount) + ' đ';

    const mockEmployees = [
        { id: 'NV01', name: 'Nguyễn Văn Nam', dept: 'Lễ tân', phone: '0912345678', email: 'nam.nv@mayhotel.com', salary: 7000000, status: 1, note: '' },
        { id: 'NV02', name: 'Trần Thị Mai', dept: 'Lễ tân', phone: '0987654321', email: 'mai.tt@mayhotel.com', salary: 7000000, status: 1, note: '' },
        { id: 'NV03', name: 'Lê Hoàng Anh', dept: 'Buồng phòng', phone: '0909123456', email: 'anh.lh@mayhotel.com', salary: 6000000, status: 1, note: '' },
        { id: 'NV04', name: 'Phạm Văn Quyết', dept: 'Bảo vệ', phone: '0911223344', email: 'quyet.pv@mayhotel.com', salary: 6500000, status: 2, note: 'Nghỉ phép' },
        { id: 'NV05', name: 'Hoàng Thị Yến', dept: 'Kế toán', phone: '0988776655', email: 'yen.ht@mayhotel.com', salary: 12000000, status: 1, note: '' }
    ];

    const renderStatus = (status) => {
        switch(status) {
            case 1: return `<span class="badge bg-success rounded-pill px-3">Đang làm việc</span>`;
            case 2: return `<span class="badge bg-warning text-dark rounded-pill px-3">Nghỉ phép</span>`;
            case 0: return `<span class="badge bg-danger rounded-pill px-3">Đã nghỉ việc</span>`;
            default: return `<span class="badge bg-secondary rounded-pill px-3">Unknown</span>`;
        }
    };

    function renderTable(dataToRender) {
        if(!dataToRender || dataToRender.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-5 text-muted">Không tìm thấy dữ liệu.</td></tr>`;
            return;
        }

        let html = '';
        dataToRender.forEach(nv => {
            html += `
                <tr>
                    <td class="text-center fw-bold text-secondary">${nv.id}</td>
                    <td class="fw-bold" style="color: #0F2942;">
                        <i class="bi bi-person-circle me-2 text-muted fs-5 align-middle"></i>${nv.name}
                    </td>
                    <td class="text-center"><span class="badge bg-info text-dark border">${nv.dept}</span></td>
                    <td class="text-center">
                        <div class="small fw-bold">${nv.phone}</div>
                        <div class="small text-muted">${nv.email}</div>
                    </td>
                    <td class="text-center text-danger fw-bold">${formatCurrency(nv.salary)}</td>
                    <td class="text-center">${renderStatus(nv.status)}</td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-warning text-white shadow-sm btn-edit" data-id="${nv.id}" title="Sửa"><i class="bi bi-pencil"></i></button>
                        <button class="btn btn-sm btn-secondary shadow-sm ms-1 btn-lock" data-id="${nv.id}" title="Khóa tài khoản"><i class="bi bi-lock-fill"></i></button>
                        <button class="btn btn-sm btn-danger shadow-sm ms-1 btn-delete" data-id="${nv.id}" title="Xóa"><i class="bi bi-trash"></i></button>
                    </td>
                </tr>
            `;
        });
        tbody.innerHTML = html;
        bindTableEvents();
    }

    // Render checkbox list for Department filter
    if (thFilterDept) {
        const depts = [...new Set(mockEmployees.map(r => r.dept))];
        thFilterDept.innerHTML = depts.map((d, idx) => `
            <li>
                <div class="form-check mb-1 ms-2">
                    <input class="form-check-input th-cb-dept" type="checkbox" value="${d}" id="th_dept_${idx}">
                    <label class="form-check-label" for="th_dept_${idx}">${d}</label>
                </div>
            </li>
        `).join('');
    }

    // Filter Logic
    document.querySelectorAll('.th-cb-dept').forEach(cb => {
        cb.addEventListener('change', applyFilters);
    });
    
    if (searchInput) {
        searchInput.addEventListener('input', applyFilters);
    }

    function applyFilters() {
        const keyword = (searchInput.value || '').toLowerCase().trim();
        const selectedDepts = Array.from(document.querySelectorAll('.th-cb-dept:checked')).map(cb => cb.value);

        const filtered = mockEmployees.filter(nv => {
            const matchKeyword = nv.name.toLowerCase().includes(keyword) || nv.dept.toLowerCase().includes(keyword) || nv.phone.includes(keyword);
            const matchDept = selectedDepts.length === 0 || selectedDepts.includes(nv.dept);
            return matchKeyword && matchDept;
        });
        renderTable(filtered);
    }

    // Initial render
    renderTable(mockEmployees);

    function bindTableEvents() {
        document.querySelectorAll('.btn-edit').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.dataset.id;
                const emp = mockEmployees.find(r => r.id === id);
                if (emp) {
                    document.getElementById('employeeModalTitle').innerText = 'Cập Nhật Nhân Viên';
                    document.getElementById('form-name').value = emp.name;
                    document.getElementById('form-phone').value = emp.phone;
                    document.getElementById('form-email').value = emp.email || '';
                    document.getElementById('form-dept').value = emp.dept;
                    document.getElementById('form-salary').value = emp.salary;
                    document.getElementById('form-note').value = emp.note || '';
                    
                    new bootstrap.Modal(document.getElementById('employeeModal')).show();
                }
            });
        });

        document.querySelectorAll('.btn-lock').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.dataset.id;
                if(confirm('Bạn có chắc chắn muốn KHÓA tài khoản của nhân viên [' + id + '] này không?')) {
                    alert('Thành công! Tài khoản nhân viên đã bị khóa, không thể đăng nhập vào hệ thống.');
                }
            });
        });

        document.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.dataset.id;
                // Giả lập Dòng sự kiện phụ: Từ chối xóa vật lý nếu nhân viên đang phụ trách giao dịch
                if(id === 'NV01' || id === 'NV02') {
                    alert('LỖI: Nhân viên này đang phụ trách hóa đơn chưa thanh toán hoặc phiếu đặt phòng đang xử lý!\n\nHệ thống từ chối xóa vật lý. Vui lòng sử dụng tính năng "Khóa tài khoản" để đảm bảo tính toàn vẹn dữ liệu.');
                } else {
                    if(confirm('Bạn có chắc chắn muốn xóa vĩnh viễn nhân viên này khỏi hệ thống?')) {
                        alert('Đã xóa thành công hồ sơ nhân viên!');
                    }
                }
            });
        });

        // Bắt sự kiện Lưu (Thêm/Sửa)
        document.getElementById('btn-save-employee')?.addEventListener('click', () => {
            const phone = document.getElementById('form-phone').value;
            // Giả lập Dòng sự kiện phụ: Kiểm tra trùng lặp
            if(phone === '0912345678') {
                alert('LỖI: Số điện thoại này đã tồn tại trên hệ thống. Vui lòng kiểm tra lại!');
                return;
            }
            alert('Lưu thông tin nhân viên thành công!');
            bootstrap.Modal.getInstance(document.getElementById('employeeModal')).hide();
        });
    }

    // Reset Modal
    const employeeModal = document.getElementById('employeeModal');
    if (employeeModal) {
        employeeModal.addEventListener('hidden.bs.modal', () => {
            document.getElementById('employeeForm').reset();
            document.getElementById('employeeModalTitle').innerText = 'Thêm Nhân Viên';
        });
    }
});
