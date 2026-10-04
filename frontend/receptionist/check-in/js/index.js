const API_URL = 'http://localhost:8080/api/checkin';
const AI_URL = 'https://[DIEN-DOMAIN-AI-CUA-BAN-VAO-DAY].ngrok-free.app';
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
    const mockCheckinInvoices = [
        { id: 3, hoTen: 'Lê Hoàng C', idTaiKhoan: 10, sdt: '0912345678', ngayCheckIn: '2026-06-18', ngayCheckOut: '2026-06-20', totalPrice: 5400000, maPhong: null },
        { id: 4, hoTen: 'Khách Vãng Lai', idTaiKhoan: null, sdt: '0999888777', ngayCheckIn: '2026-06-18', ngayCheckOut: '2026-06-19', totalPrice: 1500000, maPhong: 301 }
    ];

    const mockEmptyRooms = [
        { id: 101, view: 'Hướng Thành Phố', type: 'Standard' }, 
        { id: 104, view: 'Hướng Biển', type: 'Standard' }, 
        { id: 202, view: 'Hướng Núi', type: 'Deluxe' }, 
        { id: 203, view: 'Hướng Biển', type: 'Deluxe' },
        { id: 303, view: 'Hướng Núi', type: 'Suite' }
    ];

    try {
        const invoices = mockCheckinInvoices;
        const emptyRooms = mockEmptyRooms;
        
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
            if (item.maPhong) {
                roomSelectionHTML = `
                    <button type="button" class="btn btn-outline-navy btn-sm fw-bold w-100 text-start text-truncate" onclick="openAssignRoomModal(${item.id}, '${item.maPhong}')" id="btn-select-room-${item.id}">
                        <i class="bi bi-door-open-fill me-1"></i> Phòng ${item.maPhong} (Đổi)
                    </button>
                    <input type="hidden" name="maPhong" id="room-${item.id}" value="${item.maPhong}" required>
                `;
            } else {
                roomSelectionHTML = `
                    <button type="button" class="btn btn-outline-danger btn-sm fw-bold w-100 text-start text-truncate" onclick="openAssignRoomModal(${item.id}, null)" id="btn-select-room-${item.id}">
                        <i class="bi bi-key-fill me-1"></i> Chọn phòng...
                    </button>
                    <input type="hidden" name="maPhong" id="room-${item.id}" value="" required>
                `;
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
                    <form onsubmit="openCccdModal(event, ${item.id})" class="d-flex flex-column gap-2 p-2 rounded bg-light border">
                        ${roomSelectionHTML}
                        <div class="form-check form-switch m-0 text-start ps-5">
                            <input class="form-check-input cursor-pointer" type="checkbox" name="isPaidUpfront" value="true" id="checkPaid-${item.id}" ${isGuest ? 'checked' : ''}>
                            <label class="form-check-label small fw-bold text-secondary" for="checkPaid-${item.id}" style="font-size: 0.75rem;">Thu trước 1 đêm (TM)</label>
                        </div>
                        <button type="submit" class="btn btn-gold btn-sm w-100 rounded-pill fw-bold small py-1">
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

let stream = null;
let currentCheckinId = null;
let currentAssigningCheckinId = null;

const mockEmptyRoomsGlobal = [
    { id: 101, view: 'Hướng Thành Phố', type: 'Standard' }, 
    { id: 104, view: 'Hướng Biển', type: 'Standard' }, 
    { id: 202, view: 'Hướng Núi', type: 'Deluxe' }, 
    { id: 203, view: 'Hướng Biển', type: 'Deluxe' },
    { id: 303, view: 'Hướng Núi', type: 'Suite' }
];

function openAssignRoomModal(checkinId, currentRoomId) {
    currentAssigningCheckinId = checkinId;
    
    // Reset filters
    document.getElementById('assignRoomCategory').value = '';
    document.getElementById('assignRoomView').value = '';
    
    renderAvailableRooms();

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
    
    const filteredRooms = mockEmptyRoomsGlobal.filter(r => {
        const matchCat = filterCat === '' || r.type === filterCat;
        const matchView = filterView === '' || r.view === filterView;
        return matchCat && matchView;
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

        return `
        <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-3" onclick="selectRoomForCheckin('${r.id}', '${r.view}', '${r.type}')">
            <div>
                <h6 class="fw-bold text-navy mb-1"><i class="bi bi-door-open-fill text-gold me-2"></i>Phòng ${r.id}</h6>
                <small class="text-muted"><span class="badge bg-light text-dark border me-1">${r.type}</span></small>
            </div>
            <div class="text-end">
                <span class="text-muted small fw-bold"><i class="bi ${viewIcon} me-1"></i>${r.view}</span>
            </div>
        </button>
        `;
    }).join('');
}

// Add event listeners to filters
document.getElementById('assignRoomCategory').addEventListener('change', renderAvailableRooms);
document.getElementById('assignRoomView').addEventListener('change', renderAvailableRooms);

// Prevent confirm button logic since we select room by clicking on the list
document.getElementById('btnConfirmAssignRoom').style.display = 'none';

window.selectRoomForCheckin = function(roomId, view, type) {
    if (!currentAssigningCheckinId) return;

    // Update hidden input
    document.getElementById('room-' + currentAssigningCheckinId).value = roomId;

    // Update button text
    const btn = document.getElementById('btn-select-room-' + currentAssigningCheckinId);
    btn.innerHTML = `<i class="bi bi-door-open-fill me-1"></i> P.${roomId} (${view})`;
    btn.classList.replace('btn-outline-danger', 'btn-outline-navy');

    const modal = bootstrap.Modal.getInstance(document.getElementById('assignRoomModal'));
    modal.hide();
};

function openCccdModal(event, id) {
    event.preventDefault();
    const form = event.target;
    const maPhong = form.querySelector('select[name="maPhong"]').value;
    const isPaidUpfront = form.querySelector('input[name="isPaidUpfront"]').checked;

    if (!maPhong) {
        showAlert('Vui lòng chọn phòng trước khi check-in!', 'warning');
        return;
    }

    currentCheckinId = id;
    document.getElementById('checkin-id').value = id;
    document.getElementById('checkin-room').value = maPhong;
    document.getElementById('checkin-paid').value = isPaidUpfront;

    // Reset modal state
    document.getElementById('raw-data-result').value = '';
    document.getElementById('front-cccd').value = '';
    document.getElementById('back-cccd').value = '';
    document.getElementById('front-preview').style.display = 'none';
    document.getElementById('front-placeholder').style.display = 'block';
    document.getElementById('back-preview').style.display = 'none';
    document.getElementById('back-placeholder').style.display = 'block';
    
    // Reset manual form
    document.getElementById('manual-name').value = '';
    document.getElementById('manual-id').value = '';
    document.getElementById('manual-nationality').value = '';
    document.getElementById('manual-dob').value = '';

    stopWebcam();

    const modalEl = document.getElementById('verifyIdModal');
    // Di chuyển modal ra ngoài cùng của thẻ body để tránh lỗi z-index (bị lớp nền đen che mất)
    if (modalEl.parentNode !== document.body) {
        document.body.appendChild(modalEl);
    }
    const verifyModal = bootstrap.Modal.getOrCreateInstance(modalEl);
    verifyModal.show();
}

// Lắng nghe sự kiện đóng modal để tắt webcam nếu đang bật
document.getElementById('verifyIdModal').addEventListener('hidden.bs.modal', function () {
    stopWebcam();
});

// Xử lý preview ảnh
document.getElementById('front-cccd').addEventListener('change', function(e) {
    previewImage(this, 'front-preview', 'front-placeholder');
});
document.getElementById('back-cccd').addEventListener('change', function(e) {
    previewImage(this, 'back-preview', 'back-placeholder');
});

function previewImage(input, previewId, placeholderId) {
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        reader.onload = function(e) {
            document.getElementById(previewId).src = e.target.result;
            document.getElementById(previewId).style.display = 'block';
            document.getElementById(placeholderId).style.display = 'none';
        }
        reader.readAsDataURL(input.files[0]);
    }
}

// Nút Trích xuất (Upload)
document.getElementById('btn-extract-upload').addEventListener('click', async function() {
    const frontInput = document.getElementById('front-cccd');
    
    if (frontInput.files.length === 0) {
        alert("Vui lòng tải lên ảnh Mặt trước của CCCD để trích xuất!");
        return;
    }

    const btn = this;
    const originalHtml = btn.innerHTML;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang kết nối AI...';
    btn.disabled = true;

    try {
        const formData = new FormData();
        formData.append("cccd_image", frontInput.files[0]);

        const response = await fetch(`${AI_URL}/api/v1/ai/verify-cccd`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error(`Lỗi server AI: ${response.status}`);
        }

        const data = await response.json();
        
        // Format lại dữ liệu trả về để hiển thị
        if(data.success && data.data) {
            const resultData = {
                "Số CCCD": data.data.id || "Không rõ",
                "Họ và tên": data.data.name || "Không rõ",
                "Ngày sinh": data.data.dob || "Không rõ",
                "Giới tính": data.data.gender || "Không rõ",
                "Ngày hết hạn": data.data.date_of_expiry || "Không rõ"
            };
            document.getElementById('raw-data-result').value = JSON.stringify(resultData, null, 2);
        } else {
            alert("AI không thể đọc được thông tin. Vui lòng sử dụng ảnh rõ nét hơn!");
        }
    } catch (err) {
        console.error("AI Error:", err);
        alert("Không thể kết nối đến AI Server! Vui lòng kiểm tra lại AI_URL hoặc Ngrok.");
    } finally {
        btn.innerHTML = originalHtml;
        btn.disabled = false;
    }
});

// Xử lý Webcam
const video = document.getElementById('webcam-video');
const canvas = document.getElementById('webcam-canvas');
const btnStartCamera = document.getElementById('btn-start-camera');
const btnCaptureScan = document.getElementById('btn-capture-scan');
const placeholder = document.getElementById('webcam-placeholder');

btnStartCamera.addEventListener('click', async function() {
    if (stream) {
        stopWebcam();
        return;
    }

    try {
        stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
        video.srcObject = stream;
        video.style.display = 'block';
        placeholder.style.display = 'none';
        btnStartCamera.innerHTML = '<i class="bi bi-camera-video-off me-1"></i> Tắt Camera';
        btnStartCamera.classList.replace('btn-primary', 'btn-danger');
        btnCaptureScan.disabled = false;
    } catch (err) {
        console.error("Lỗi truy cập camera:", err);
        alert("Không thể truy cập Camera. Vui lòng kiểm tra quyền truy cập.");
    }
});

function stopWebcam() {
    if (stream) {
        stream.getTracks().forEach(track => track.stop());
        stream = null;
    }
    video.style.display = 'none';
    placeholder.style.display = 'block';
    btnStartCamera.innerHTML = '<i class="bi bi-camera me-1"></i> Bật Camera';
    btnStartCamera.classList.replace('btn-danger', 'btn-primary');
    btnCaptureScan.disabled = true;
}

btnCaptureScan.addEventListener('click', function() {
    // Chụp ảnh từ video vẽ lên canvas
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);

    // Hiển thị canvas (ảnh tĩnh) thay vì video stream
    video.style.display = 'none';
    canvas.style.display = 'block';

    // Lưu ảnh dưới dạng Base64
    const faceBase64 = canvas.toDataURL('image/jpeg');
    
    // Gắn vào một thẻ ẩn hoặc biến toàn cục để gửi kèm form Check-in
    // Ở đây ta mô phỏng bằng Alert thông báo thành công
    alert("Đã chụp và lưu trữ ảnh khuôn mặt thành công! Sẵn sàng đính kèm vào hồ sơ Check-in.");
    
    // Đổi nút Bật Camera thành Chụp Lại
    btnStartCamera.innerHTML = '<i class="bi bi-camera-video me-1"></i> Chụp lại';
    btnStartCamera.classList.replace('btn-danger', 'btn-primary');
    
    // Dừng Webcam để tiết kiệm tài nguyên sau khi chụp xong
    if (stream) {
        stream.getTracks().forEach(track => track.stop());
        stream = null;
    }
});

// Xử lý Nút Hoàn Tất trong Modal
document.getElementById('btn-complete-checkin').addEventListener('click', async function() {
    // Kiểm tra xem đã có dữ liệu chưa (nếu nhập tay thì lấy từ form tay)
    const activeTab = document.querySelector('#verifyTabs .nav-link.active').id;
    let data = document.getElementById('raw-data-result').value;

    if (activeTab === 'manual-tab') {
        const name = document.getElementById('manual-name').value;
        const idNum = document.getElementById('manual-id').value;
        if (!name || !idNum) {
            alert("Vui lòng nhập đầy đủ Họ tên và Số giấy tờ!");
            return;
        }
        const nationality = document.getElementById('manual-nationality').value;
        const dob = document.getElementById('manual-dob').value;
        data = JSON.stringify({ "Họ Tên": name, "Giấy tờ": idNum, "Quốc tịch": nationality, "Ngày sinh": dob }, null, 2);
    } else if (!data) {
        alert("Vui lòng thực hiện quét CCCD để lấy dữ liệu trước khi hoàn tất!");
        return;
    }

    if (!confirm('Xác nhận số phòng vật lý bàn giao, lập biên bản cọc và tiến hành Check-in?')) {
        return;
    }

    const id = document.getElementById('checkin-id').value;
    const maPhong = document.getElementById('checkin-room').value;
    const isPaidUpfront = document.getElementById('checkin-paid').value === 'true';

    const btn = this;
    const originalHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xử lý...';

    try {
        const response = await fetch(`${API_URL}/execute/${id}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ maPhong, isPaidUpfront, verifyData: data })
        });

        if (response.ok) {
            showAlert('Duyệt nhận phòng thành công!', 'success');
            const verifyModal = bootstrap.Modal.getInstance(document.getElementById('verifyIdModal'));
            verifyModal.hide();
            fetchCheckinData(); // Reload list
        } else {
            throw new Error('Thao tác thất bại');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Có lỗi xảy ra khi Check-in', 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalHtml;
    }
});

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
