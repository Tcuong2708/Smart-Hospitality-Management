const API_BASE_URL = 'http://localhost:8080/admin/api/rooms';

console.log("-> [MAY HOTEL]: Kích hoạt hệ thống tương tác Client REST API");

document.addEventListener("DOMContentLoaded", loadRoomsFromAPI);

// =========================================================================
// 1. HÀM GET: Kéo dữ liệu JSON đổ động vào bảng vật lý
// =========================================================================
function loadRoomsFromAPI() {
    const tbody = document.getElementById("apiRoomTableBody");
    if (!tbody) return;

    fetch(`${API_BASE_URL}/json`)
        .then(response => {
            if (!response.ok) throw new Error("Không thể kết nối máy chủ REST API.");
            return response.json();
        })
        .then(data => {
            tbody.innerHTML = "";

            if (!data || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" class="text-center py-5 text-muted">Kho CSDL API phòng hiện đang trống.</td></tr>`;
                return;
            }

            data.forEach(room => {
                const tr = document.createElement("tr");
                const formattedPrice = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(room.price);
                const statusBadge = room.maTrangThai === 1
                    ? '<span class="badge bg-success px-3 py-1 rounded-pill fw-bold">Trống</span>'
                    : '<span class="badge bg-danger px-3 py-1 rounded-pill fw-bold">Đang bận</span>';

                tr.innerHTML = `
                    <td class="ps-4 fw-bold text-navy font-monospace">#${room.id}</td>
                    <td class="fw-bold text-secondary">${room.name}</td>
                    <td class="text-danger fw-bold">${formattedPrice}</td>
                    <td><small class="text-muted font-monospace">${room.imageUrl || 'default.png'}</small></td>
                    <td class="text-center">${statusBadge}</td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-warning text-white shadow-sm me-1 fw-bold"
                                onclick="handleUpdateRoomPUT(${room.id}, '${room.name}', ${room.price})">
                            <i class="bi bi-pencil-square"></i> Sửa
                        </button>
                        <button class="btn btn-sm btn-danger shadow-sm fw-bold"
                                onclick="handleDeleteRoomDELETE(${room.id})">
                            <i class="bi bi-trash3-fill"></i> Xóa
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(error => {
            console.error("-> Lỗi Fetch API:", error);
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-5 text-danger fw-bold">Không thể tải dữ liệu phòng: ${error.message}</td></tr>`;
        });
}

// =========================================================================
// 2. HÀM POST: Thêm phòng mới
// =========================================================================
function handleAddNewRoomPOST() {
    const roomName = prompt("Nhập tên phòng muốn thêm:");
    if (!roomName || roomName.trim() === "") return;

    const roomPrice = prompt("Nhập giá tiền niêm yết/đêm:");
    if (!roomPrice || isNaN(roomPrice)) {
        alert("Đơn giá không hợp lệ!");
        return;
    }

    const rawBodyData = {
        name: roomName.trim(),
        price: parseFloat(roomPrice),
        maLoai: 1,
        maTrangThai: 1,
        imageUrl: "default.png",
        detail: "Tạo tự động qua API POST.",
        ghiChu: "API POST Test"
    };

    fetch(`${API_BASE_URL}/add`, {
        method: 'POST',
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(rawBodyData)
    })
    .then(response => {
        if (response.status === 201 || response.ok) {
            alert("Thêm phòng qua API POST thành công!");
            loadRoomsFromAPI();
        } else {
            throw new Error("Không thể xử lý yêu cầu thêm.");
        }
    })
    .catch(error => {
        alert("Lỗi thực thi POST: " + error.message);
    });
}

// =========================================================================
// 3. PUT: Chỉnh sửa thông tin phòng vật lý
// =========================================================================
function handleUpdateRoomPUT(id, currentName, currentPrice) {
    const newName = prompt(`[API PUT] Sửa tên phòng #${id}:`, currentName);
    if (newName === null || newName.trim() === "") return;

    const newPrice = prompt(`[API PUT] Sửa đơn giá phòng #${id}:`, currentPrice);
    if (newPrice === null || isNaN(newPrice)) {
        alert("Đơn giá nhập vào không hợp lệ!");
        return;
    }

    const rawBodyData = {
        name: newName.trim(),
        price: parseFloat(newPrice),
        imageUrl: "default.png",
        maTrangThai: 1,
        detail: "Dữ liệu được cập nhật tự động từ trang kiểm thử API Playground."
    };

    fetch(`${API_BASE_URL}/${id}`, {
        method: 'PUT',
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(rawBodyData)
    })
    .then(response => {
        if (response.ok) {
            alert(`Cập nhật thông tin phòng #${id} qua API PUT thành công!`);
            loadRoomsFromAPI();
        } else {
            throw new Error("Cập nhật thất bại, vui lòng kiểm tra ID.");
        }
    })
    .catch(error => {
        alert("Lỗi thực thi PUT Method: " + error.message);
    });
}

// =========================================================================
// 4. DELETE
// =========================================================================
function handleDeleteRoomDELETE(id) {
    if (!confirm(`CẢNH BÁO: Bạn có chắc chắn muốn xóa phòng #${id} vĩnh viễn ra khỏi hệ thống bằng phương thức API DELETE không?`)) return;

    fetch(`${API_BASE_URL}/${id}`, {
        method: 'DELETE',
    })
    .then(response => {
        if (response.ok) {
            alert(`Xóa phòng #${id} qua API DELETE thành công vĩnh viễn!`);
            loadRoomsFromAPI();
        } else {
            throw new Error("Không thể xóa phòng này do mã phòng đã phát sinh lịch sử hóa đơn khách ở!");
        }
    })
    .catch(error => {
        alert("Lỗi thực thi DELETE Method: " + error.message);
    });
}
