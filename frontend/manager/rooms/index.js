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
        { id: 'PH101', name: 'Standard Room 101', category: 'Standard', price: 800000, imageUrl: 'Deluxe1.jpg', maTrangThai: 1, desc: 'Phòng tiêu chuẩn 1 giường đơn', note: '' },
        { id: 'PH102', name: 'Standard Room 102', category: 'Standard', price: 800000, imageUrl: 'Deluxe2.jpg', maTrangThai: 1, desc: 'Phòng tiêu chuẩn 1 giường đơn', note: '' },
        { id: 'PH201', name: 'Superior Room 201', category: 'Superior', price: 1200000, imageUrl: 'Deluxe3.jpg', maTrangThai: 1, desc: 'Phòng cao cấp 1 giường đôi', note: 'View biển' },
        { id: 'PH202', name: 'Superior Room 202', category: 'Superior', price: 1200000, imageUrl: 'Deluxe4.jpg', maTrangThai: 2, desc: 'Phòng cao cấp 1 giường đôi', note: '' },
        { id: 'PH301', name: 'Deluxe Room 301', category: 'Deluxe', price: 1800000, imageUrl: 'Deluxe5.jpg', maTrangThai: 3, desc: 'Phòng Deluxe 2 giường đôi', note: 'Đang sửa ống nước' },
        { id: 'PH401', name: 'Suite Presidential 401', category: 'Suite', price: 3500000, imageUrl: 'Deluxe6.jpg', maTrangThai: 1, desc: 'Phòng Tổng thống', note: 'VIP' }
    ];

    const data = mockRooms;
    
    if(!data || data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Không có dữ liệu phòng.</td></tr>`;
        return;
    }

    let html = '';
    data.forEach(p => {
        let imgUrl = p.imageUrl ? (p.imageUrl.startsWith("http") ? p.imageUrl : `../../images/${p.imageUrl}`) : 'https://via.placeholder.com/80';
        
        html += `
            <tr>
                <td class="text-center fw-bold text-secondary">${p.id}</td>
                <td class="text-center">
                    <img src="${imgUrl}" alt="${p.name}" class="rounded" style="width: 80px; height: 60px; object-fit: cover; border: 1px solid #ddd;">
                </td>
                <td class="text-center fw-bold" style="color: #0F2942;">${p.name}</td>
                <td class="text-center"><span class="badge bg-secondary">${p.category}</span></td>
                <td class="text-center text-danger fw-bold">${formatCurrency(p.price)}</td>
                <td class="text-center">${renderStatus(p.maTrangThai)}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-info text-white shadow-sm btn-view" data-id="${p.id}" title="Chi tiết"><i class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-warning text-white shadow-sm mx-1 btn-edit" data-id="${p.id}" title="Sửa"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-danger shadow-sm ms-1 btn-delete" data-id="${p.id}" title="Xóa"><i class="bi bi-trash"></i></button>
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;

    // Events
    document.querySelectorAll('.btn-edit').forEach(btn => {
        btn.addEventListener('click', () => {
            new bootstrap.Modal(document.getElementById('roomModal')).show();
        });
    });
    
    document.querySelectorAll('.btn-delete').forEach(btn => {
        btn.addEventListener('click', () => {
            if(confirm('Bạn có chắc chắn muốn xóa phòng này?')) alert('Đã xóa!');
        });
    });
    
    document.querySelectorAll('.btn-view').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = e.currentTarget.dataset.id;
            const room = mockRooms.find(r => r.id === id);
            if(room) {
                alert(`Mã: ${room.id}\nTên: ${room.name}\nLoại: ${room.category}\nMô tả: ${room.desc}\nGhi chú: ${room.note}`);
            }
        });
    });
});
