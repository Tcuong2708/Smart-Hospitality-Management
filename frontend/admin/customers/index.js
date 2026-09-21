document.addEventListener('DOMContentLoaded', () => {
    const mockData = [
        { id: 1, name: 'Nguyễn Văn A', email: 'nva@gmail.com', phone: '0901234567', tier: 'Vàng', status: 1 },
        { id: 2, name: 'Trần Thị B', email: 'ttb@gmail.com', phone: '0912345678', tier: 'Bạc', status: 1 }
    ];
    const tableBody = document.getElementById('table-body');
    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold">${item.name}</td>
                <td class="text-center">${item.email}</td>
                <td class="text-center">${item.phone}</td>
                <td class="text-center"><span class="badge bg-warning text-dark">${item.tier}</span></td>
                <td class="text-center"><span class="badge bg-success">Hoạt động</span></td>
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
