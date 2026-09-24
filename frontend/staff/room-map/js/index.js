const mockRooms = [
    { id: 101, maLoai: 'Standard', price: 500000, capacity: 2, maTrangThai: 1 },
    { id: 102, maLoai: 'Standard', price: 500000, capacity: 2, maTrangThai: 2 },
    { id: 103, maLoai: 'Standard', price: 500000, capacity: 2, maTrangThai: 3 },
    { id: 104, maLoai: 'Standard', price: 500000, capacity: 2, maTrangThai: 1 },
    { id: 201, maLoai: 'Deluxe', price: 1000000, capacity: 3, maTrangThai: 2 },
    { id: 202, maLoai: 'Deluxe', price: 1000000, capacity: 3, maTrangThai: 1 },
    { id: 203, maLoai: 'Deluxe', price: 1000000, capacity: 3, maTrangThai: 1 },
    { id: 301, maLoai: 'Suite', price: 2000000, capacity: 4, maTrangThai: 3 },
    { id: 302, maLoai: 'Suite', price: 2000000, capacity: 4, maTrangThai: 2 },
    { id: 303, maLoai: 'Suite', price: 2000000, capacity: 4, maTrangThai: 1 },
];

document.addEventListener('DOMContentLoaded', () => {
    // Sửa lỗi backdrop che modal
    const modalElement = document.getElementById('transferRoomModal');
    if (modalElement) {
        document.body.appendChild(modalElement);
    }

    initFilters();
    fetchRoomMap(mockRooms);
});

function initFilters() {
    const filterFloorDropdown = document.getElementById('filterFloorDropdown');
    const filterStatusDropdown = document.getElementById('filterStatusDropdown');
    
    // Khởi tạo các Tầng
    if (filterFloorDropdown) {
        const uniqueFloors = [...new Set(mockRooms.map(room => Math.floor(room.id / 100)))].sort((a,b) => a-b);
        filterFloorDropdown.innerHTML = uniqueFloors.map((floor, idx) => `
            <li>
                <div class="form-check mb-1">
                    <input class="form-check-input filter-floor-cb" type="checkbox" value="${floor}" id="cb_floor_${idx}">
                    <label class="form-check-label" for="cb_floor_${idx}">Tầng ${floor}</label>
                </div>
            </li>
        `).join('');
    }

    // Khởi tạo trạng thái
    if (filterStatusDropdown) {
        const statuses = [
            { val: 1, label: 'Trống' },
            { val: 2, label: 'Đang ở (Check-in)' },
            { val: 3, label: 'Chờ dọn dẹp (Check-out)' }
        ];
        filterStatusDropdown.innerHTML = statuses.map((st, idx) => `
            <li>
                <div class="form-check mb-1">
                    <input class="form-check-input filter-status-cb" type="checkbox" value="${st.val}" id="cb_status_${idx}">
                    <label class="form-check-label" for="cb_status_${idx}">${st.label}</label>
                </div>
            </li>
        `).join('');
    }

    // Ngăn chặn dropdown đóng khi click vào checkbox
    if (filterFloorDropdown) filterFloorDropdown.addEventListener('click', e => e.stopPropagation());
    if (filterStatusDropdown) filterStatusDropdown.addEventListener('click', e => e.stopPropagation());

    // Gắn event onChange
    document.querySelectorAll('.filter-floor-cb, .filter-status-cb').forEach(cb => {
        cb.addEventListener('change', applyFilters);
    });
}

function applyFilters() {
    const selectedFloors = Array.from(document.querySelectorAll('.filter-floor-cb:checked')).map(cb => parseInt(cb.value));
    const selectedStatuses = Array.from(document.querySelectorAll('.filter-status-cb:checked')).map(cb => parseInt(cb.value));

    const filtered = mockRooms.filter(room => {
        const floor = Math.floor(room.id / 100);
        const matchFloor = selectedFloors.length === 0 || selectedFloors.includes(floor);
        const matchStatus = selectedStatuses.length === 0 || selectedStatuses.includes(room.maTrangThai);
        return matchFloor && matchStatus;
    });

    fetchRoomMap(filtered);
}

function fetchRoomMap(roomsToRender) {
    const container = document.getElementById('room-map-container');
    const spinner = document.getElementById('loading-spinner');
    
    spinner.style.display = 'none';

    if (!roomsToRender || roomsToRender.length === 0) {
        container.innerHTML = `<div class="w-100 text-center py-5 text-muted">Không tìm thấy phòng nào phù hợp với bộ lọc.</div>`;
        return;
    }

    // Nhóm phòng theo tầng
    const floors = {};
    roomsToRender.forEach(room => {
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

            const capacityStr = `${room.capacity || 2} người`;
            
            html += `
            <div class="room-card p-3 d-flex flex-column justify-content-between ${statusClass}">
                <div>
                    <div class="d-flex justify-content-between align-items-start">
                        <h5 class="fw-bold text-dark m-0">Phòng ${room.id}</h5>
                        <span class="badge bg-light text-dark border fw-bold">${room.maLoai || 'Loại 1'}</span>
                    </div>
                    <p class="text-muted small mt-1 mb-2"><i class="bi bi-people-fill me-1"></i>Tối đa: ${capacityStr}</p>
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
