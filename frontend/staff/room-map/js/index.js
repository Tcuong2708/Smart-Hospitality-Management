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
    const container = document.getElementById('room-map-container');
    const spinner = document.getElementById('loading-spinner');
    
    spinner.style.display = 'none';

    if (!mockRooms || mockRooms.length === 0) {
        container.innerHTML = `<div class="w-100 text-center py-5 text-muted">Không có dữ liệu phòng.</div>`;
        return;
    }

    // Nhóm phòng theo tầng
    const floors = {};
    mockRooms.forEach(room => {
        const floorNum = Math.floor(room.id / 100);
        if(!floors[floorNum]) {
            floors[floorNum] = [];
        }
        floors[floorNum].push(room);
    });

    let html = '';
    
    Object.keys(floors).sort((a,b) => a-b).forEach(floorNum => {
        html += `
            <div class="floor-section mb-4">
                <h4 class="fw-bold text-navy mb-3 pb-2 border-bottom border-warning border-2" style="color: var(--navy-color);">Tầng ${floorNum}</h4>
                <div class="room-grid" style="padding: 10px 0;">
        `;
        
        floors[floorNum].forEach(room => {
            let statusClass = '';
            let actionHtml = '';

            if (room.maTrangThai === 1) { // Trống
                statusClass = 'status-empty';
                actionHtml = `
                    <a href="../../admin/check-in/index.html?roomId=${room.id}" class="btn btn-success btn-action py-1 w-100 mb-1">
                        <i class="bi bi-box-arrow-in-right me-1"></i>Check-In
                    </a>
                `;
            } else if (room.maTrangThai === 2) { // Đang ở
                statusClass = 'status-occupied';
                actionHtml = `
                    <div class="d-flex gap-1 mt-2">
                        <a href="../../admin/check-out/index.html?roomId=${room.id}" class="btn btn-danger btn-action py-1 flex-grow-1" style="font-size: 0.8rem;">
                            <i class="bi bi-box-arrow-left me-1"></i>Check-Out
                        </a>
                        <button class="btn btn-info text-white btn-action py-1 btn-transfer" style="font-size: 0.8rem;"
                            data-id="${room.id}" data-type="${room.maLoai}" data-price="${room.price}">
                            <i class="bi bi-arrow-left-right"></i> Đổi
                        </button>
                    </div>
                `;
            } else { // Chờ dọn dẹp
                statusClass = 'status-dirty';
                actionHtml = `
                    <button class="btn btn-warning text-dark btn-action py-1 w-100" disabled>
                        <i class="bi bi-hourglass-split me-1"></i>Chờ dọn...
                    </button>
                `;
            }

            const priceFormatted = new Intl.NumberFormat('vi-VN').format(room.price || 0) + ' VNĐ';
            
            html += `
            <div class="room-card p-3 d-flex flex-column justify-content-between ${statusClass}">
                <div>
                    <div class="d-flex justify-content-between align-items-start">
                        <h5 class="fw-bold text-dark m-0">Phòng ${room.id}</h5>
                        <span class="badge bg-light text-dark border fw-bold">${room.maLoai || 'Loại 1'}</span>
                    </div>
                    <p class="text-muted small mt-1 mb-2">${priceFormatted}</p>
                </div>
                <div>
                    ${actionHtml}
                </div>
            </div>
            `;
        });
        
        html += `
                </div>
            </div>
        `;
    });

    container.innerHTML = html;

    // --- Transfer Room Logic ---
    const formatMoney = (val) => new Intl.NumberFormat('vi-VN').format(val) + ' đ';
    let transferModalInstance = null;
    let currentTransferRoomPrice = 0;
    const newRoomSelect = document.getElementById('new-room-select');
    const transferReason = document.getElementById('transfer-reason');
    const otherReasonContainer = document.getElementById('other-reason-container');
    const newPriceDisp = document.getElementById('new-price-disp');
    const diffDisp = document.getElementById('price-diff-disp');

    // Mở modal
    document.querySelectorAll('.btn-transfer').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const btnTarget = e.currentTarget;
            const rId = btnTarget.dataset.id;
            const rType = btnTarget.dataset.type;
            currentTransferRoomPrice = parseInt(btnTarget.dataset.price) || 0;

            document.getElementById('current-room-id').textContent = rId;
            document.getElementById('current-room-type').textContent = rType;
            document.getElementById('current-price-disp').textContent = formatMoney(currentTransferRoomPrice);
            newPriceDisp.textContent = '0 đ';
            diffDisp.textContent = '0 đ';
            diffDisp.className = 'fw-bold text-dark fs-5';

            // Đổ danh sách phòng trống
            newRoomSelect.innerHTML = '<option value="">-- Danh sách phòng trống --</option>';
            mockRooms.filter(r => r.maTrangThai === 1).forEach(r => {
                const opt = document.createElement('option');
                opt.value = r.price;
                opt.textContent = `Phòng ${r.id} (${r.maLoai}) - ${formatMoney(r.price)}`;
                newRoomSelect.appendChild(opt);
            });

            transferReason.value = 'Khách yêu cầu nâng hạng';
            otherReasonContainer.style.display = 'none';

            if (!transferModalInstance) {
                transferModalInstance = new bootstrap.Modal(document.getElementById('transferRoomModal'));
            }
            transferModalInstance.show();
        });
    });

    // Bắt sự kiện chọn phòng mới để tính tiền chênh lệch
    newRoomSelect.addEventListener('change', (e) => {
        const newPrice = parseInt(e.target.value) || 0;
        if(newPrice === 0) {
            newPriceDisp.textContent = '0 đ';
            diffDisp.textContent = '0 đ';
            return;
        }

        newPriceDisp.textContent = formatMoney(newPrice);
        const diff = newPrice - currentTransferRoomPrice;
        
        diffDisp.textContent = formatMoney(Math.abs(diff)) + (diff > 0 ? ' (Thu thêm)' : (diff < 0 ? ' (Hoàn lại)' : ''));
        diffDisp.className = 'fw-bold fs-5 ' + (diff > 0 ? 'text-danger' : (diff < 0 ? 'text-success' : 'text-dark'));
    });

    transferReason.addEventListener('change', (e) => {
        if(e.target.value === 'Lý do khác') {
            otherReasonContainer.style.display = 'block';
        } else {
            otherReasonContainer.style.display = 'none';
        }
    });

    document.getElementById('btn-confirm-transfer').addEventListener('click', () => {
        if(!newRoomSelect.value) {
            alert('Vui lòng chọn phòng mới!');
            return;
        }
        transferModalInstance.hide();
        alert('Đã thực hiện chuyển phòng thành công. Hệ thống đã lưu lại giao dịch và chênh lệch!');
    });
}
