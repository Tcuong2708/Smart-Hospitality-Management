const mockRooms = [
    { id: 101, maLoai: 'Standard', price: 500000, maTrangThai: 1 },
    { id: 102, maLoai: 'Standard', price: 500000, maTrangThai: 2 },
    { id: 103, maLoai: 'Standard', price: 500000, maTrangThai: 3 },
    { id: 104, maLoai: 'Standard', price: 500000, maTrangThai: 1 },
    { id: 201, maLoai: 'Deluxe', price: 1000000, maTrangThai: 2 },
    { id: 202, maLoai: 'Deluxe', price: 1000000, maTrangThai: 1 },
    { id: 203, maLoai: 'Deluxe', price: 1000000, maTrangThai: 1 },
    { id: 301, maLoai: 'Suite', price: 2000000, maTrangThai: 3 },
    { id: 302, maLoai: 'Suite', price: 2000000, maTrangThai: 2 },
    { id: 303, maLoai: 'Suite', price: 2000000, maTrangThai: 1 },
];

document.addEventListener('DOMContentLoaded', () => {
    fetchRoomMap();
});

function fetchRoomMap() {
    const grid = document.getElementById('room-grid');
    const spinner = document.getElementById('loading-spinner');
    
    spinner.style.display = 'none';

    if (!mockRooms || mockRooms.length === 0) {
        grid.innerHTML = `<div class="w-100 text-center py-5 text-muted">Không có dữ liệu phòng.</div>`;
        return;
    }

    grid.innerHTML = mockRooms.map(room => {
        let statusClass = '';
        let actionHtml = '';

        if (room.maTrangThai === 1) { // Trống
            statusClass = 'status-empty';
            actionHtml = `
                <a href="../../admin/check-in/index.html?roomId=${room.id}" class="btn btn-success btn-action py-1">
                    <i class="bi bi-box-arrow-in-right me-1"></i>Check-In
                </a>
            `;
        } else if (room.maTrangThai === 2) { // Đang ở
            statusClass = 'status-occupied';
            actionHtml = `
                <a href="../../admin/check-out/index.html?roomId=${room.id}" class="btn btn-danger btn-action py-1">
                    <i class="bi bi-box-arrow-left me-1"></i>Check-Out
                </a>
            `;
        } else { // Chờ dọn dẹp
            statusClass = 'status-dirty';
            actionHtml = `
                <button class="btn btn-warning text-dark btn-action py-1" disabled>
                    <i class="bi bi-hourglass-split me-1"></i>Chờ dọn dẹp...
                </button>
            `;
        }

        const priceFormatted = new Intl.NumberFormat('vi-VN').format(room.price || 0) + ' VNĐ';
        
        return `
        <div class="room-card p-3 d-flex flex-column justify-content-between ${statusClass}">
            <div>
                <div class="d-flex justify-content-between align-items-start">
                    <h5 class="fw-bold text-dark m-0">Phòng ${room.id}</h5>
                    <span class="badge bg-light text-dark border fw-bold">${room.maLoai || 'Loại 1'}</span>
                </div>
                <p class="text-muted small mt-1 mb-2">${priceFormatted}</p>
            </div>
            <div class="mt-3">
                ${actionHtml}
            </div>
        </div>
        `;
    }).join('');
}
