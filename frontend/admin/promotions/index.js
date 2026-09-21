document.addEventListener('DOMContentLoaded', () => {
    const mockData = [
        { id: 'SUMMER2026', name: 'Chào Hè Rực Rỡ', discount: '15%', time: '01/06/2026 - 31/08/2026', status: 'Hết hạn' },
        { id: 'TET2027', name: 'Tết Sum Vầy', discount: '20%', time: '15/01/2027 - 15/02/2027', status: 'Sắp diễn ra' },
        { id: 'VIPGUEST', name: 'Tri Ân Khách VIP', discount: '10%', time: 'Không giới hạn', status: 'Đang diễn ra' }
    ];
    
    const tableBody = document.getElementById('table-body');
    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        data.forEach(item => {
            let statusBadge = '';
            if (item.status === 'Đang diễn ra') statusBadge = 'bg-success';
            else if (item.status === 'Hết hạn') statusBadge = 'bg-secondary';
            else statusBadge = 'bg-warning text-dark';
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold" style="color: var(--accent-color);">${item.id}</td>
                <td class="text-center fw-bold" style="color: var(--navy-color);">${item.name}</td>
                <td class="text-center fw-bold text-danger">${item.discount}</td>
                <td class="text-center">${item.time}</td>
                <td class="text-center"><span class="badge ${statusBadge} px-3 py-2 rounded-pill shadow-sm">${item.status}</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-info text-white shadow-sm" title="Xem chi tiết"><i class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-warning text-white shadow-sm mx-1" title="Chỉnh sửa"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-danger text-white shadow-sm" title="Xóa"><i class="bi bi-trash"></i></button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    }
    renderData(mockData);
});
