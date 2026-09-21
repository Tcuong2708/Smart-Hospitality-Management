document.addEventListener('DOMContentLoaded', () => {
    const mockData = [
        { id: 'NV001', name: 'Lê Văn Luyện', role: 'Lễ tân', dept: 'Tiền sảnh', status: 1 },
        { id: 'NV002', name: 'Nguyễn Thị Nở', role: 'Buồng phòng', dept: 'Lưu trú', status: 1 }
    ];
    const tableBody = document.getElementById('table-body');
    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold">${item.id}</td>
                <td class="text-center">${item.name}</td>
                <td class="text-center">${item.role}</td>
                <td class="text-center">${item.dept}</td>
                <td class="text-center"><span class="badge bg-success">Đang làm việc</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-info text-white"><i class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-warning text-white"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-danger text-white"><i class="bi bi-trash"></i></button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    }
    renderData(mockData);
});
