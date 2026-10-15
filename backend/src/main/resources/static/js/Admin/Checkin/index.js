const API_URL = 'http://localhost:8080/api/checkin';
document.addEventListener('DOMContentLoaded', () => {
    fetchCheckinData();
});

function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alert-container');
    alertContainer.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}

async function fetchCheckinData() {
    const tbody = document.getElementById('checkin-table-body');
    const invoices = window.mockCheckinInvoices || window.mockInvoices || [];

    try {
        const emptyRooms = window.mockEmptyRooms || [];
        
        if (invoices.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-5 text-muted">
                        <i class="bi bi-inbox fs-2 d-block mb-2 opacity-50"></i>
                        Hiện tại chưa có phiếu đặt phòng nào chờ làm thủ tục Check-in.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = invoices.map(item => {
            const isGuest = item.idTaiKhoan == null;
            const priceFormatted = new Intl.NumberFormat('vi-VN').format(item.totalPrice || 0) + ' đ';
            
            let roomBadge = '';
            if (item.maPhong) {
                roomBadge = `<span class="badge bg-success px-3 py-2 rounded-pill fw-bold"><i class="bi bi-door-open-fill me-1"></i> Phòng ${item.maPhong}</span>`;
            } else {
                roomBadge = `<span class="badge bg-warning text-dark px-3 py-2 rounded-pill fw-bold"><i class="bi bi-exclamation-triangle-fill me-1"></i> Chưa xếp phòng</span>`;
            }

            let roomSelectionHTML = '';
            if (!item.isVerified) {
                // Chưa xác thực -> Bắt buộc xác thực Mobile trước
                roomSelectionHTML = `
                    <div class="alert alert-warning py-1 px-2 mb-2 text-center small fw-bold" style="font-size: 0.7rem;">
                        <i class="bi bi-shield-lock-fill me-1"></i>Cần xác thực danh tính
                    </div>
                    <button type="button" class="btn btn-outline-primary btn-sm fw-bold w-100 text-truncate mb-2" onclick="openMobileVerifyModal(${item.id}, ${item.bookingId || 'null'})">
                        <i class="bi bi-phone-vibrate me-1"></i> Xác thực (Mobile)
                    </button>
                    <input type="hidden" name="maPhong" id="room-${item.id}" value="" required>
                `;
            } else {
                // Đã xác thực -> Hiện nút Chọn phòng / Đổi phòng
                if (item.maPhong) {
                    roomSelectionHTML = `
                        <div class="text-success text-center small fw-bold mb-1" style="font-size: 0.7rem;"><i class="bi bi-check-circle-fill me-1"></i>Đã xác thực FaceID</div>
                        <button type="button" class="btn btn-outline-navy btn-sm fw-bold w-100 text-start text-truncate mb-2" onclick="openAssignRoomModal(${item.id}, '${item.maPhong}', ${item.maLoaiPhong || 'null'})" id="btn-select-room-${item.id}">
                            <i class="bi bi-door-open-fill me-1"></i> Phòng ${item.maPhong} (Đổi)
                        </button>
                        <input type="hidden" name="maPhong" id="room-${item.id}" value="${item.maPhong}" required>
                    `;
                } else {
                    roomSelectionHTML = `
                        <div class="text-success text-center small fw-bold mb-1" style="font-size: 0.7rem;"><i class="bi bi-check-circle-fill me-1"></i>Đã xác thực FaceID</div>
                        <button type="button" class="btn btn-outline-danger btn-sm fw-bold w-100 text-start text-truncate mb-2" onclick="openAssignRoomModal(${item.id}, null, ${item.maLoaiPhong || 'null'})" id="btn-select-room-${item.id}">
                            <i class="bi bi-key-fill me-1"></i> Chọn phòng...
                        </button>
                        <input type="hidden" name="maPhong" id="room-${item.id}" value="" required>
                    `;
                }
            }

            return `
            <tr>
                <td class="ps-4 fw-bold text-navy">#${item.id}</td>
                <td class="fw-bold text-secondary">
                    <span>${item.hoTen}</span>
                    ${isGuest ? '<span class="badge bg-light text-dark border small ms-1" style="font-size: 0.65rem;">Khách vãng lai</span>' : ''}
                </td>
                <td>${item.sdt || ''}</td>
                <td class="text-center">${roomBadge}</td>
                <td>
                    <small class="d-block text-muted">Nhận: <strong class="text-dark">${item.ngayCheckIn || ''}</strong></small>
                    <small class="d-block text-muted">Trả: <strong class="text-dark">${item.ngayCheckOut || ''}</strong></small>
                </td>
                <td class="text-end fw-bold text-danger">${priceFormatted}</td>
                <td class="text-center">
                    <form onsubmit="submitCheckin(event, ${item.id})" class="d-flex flex-column gap-2 p-2 rounded bg-light border">
                        ${roomSelectionHTML}
                        <div class="form-check form-switch m-0 text-start ps-5">
                            <input class="form-check-input cursor-pointer" type="checkbox" name="isPaidUpfront" value="true" id="checkPaid-${item.id}" ${isGuest ? 'checked' : ''}>
                            <label class="form-check-label small fw-bold text-secondary" for="checkPaid-${item.id}" style="font-size: 0.75rem;">Thu trước 1 đêm (TM)</label>
                        </div>
                        <button type="submit" class="btn btn-sm text-white w-100 rounded-pill fw-bold small py-1 shadow-sm border-0" style="background: linear-gradient(135deg, #C5A017, #d4af37); transition: all 0.3s;" ${!item.isVerified ? 'disabled' : ''}>
                            <i class="bi bi-check2-circle me-1"></i> Duyệt nhận phòng
                        </button>
                    </form>
                </td>
            </tr>
            `;
        }).join('');

    } catch (error) {
        console.error('Error fetching data:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5 text-danger">
                    <i class="bi bi-exclamation-triangle fs-2 d-block mb-2 opacity-50"></i>
                    <p>Lỗi kết nối API. Hãy đảm bảo API ${API_URL}/init đang hoạt động.</p>
                </td>
            </tr>
        `;
    }
}

let currentAssigningCheckinId = null;
let currentVerifyCheckinId = null;
let currentVerifyBookingId = null;
let selectedRoomForCheckin = null;
let currentRequiredRoomTypeId = null;

let pollingInterval = null;
let mobileTransitionTimeout = null;

// ==========================================
// LUỒNG XÁC THỰC MOBILE NFC
// ==========================================
window.openMobileVerifyModal = function(invoiceId, bookingId) {
    currentVerifyCheckinId = invoiceId;
    currentVerifyBookingId = bookingId;
    const invoice = (window.mockCheckinInvoices || []).find(
        item => String(item.id) === String(invoiceId)
    );
    const hoTen = invoice?.hoTen || '';
    
    // Reset giao diện Modal
    document.getElementById('mobile-verify-waiting').classList.remove('d-none');
    document.getElementById('mobile-verify-success').classList.add('d-none');
    
    document.getElementById('mobile-verify-code').textContent = bookingId
        ? 'BOOKING-' + bookingId
        : 'REQ-' + invoiceId;
    document.getElementById('sync-name').textContent = hoTen;
    document.getElementById('sync-method').textContent = 'Mobile NFC + VPS AI';
    setMobileVerifyStatus(
        bookingId ? 'Đang kiểm tra trạng thái từ máy chủ...' : 'Phiếu này chưa có mã booking để đồng bộ Mobile.',
        bookingId ? 'info' : 'danger'
    );

    document.getElementById('btn-cancel-mobile-verify').classList.remove('d-none');
    document.getElementById('btn-manual-exception').classList.remove('d-none');
    document.getElementById('btn-simulate-mobile').classList.remove('d-none');
    document.getElementById('btn-continue-room').classList.add('d-none');

    const modalEl = document.getElementById('mobileVerifyModal');
    if (modalEl.parentNode !== document.body) {
        document.body.appendChild(modalEl);
    }
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();

    // Bắt đầu Polling API 3 giây 1 lần
    if (pollingInterval) clearInterval(pollingInterval);
    if (mobileTransitionTimeout) clearTimeout(mobileTransitionTimeout);

    const pollVerificationStatus = async () => {
        if (!currentVerifyBookingId) return;
        try {
            const res = await fetch(`/api/v1/reception/bookings/${currentVerifyBookingId}/verification-status`, {
                cache: 'no-store'
            });
            if (res.ok) {
                const data = await res.json();
                if (data.isFaceVerified === true) {
                    clearInterval(pollingInterval);
                    window.simulateMobileSuccess(true, data.verificationMethod);
                } else {
                    const requiredGuests = Number(data.requiredGuestCount || 1);
                    const verifiedGuests = Number(data.verifiedGuestCount || 0);
                    const progress = requiredGuests > 1
                        ? ` Đã xác thực ${verifiedGuests}/${requiredGuests} khách.`
                        : '';
                    setMobileVerifyStatus(`Đã kết nối máy chủ.${progress} Đang chờ hoàn tất NFC và xác thực khuôn mặt trên Mobile...`, 'info');
                }
            } else {
                setMobileVerifyStatus('Chưa đọc được trạng thái booking. Hệ thống sẽ tự thử lại.', 'warning');
            }
        } catch (e) {
            console.log('Polling AI verification failed:', e);
            setMobileVerifyStatus('Mất kết nối tạm thời với máy chủ. Hệ thống sẽ tự thử lại.', 'warning');
        }
    };
    pollVerificationStatus();
    pollingInterval = setInterval(pollVerificationStatus, 3000);
    
    // Xoá polling nếu Modal bị đóng thủ công
    modalEl.addEventListener('hidden.bs.modal', function onModalHidden() {
        if (pollingInterval) clearInterval(pollingInterval);
        modalEl.removeEventListener('hidden.bs.modal', onModalHidden);
    });
};

function setMobileVerifyStatus(message, type) {
    const status = document.getElementById('mobile-verify-status');
    status.className = `alert alert-${type} py-2 small text-start mb-3`;
    status.innerHTML = `<i class="bi bi-broadcast me-1"></i>${message}`;
}

window.simulateMobileSuccess = async function(fromApi = false, verificationMethod = null) {
    if (pollingInterval) clearInterval(pollingInterval);

    if (!fromApi) {
        if (!currentVerifyBookingId) {
            showAlert('Phiếu này chưa có mã booking để chạy giả lập.', 'danger');
            return;
        }
        try {
            const response = await fetch(`/api/v1/reception/bookings/${currentVerifyBookingId}/simulate-verification`, {
                method: 'POST'
            });
            const result = await response.json();
            if (!response.ok) {
                throw new Error(result.message || 'Không thể lưu trạng thái giả lập.');
            }
            verificationMethod = result.verificationMethod;
        } catch (error) {
            showAlert(error.message || 'Không thể chạy giả lập xác thực.', 'danger');
            return;
        }
    }

    const invoice = (window.mockCheckinInvoices || []).find(
        item => String(item.id) === String(currentVerifyCheckinId)
    );
    if (invoice) {
        invoice.isVerified = true;
    }

    // Đóng các nút chờ sau khi backend đã ghi nhận xác thực
    document.getElementById('btn-cancel-mobile-verify').classList.add('d-none');
    document.getElementById('btn-manual-exception').classList.add('d-none');
    document.getElementById('btn-simulate-mobile').classList.add('d-none');
    
    // Hiển thị kết quả thành công và nút Tiếp tục
    document.getElementById('mobile-verify-waiting').classList.add('d-none');
    document.getElementById('mobile-verify-success').classList.remove('d-none');
    document.getElementById('btn-continue-room').classList.remove('d-none');
    document.getElementById('sync-method').textContent = verificationMethod === 'MANUAL_EXCEPTION'
        ? 'Nhập tay ngoại lệ bởi lễ tân'
        : verificationMethod === 'DEMO_SIMULATION'
            ? 'Giả lập xác thực (Demo)'
            : 'Mobile NFC + VPS AI';

    if (fromApi) {
        mobileTransitionTimeout = setTimeout(() => {
            window.completeMobileVerification();
        }, 800);
    }
};

window.openManualVerificationModal = function() {
    if (!currentVerifyBookingId) {
        showAlert('Phiếu này chưa có mã booking để lưu xác thực nhập tay.', 'danger');
        return;
    }
    if (pollingInterval) clearInterval(pollingInterval);

    document.getElementById('manual-reason').value = '';
    document.getElementById('manual-name').value = document.getElementById('sync-name').textContent || '';
    document.getElementById('manual-id').value = '';
    document.getElementById('manual-nationality').value = '';
    document.getElementById('manual-dob').value = '';

    const mobileModal = bootstrap.Modal.getInstance(document.getElementById('mobileVerifyModal'));
    if (mobileModal) mobileModal.hide();

    const manualModalEl = document.getElementById('verifyIdModal');
    if (manualModalEl.parentNode !== document.body) {
        document.body.appendChild(manualModalEl);
    }
    setTimeout(() => bootstrap.Modal.getOrCreateInstance(manualModalEl).show(), 250);
};

window.completeMobileVerification = async function() {
    if (mobileTransitionTimeout) clearTimeout(mobileTransitionTimeout);

    const verifiedInvoice = (window.mockCheckinInvoices || []).find(
        invoice => String(invoice.id) === String(currentVerifyCheckinId)
    );
    if (!verifiedInvoice) {
        showAlert('Không tìm thấy phiếu check-in cần đồng bộ.', 'danger');
        return;
    }
    verifiedInvoice.isVerified = true;

    // Đóng modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('mobileVerifyModal'));
    modal.hide();
    
    // Rerender lại bảng để hiện nút chọn phòng
    await fetchCheckinData();
    
    // Bật luôn modal Chọn phòng để lễ tân tiện thao tác
    setTimeout(() => {
        openAssignRoomModal(
            currentVerifyCheckinId,
            verifiedInvoice.maPhong,
            verifiedInvoice.maLoaiPhong
        );
    }, 400);
};

const availableRooms = Array.isArray(window.mockEmptyRooms) ? window.mockEmptyRooms : [];
const mockEmptyRoomsGlobal = availableRooms.map(r => {
    const roomName = r.name == null ? '' : String(r.name);
    return {
        id: r.id,
        roomTypeId: r.roomTypeId,
        view: r.view || 'Hướng Thành Phố',
        type: r.type || 'Chưa phân loại',
        label: r.label || roomName.replace(/\D/g, '') || String(r.id),
        occupied: r.occupied === true
    };
});

function openAssignRoomModal(checkinId, currentRoomId, requiredRoomTypeId) {
    currentAssigningCheckinId = checkinId;
    currentRequiredRoomTypeId = requiredRoomTypeId == null ? null : String(requiredRoomTypeId);
    selectedRoomForCheckin = mockEmptyRoomsGlobal.find(
        room => String(room.id) === String(currentRoomId)
            && String(room.roomTypeId) === currentRequiredRoomTypeId
            && !room.occupied
    ) || null;
    
    // Reset filters
    const categoryFilter = document.getElementById('assignRoomCategory');
    const requiredTypeRoom = selectedRoomForCheckin || mockEmptyRoomsGlobal.find(
        room => String(room.roomTypeId) === currentRequiredRoomTypeId
    );
    const hasMatchingCategory = requiredTypeRoom
        && Array.from(categoryFilter.options).some(option => option.value === requiredTypeRoom.type);
    categoryFilter.value = hasMatchingCategory ? requiredTypeRoom.type : '';
    categoryFilter.disabled = true;
    categoryFilter.title = 'Loại phòng được cố định theo booking của khách';
    document.getElementById('assignRoomView').value = '';
    
    renderAvailableRooms();
    updateAssignRoomConfirmation();

    const modalEl = document.getElementById('assignRoomModal');
    if (modalEl.parentNode !== document.body) {
        document.body.appendChild(modalEl);
    }
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();
}

function renderAvailableRooms() {
    const listContainer = document.getElementById('availableRoomsList');
    const filterCat = document.getElementById('assignRoomCategory').value;
    const filterView = document.getElementById('assignRoomView').value;

    if (currentRequiredRoomTypeId == null) {
        listContainer.innerHTML = '<div class="text-center py-4 text-danger small"><i class="bi bi-exclamation-triangle fs-3 d-block mb-1"></i>Không xác định được loại phòng khách đã đặt.</div>';
        return;
    }
    
    const filteredRooms = mockEmptyRoomsGlobal.filter(r => {
        const matchBookedType = String(r.roomTypeId) === currentRequiredRoomTypeId;
        const matchCat = filterCat === '' || r.type === filterCat;
        const matchView = filterView === '' || r.view === filterView;
        return matchBookedType && matchCat && matchView;
    });

    if (filteredRooms.length === 0) {
        listContainer.innerHTML = '<div class="text-center py-4 text-muted small"><i class="bi bi-exclamation-circle fs-3 d-block mb-1"></i>Không có phòng trống phù hợp.</div>';
        return;
    }

    listContainer.innerHTML = filteredRooms.map(r => {
        let viewIcon = 'bi-compass';
        if (r.view === 'Hướng Biển') viewIcon = 'bi-water text-primary';
        else if (r.view === 'Hướng Thành Phố') viewIcon = 'bi-buildings text-secondary';
        else if (r.view === 'Hướng Núi') viewIcon = 'bi-tree text-success';
        const isSelected = selectedRoomForCheckin
            && String(selectedRoomForCheckin.id) === String(r.id);
        const occupiedClass = r.occupied ? 'disabled bg-light opacity-75' : '';

        return `
        <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-3 room-option ${occupiedClass} ${isSelected ? 'border border-2 border-success' : ''}"
                data-room-id="${r.id}" data-room-view="${r.view}" data-room-type="${r.type}" data-room-label="${r.label}"
                data-room-occupied="${r.occupied}" aria-pressed="${isSelected}" ${r.occupied ? 'disabled aria-disabled="true"' : ''}>
            <div>
                <h6 class="fw-bold text-navy mb-1"><i class="bi bi-door-open-fill text-gold me-2"></i>Phòng ${r.label}</h6>
                <small class="text-muted"><span class="badge bg-light text-dark border me-1">${r.type}</span></small>
                ${r.occupied ? '<span class="badge bg-danger-subtle text-danger border border-danger-subtle ms-1"><i class="bi bi-person-fill-lock me-1"></i>Đang lưu trú - Không thể chọn</span>' : ''}
            </div>
            <div class="text-end">
                <span class="text-muted small fw-bold"><i class="bi ${viewIcon} me-1"></i>${r.view}</span>
                ${isSelected ? '<i class="bi bi-check-circle-fill text-success ms-2" title="Đã chọn"></i>' : ''}
            </div>
        </button>
        `;
    }).join('');
}

document.getElementById('availableRoomsList').addEventListener('click', function (event) {
    const roomOption = event.target.closest('.room-option');
    if (!roomOption || !this.contains(roomOption)) {
        return;
    }
    event.preventDefault();
    if (roomOption.dataset.roomOccupied === 'true') {
        showAlert('Phòng này đang có khách lưu trú và không thể gán cho phiếu check-in.', 'warning');
        return;
    }
    selectedRoomForCheckin = mockEmptyRoomsGlobal.find(
        room => String(room.id) === String(roomOption.dataset.roomId)
            && String(room.roomTypeId) === currentRequiredRoomTypeId
            && !room.occupied
    ) || null;
    renderAvailableRooms();
    updateAssignRoomConfirmation();
});

// Add event listeners to filters
document.getElementById('assignRoomCategory').addEventListener('change', renderAvailableRooms);
document.getElementById('assignRoomView').addEventListener('change', renderAvailableRooms);

const confirmAssignRoomButton = document.getElementById('btnConfirmAssignRoom');
confirmAssignRoomButton.addEventListener('click', function() {
    if (!selectedRoomForCheckin) {
        showAlert('Vui lòng chọn một phòng trước khi xác nhận!', 'warning');
        return;
    }
    window.selectRoomForCheckin(
        selectedRoomForCheckin.id,
        selectedRoomForCheckin.view,
        selectedRoomForCheckin.type,
        selectedRoomForCheckin.label
    );
});

function updateAssignRoomConfirmation() {
    confirmAssignRoomButton.disabled = !selectedRoomForCheckin;
    confirmAssignRoomButton.innerHTML = selectedRoomForCheckin
        ? `<i class="bi bi-check-circle-fill me-1"></i> Xác nhận Phòng ${selectedRoomForCheckin.label}`
        : '<i class="bi bi-check-circle-fill me-1"></i> Xác nhận Gán phòng';
}

window.selectRoomForCheckin = function(roomId, view, type, label) {
    if (!currentAssigningCheckinId) return;

    // Update hidden input
    const roomInput = document.getElementById('room-' + currentAssigningCheckinId);
    const btn = document.getElementById('btn-select-room-' + currentAssigningCheckinId);
    if (!roomInput || !btn) {
        showAlert('Không thể gán phòng cho phiếu check-in này.', 'danger');
        return;
    }
    roomInput.value = roomId;

    // Update button text
    btn.innerHTML = `<i class="bi bi-door-open-fill me-1"></i> Phòng ${label || roomId} (${view})`;
    btn.classList.replace('btn-outline-danger', 'btn-outline-navy');

    const modal = bootstrap.Modal.getInstance(document.getElementById('assignRoomModal'));
    modal.hide();
};

// Lưu xác thực nhập tay ngoại lệ; AI/NFC chỉ chạy trên Mobile.
document.getElementById('btn-complete-checkin').addEventListener('click', async function() {
    const payload = {
        reason: document.getElementById('manual-reason').value,
        fullName: document.getElementById('manual-name').value.trim(),
        documentNumber: document.getElementById('manual-id').value.trim(),
        nationality: document.getElementById('manual-nationality').value.trim(),
        dateOfBirth: document.getElementById('manual-dob').value
    };
    if (Object.values(payload).some(value => !value)) {
        showAlert('Vui lòng nhập đầy đủ lý do, họ tên, số giấy tờ, quốc tịch và ngày sinh.', 'warning');
        return;
    }

    const btn = this;
    const originalHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang lưu...';

    try {
        const response = await fetch(`/api/v1/reception/bookings/${currentVerifyBookingId}/manual-verification`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const result = await response.json();
        if (!response.ok) {
            throw new Error(result.message || 'Không thể lưu xác thực nhập tay.');
        }

        const invoice = (window.mockCheckinInvoices || []).find(
            item => String(item.id) === String(currentVerifyCheckinId)
        );
        if (invoice) invoice.isVerified = true;

        const verifyModal = bootstrap.Modal.getInstance(document.getElementById('verifyIdModal'));
        if (verifyModal) verifyModal.hide();
        showAlert('Đã lưu xác thực nhập tay ngoại lệ. Vui lòng chọn phòng đúng loại đã đặt.', 'success');
        await fetchCheckinData();
        setTimeout(() => {
            if (invoice) {
                openAssignRoomModal(currentVerifyCheckinId, invoice.maPhong, invoice.maLoaiPhong);
            }
        }, 300);
    } catch (error) {
        console.error('Error:', error);
        showAlert(error.message || 'Có lỗi khi lưu xác thực nhập tay.', 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalHtml;
    }
});

window.submitCheckin = function(event, id) {
    event.preventDefault();
    const form = event.target;
    const roomInput = form.querySelector('input[name="maPhong"]');
    const maPhong = roomInput ? roomInput.value : '';
    if (!maPhong) {
        showAlert('Vui lòng chọn phòng trước khi check-in!', 'warning');
        return;
    }
    if (!confirm('Xác nhận số phòng vật lý bàn giao, lập biên bản cọc và tiến hành Check-in?')) {
        return;
    }
    const isPaidUpfront = form.querySelector('input[name="isPaidUpfront"]').checked;
    const query = new URLSearchParams({
        maPhong,
        isPaidUpfront: String(isPaidUpfront)
    });
    window.location.assign(`/receptionist/checkin/execute/${id}?${query.toString()}`);
};

// ==========================================
// TÍNH NĂNG TRA CỨU MÃ ĐẶT PHÒNG
// ==========================================

/**
 * Hàm format ngày giờ chuẩn Việt Nam (DD/MM/YYYY HH:mm)
 */
function formatDateVN(dateString) {
    if (!dateString) return '--/--/---- --:--';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString; // Fallback nếu chuỗi lỗi
    
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

/**
 * Hàm xử lý khi submit form tra cứu
 */
async function searchBookingCode(event) {
    event.preventDefault(); // Ngăn trang reload
    const codeInput = document.getElementById('bookingCodeInput').value.trim();
    if (!codeInput) return;

    // TODO: Khi nối với Backend thật, hãy dùng đoạn mã fetch này:
    /*
    try {
        const response = await fetch(`${API_URL}/search?code=${codeInput}`);
        if (!response.ok) throw new Error('Không tìm thấy mã đặt phòng');
        const data = await response.json();
        showSearchResultModal(data);
    } catch (error) {
        showAlert(error.message, 'danger');
    }
    */

    // DỮ LIỆU GIẢ LẬP (MOCK DATA) ĐỂ DEMO
    const mockDatabase = {
        'BOOK-123': {
            hoTen: 'Nguyễn Văn A',
            ngayCheckIn: '2026-06-18T14:00:00',
            ngayCheckOut: '2026-06-20T12:00:00',
            trangThai: 'PAID' // Đã thanh toán
        },
        'BOOK-456': {
            hoTen: 'Trần Thị B',
            ngayCheckIn: '2026-06-19T14:00:00',
            ngayCheckOut: '2026-06-21T12:00:00',
            trangThai: 'UNPAID' // Chưa thanh toán
        },
        'BOOK-789': {
            hoTen: 'Lê Văn C',
            ngayCheckIn: '2026-05-10T14:00:00',
            ngayCheckOut: '2026-05-12T12:00:00',
            trangThai: 'CANCELLED' // Đã hủy
        },
        'BOOK-000': {
            hoTen: 'Phạm Thị D',
            ngayCheckIn: '2026-05-01T14:00:00',
            ngayCheckOut: '2026-05-03T12:00:00',
            trangThai: 'CHECKED_OUT' // Đã trả phòng
        }
    };

    const searchResult = mockDatabase[codeInput.toUpperCase()];
    
    if (searchResult) {
        showSearchResultModal(searchResult);
    } else {
        showAlert(`Không tìm thấy thông tin cho mã đặt phòng: ${codeInput}`, 'danger');
    }
}

/**
 * Hiển thị Modal với thông tin trả về
 */
function showSearchResultModal(data) {
    // 1. Gán Họ tên
    document.getElementById('res-hoTen').innerText = data.hoTen;
    
    // 2. Gán ngày nhận/trả format DD/MM/YYYY HH:mm
    document.getElementById('res-ngayCheckIn').innerText = formatDateVN(data.ngayCheckIn);
    document.getElementById('res-ngayCheckOut').innerText = formatDateVN(data.ngayCheckOut);
    
    // 3. Xử lý Trạng thái & Màu sắc
    const statusBadge = document.getElementById('res-trangThai');
    // Reset các class màu cũ
    statusBadge.className = 'badge px-3 py-2 fs-6 rounded-pill shadow-sm';
    
    switch (data.trangThai) {
        case 'PAID':
            statusBadge.innerText = 'Đã thanh toán';
            statusBadge.classList.add('bg-success'); // Màu xanh lá
            break;
        case 'UNPAID':
            statusBadge.innerText = 'Chưa thanh toán';
            statusBadge.classList.add('bg-warning', 'text-dark'); // Màu vàng
            break;
        case 'CANCELLED':
            statusBadge.innerText = 'Đã hủy';
            statusBadge.classList.add('bg-danger'); // Màu đỏ
            break;
        case 'CHECKED_OUT':
            statusBadge.innerText = 'Đã trả phòng';
            statusBadge.classList.add('bg-secondary'); // Màu xám
            break;
        default:
            statusBadge.innerText = 'Không xác định';
            statusBadge.classList.add('bg-dark');
    }
    
    // 4. Hiển thị modal
    const modalEl = document.getElementById('searchResultModal');
    if (modalEl.parentNode !== document.body) {
        document.body.appendChild(modalEl);
    }
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();
}
