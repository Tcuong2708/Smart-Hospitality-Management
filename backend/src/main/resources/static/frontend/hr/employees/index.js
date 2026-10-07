document.addEventListener('DOMContentLoaded', () => {
    const mockData = [
        { id: 'NV001', name: 'Lê Văn Luyện', dob: '1995-02-14', gender: 'Nam', phone: '0909123456', role: 'Lễ tân', username: 'letan_luyen', status: 1 },
        { id: 'NV002', name: 'Nguyễn Thị Nở', dob: '1992-08-25', gender: 'Nữ', phone: '0988765432', role: 'Buồng phòng', username: 'buongphong_no', status: 1 },
        { id: 'NV003', name: 'Trần Cường', dob: '1988-11-05', gender: 'Nam', phone: '0912345678', role: 'Quản lý', username: 'admin_master', status: 1 }
    ];

    const tableBody = document.getElementById('table-body');
    
    function formatDate(dateStr) {
        if(!dateStr) return '';
        const d = new Date(dateStr);
        return `${d.getDate().toString().padStart(2,'0')}/${(d.getMonth()+1).toString().padStart(2,'0')}/${d.getFullYear()}`;
    }

    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        if(data.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="9" class="text-center py-4 text-muted">Không tìm thấy nhân viên nào.</td></tr>';
            return;
        }

        data.forEach(item => {
            const tr = document.createElement('tr');
            
            let roleBadge = 'bg-secondary';
            if(item.role === 'Quản lý') roleBadge = 'bg-danger';
            else if(item.role === 'Lễ tân') roleBadge = 'bg-primary';
            else if(item.role === 'Buồng phòng') roleBadge = 'bg-info text-dark';
            else if(item.role === 'Kế toán') roleBadge = 'bg-warning text-dark';

            tr.innerHTML = `
                <td class="text-center fw-bold text-muted">${item.id}</td>
                <td class="text-center fw-bold text-navy">${item.name}</td>
                <td class="text-center">${formatDate(item.dob)}</td>
                <td class="text-center">${item.gender}</td>
                <td class="text-center fw-bold">${item.phone}</td>
                <td class="text-center"><span class="badge ${roleBadge}">${item.role}</span></td>
                <td class="text-center text-muted fst-italic">@${item.username || 'N/A'}</td>
                <td class="text-center"><span class="badge bg-success">Đang làm việc</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-warning text-white btn-edit" data-id="${item.id}" title="Sửa"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-danger text-white btn-delete" title="Xóa"><i class="bi bi-trash"></i></button>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        // Events
        document.querySelectorAll('.btn-edit').forEach(btn => {
            btn.addEventListener('click', () => {
                new bootstrap.Modal(document.getElementById('employeeModal')).show();
            });
        });
        
        document.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', () => {
                if(confirm('Bạn có chắc chắn muốn xóa nhân viên này?')) alert('Đã xóa!');
            });
        });
    }

    // Tìm kiếm
    const searchInput = document.getElementById('search-input');
    if(searchInput) {
        searchInput.addEventListener('input', () => {
            const q = searchInput.value.toLowerCase();
            const filtered = mockData.filter(e => 
                e.id.toLowerCase().includes(q) || 
                e.name.toLowerCase().includes(q) || 
                e.phone.includes(q)
            );
            renderData(filtered);
        });
    }

    renderData(mockData);
});
