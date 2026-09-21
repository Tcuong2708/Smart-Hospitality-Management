document.addEventListener('DOMContentLoaded', () => {
    const mockData = [
        { tier: 'Hạng Đồng (Bronze)', points: '0 - 999', perks: 'Tích điểm cơ bản', status: 'Áp dụng', badgeColor: 'bg-secondary' },
        { tier: 'Hạng Bạc (Silver)', points: '1000 - 4999', perks: 'Giảm 5% hóa đơn phòng, Nước uống Welcome', status: 'Áp dụng', badgeColor: 'bg-info' },
        { tier: 'Hạng Vàng (Gold)', points: '5000 - 9999', perks: 'Giảm 10%, Trả phòng muộn 14:00', status: 'Áp dụng', badgeColor: 'bg-warning text-dark' },
        { tier: 'Hạng Kim Cương (Diamond)', points: '10000+', perks: 'Giảm 15%, Đưa đón sân bay miễn phí', status: 'Áp dụng', badgeColor: 'bg-primary' }
    ];
    
    const tableBody = document.getElementById('table-body');
    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold"><span class="badge ${item.badgeColor} fs-6 shadow-sm">${item.tier}</span></td>
                <td class="text-center fw-bold text-danger">${item.points}</td>
                <td class="text-start">${item.perks}</td>
                <td class="text-center"><span class="badge bg-success px-3 py-2 rounded-pill shadow-sm">${item.status}</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-warning text-white shadow-sm mx-1" title="Chỉnh sửa"><i class="bi bi-pencil"></i></button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    }
    renderData(mockData);
});
