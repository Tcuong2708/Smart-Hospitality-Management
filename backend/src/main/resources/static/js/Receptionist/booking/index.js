document.addEventListener('DOMContentLoaded', () => {
    // Sửa lỗi backdrop che modal
    const modalElement = document.getElementById('directBookingModal');
    if (modalElement) {
        document.body.appendChild(modalElement);
    }

    const formatDate = (dateInput) => {
        if (!dateInput) return '';
        const d = new Date(dateInput);
        return d.toLocaleDateString('vi-VN');
    };

    const mockData = window.mockBookings && window.mockBookings.length > 0 ? window.mockBookings.map(b => ({
        id: b.order.id,
        customer: b.customerName,
        room: b.roomName,
        checkin: formatDate(b.order.expectedIn),
        checkout: formatDate(b.order.expectedOut),
        status: b.order.status,
        aiRisk: (b.order.noShowRiskLevel || 'Không xác định') + ' (' + (b.order.noShowProbability != null ? b.order.noShowProbability : '?') + '%)'
    })) : [
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

    // Logic xử lý Đặt phòng trực tiếp
    const btnSearchRooms = document.getElementById('btn-search-rooms');
    const searchResults = document.getElementById('db-search-results');
    const btnCheckCustomer = document.getElementById('btn-check-customer');
    const btnNfcScan = document.getElementById('btn-nfc-scan');
    const inputCccd = document.getElementById('db-cccd');
    const inputFullname = document.getElementById('db-fullname');
    const inputPhone = document.getElementById('db-phone');
    const btnConfirmBooking = document.getElementById('btn-confirm-booking');

    if(btnSearchRooms) {
        btnSearchRooms.addEventListener('click', () => {
            const checkin = document.getElementById('db-checkin').value;
            const checkout = document.getElementById('db-checkout').value;
            if(!checkin || !checkout) {
                alert('Vui lòng chọn ngày Check-in và Check-out để tìm phòng trống.');
                return;
            }
            // Popoulate vacant rooms from window.mockVacantRooms
            const roomSelect = document.getElementById('db-selected-room');
            if (window.mockVacantRooms && window.mockVacantRooms.length > 0) {
                roomSelect.innerHTML = window.mockVacantRooms.map(r => `<option value="${r.id}">Phòng ${r.name || r.id} - ${r.price ? r.price + ' đ' : ''}</option>`).join('');
            } else {
                roomSelect.innerHTML = '<option value="">Không có phòng trống</option>';
            }
            searchResults.classList.remove('d-none');
        });
    }

    if(btnCheckCustomer) {
        btnCheckCustomer.addEventListener('click', () => {
            const cccd = inputCccd.value.trim();
            if(!cccd) {
                alert('Vui lòng nhập CCCD để kiểm tra!');
                return;
            }
            
            // Giả lập KiemTraKhachCu (UC21)
            if(cccd === '0123456789' || cccd === '079099123456') {
                inputFullname.value = 'Nguyễn Văn Khách Cũ';
                inputPhone.value = '0901234567';
                alert('Đã tìm thấy thông tin khách hàng cũ!');
            } else {
                inputFullname.value = '';
                inputPhone.value = '';
                alert('Khách hàng mới. Vui lòng nhập thông tin.');
            }
        });
    }

    if(btnNfcScan) {
        btnNfcScan.addEventListener('click', () => {
            // Giả lập XacThucCCCD_NFC()
            alert('Đang chờ thiết bị đọc NFC... Đã nhận dạng thẻ CCCD!');
            inputCccd.value = '079099123456';
            inputFullname.value = 'Nguyễn Văn Khách Cũ (NFC)';
            inputPhone.value = '0901234567';
        });
    }

    if(btnConfirmBooking) {
        // Form submission is handled by backend POST action
    }
});
