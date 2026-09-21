document.addEventListener('DOMContentLoaded', () => {
    const API_URL = 'http://localhost:8080/admin/api/rooms/json';
    const tbody = document.getElementById('table-body');
    const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount) + ' đ';

    const renderStatus = (maTrangThai) => {
        switch(maTrangThai) {
            case 1: return `<span class="status-badge status-1">Đang Trống</span>`;
            case 2: return `<span class="status-badge status-2">Đã Đặt</span>`;
            case 3: return `<span class="status-badge status-3">Bảo Trì</span>`;
            default: return `<span class="status-badge bg-secondary text-white">Unknown</span>`;
        }
    };

    const mockRooms = [
        { id: 1, name: 'Standard Room 101', price: 800000, imageUrl: 'Deluxe1.jpg', maTrangThai: 1 },
        { id: 2, name: 'Standard Room 102', price: 800000, imageUrl: 'Deluxe2.jpg', maTrangThai: 1 },
        { id: 3, name: 'Superior Room 201', price: 1200000, imageUrl: 'Deluxe3.jpg', maTrangThai: 1 },
        { id: 4, name: 'Superior Room 202', price: 1200000, imageUrl: 'Deluxe4.jpg', maTrangThai: 2 },
        { id: 5, name: 'Deluxe Room 301', price: 1800000, imageUrl: 'Deluxe5.jpg', maTrangThai: 3 },
        { id: 6, name: 'Suite Presidential 401', price: 3500000, imageUrl: 'Deluxe6.jpg', maTrangThai: 1 }
    ];

    const data = mockRooms;
    
    if(!data || data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Không có dữ liệu phòng.</td></tr>`;
        return;
    }

    let html = '';
    data.forEach(p => {
        let imgUrl = p.imageUrl ? (p.imageUrl.startsWith("http") ? p.imageUrl : `../../images/${p.imageUrl}`) : 'https://via.placeholder.com/80';
        
        html += `
            <tr>
                <td class="text-center fw-bold text-secondary">#${p.id}</td>
                <td class="text-center">
                    <img src="${imgUrl}" alt="${p.name}" class="rounded" style="width: 80px; height: 60px; object-fit: cover; border: 1px solid #ddd;">
                </td>
                <td class="text-center fw-bold" style="color: #0F2942;">${p.name}</td>
                <td class="text-center text-danger fw-bold">${formatCurrency(p.price)}</td>
                <td class="text-center">${renderStatus(p.maTrangThai)}</td>
                <td class="text-center">
                    <a href="details.html?id=${p.id}" class="btn btn-sm btn-info text-white shadow-sm" title="Chi tiết"><i class="bi bi-eye"></i></a>
                    <a href="edit.html?id=${p.id}" class="btn btn-sm btn-warning text-white shadow-sm mx-1" title="Sửa"><i class="bi bi-pencil"></i></a>
                    <a href="delete.html?id=${p.id}" class="btn btn-sm btn-danger shadow-sm ms-1" title="Xóa"><i class="bi bi-trash"></i></a>
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
});
