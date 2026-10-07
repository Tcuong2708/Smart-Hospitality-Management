document.addEventListener('DOMContentLoaded', () => {
    const mockData = [
        { id: 1, name: 'Báo cáo doanh thu tháng 8', type: 'Doanh thu', date: '01/09/2026', creator: 'Kế toán trưởng', status: 1 },
        { id: 2, name: 'Báo cáo công suất phòng', type: 'Công suất', date: '01/09/2026', creator: 'Kế toán viên', status: 1 }
    ];
    const tableBody = document.getElementById('table-body');
    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold">${item.name}</td>
                <td class="text-center">${item.type}</td>
                <td class="text-center">${item.date}</td>
                <td class="text-center">${item.creator}</td>
                <td class="text-center"><span class="badge bg-success">Hoàn tất</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-success text-white"><i class="bi bi-download"></i> Excel</button>
                    <button class="btn btn-sm btn-danger text-white"><i class="bi bi-file-pdf"></i> PDF</button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    }
    renderData(mockData);
});
