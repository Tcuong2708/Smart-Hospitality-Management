document.addEventListener('DOMContentLoaded', () => {
    const mockData = [
        { id: 'BK1001', customer: 'Trần Văn X', room: 'P101', checkin: '10/09/2026', checkout: '12/09/2026', status: 'Đã nhận phòng', aiRisk: 'Thấp (10%)' },
        { id: 'BK1002', customer: 'Lê Thị Y', room: 'P202', checkin: '12/09/2026', checkout: '15/09/2026', status: 'Chờ nhận phòng', aiRisk: 'Cao (85%)' },
        { id: 'BK1003', customer: 'Nguyễn Văn Z', room: 'P303', checkin: '15/09/2026', checkout: '18/09/2026', status: 'Chờ nhận phòng', aiRisk: 'Vừa (45%)' }
    ];
    const tableBody = document.getElementById('table-body');
    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        data.forEach(item => {
            let statusBadge = item.status === 'Đã nhận phòng' ? 'bg-success' : 'bg-warning text-dark';
            
            let riskBadge = '';
            if (item.aiRisk.includes('Thấp')) riskBadge = 'bg-success';
            else if (item.aiRisk.includes('Cao')) riskBadge = 'bg-danger';
            else riskBadge = 'bg-warning text-dark';

            let autoCancelBtn = item.aiRisk.includes('Cao') ? `<button class="btn btn-sm btn-outline-danger mt-1 fw-bold" onclick="alert('Đã kích hoạt Hủy tự động cho ${item.id}')" title="Kích hoạt hệ thống AI tự hủy"><i class="bi bi-robot me-1"></i>Hủy tự động</button>` : '';

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold">${item.id}</td>
                <td class="text-center">${item.customer}</td>
                <td class="text-center fw-bold text-navy">${item.room}</td>
                <td class="text-center">${item.checkin}</td>
                <td class="text-center">${item.checkout}</td>
                <td class="text-center"><span class="badge ${statusBadge}">${item.status}</span></td>
                <td class="text-center">
                    <span class="badge ${riskBadge} fs-6 shadow-sm mb-1">${item.aiRisk}</span><br>
                    ${autoCancelBtn}
                </td>
                <td class="text-center">
                    <button class="btn btn-sm btn-info text-white shadow-sm" title="Xem chi tiết"><i class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-warning text-white shadow-sm mx-1" title="Sửa"><i class="bi bi-pencil"></i></button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    }
    renderData(mockData);
});
