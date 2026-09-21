document.addEventListener('DOMContentLoaded', () => {
    // Inject modal to body
    const modalHTML = `
        <div class="modal fade" id="statusModal" tabindex="-1">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg">
              <div class="modal-header text-white" style="background-color: var(--navy-color, #0F2942);">
                <h5 class="modal-title fw-bold">Cập nhật trạng thái <span id="modalRoomName" style="color: var(--accent-color, #C5A017);"></span></h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body p-4">
                <label class="form-label fw-bold text-navy">Chọn trạng thái mới:</label>
                <select class="form-select form-select-lg mb-3 shadow-sm border-0 bg-light" id="newStatusSelect">
                    <option value="Phòng trống">Phòng trống (Xanh lá)</option>
                    <option value="Đang bảo trì">Đang bảo trì (Xám)</option>
                </select>
                <div id="modalError" class="alert alert-danger d-none small mb-0"><i class="bi bi-exclamation-triangle-fill me-2"></i><span></span></div>
              </div>
              <div class="modal-footer bg-light border-0">
                <button type="button" class="btn btn-secondary fw-bold shadow-sm" data-bs-dismiss="modal">Hủy</button>
                <button type="button" class="btn text-white fw-bold px-4 shadow-sm" id="btnSaveStatus" style="background-color: var(--accent-color, #C5A017); border:none;">Lưu</button>
              </div>
            </div>
          </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHTML);

    const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
    let currentEditingItem = null;

    let mockData = [
        { floor: 'Tầng 1', room: 'P101', type: 'Standard', status: 'Chờ dọn dẹp', note: 'Khách vừa trả phòng' },
        { floor: 'Tầng 1', room: 'P102', type: 'Deluxe', status: 'Phòng trống', note: 'Đã dọn sạch' },
        { floor: 'Tầng 2', room: 'P201', type: 'Suite', status: 'Cần bảo trì', note: 'Hỏng máy lạnh' }
    ];
    
    const tableBody = document.getElementById('table-body');
    
    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        data.forEach(item => {
            let statusBadge = '';
            if (item.status === 'Phòng trống') {
                statusBadge = 'bg-success'; // Xanh lá
            } else if (item.status === 'Đang bảo trì') {
                statusBadge = 'bg-secondary'; // Xám
            } else if (item.status === 'Chờ dọn dẹp') {
                statusBadge = 'bg-warning text-dark';
            } else if (item.status === 'Cần bảo trì') {
                statusBadge = 'bg-danger';
            } else {
                statusBadge = 'bg-primary';
            }

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold">${item.floor}</td>
                <td class="text-center fw-bold" style="color: var(--navy-color, #0F2942);">${item.room}</td>
                <td class="text-center">${item.type}</td>
                <td class="text-center"><span class="badge ${statusBadge} px-3 py-2 rounded-pill shadow-sm">${item.status}</span></td>
                <td class="text-center">${item.note}</td>
                <td class="text-center">
                    <button class="btn btn-sm text-white fw-bold shadow-sm btn-update-status" data-room="${item.room}" style="background-color: var(--accent-color, #C5A017); border: none;">
                        Cập nhật <i class="bi bi-arrow-clockwise ms-1"></i>
                    </button>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        // Gắn sự kiện click cho các nút cập nhật
        document.querySelectorAll('.btn-update-status').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const roomName = e.currentTarget.getAttribute('data-room');
                currentEditingItem = mockData.find(r => r.room === roomName);
                
                document.getElementById('modalRoomName').textContent = roomName;
                document.getElementById('modalError').classList.add('d-none');
                document.getElementById('newStatusSelect').value = 'Phòng trống';
                
                statusModal.show();
            });
        });
    }

    renderData(mockData);

    document.getElementById('btnSaveStatus').addEventListener('click', async () => {
        const btnSave = document.getElementById('btnSaveStatus');
        const errorBox = document.getElementById('modalError');
        const errorText = errorBox.querySelector('span');
        const newStatus = document.getElementById('newStatusSelect').value;

        btnSave.disabled = true;
        btnSave.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...';
        errorBox.classList.add('d-none');

        try {
            // Giả lập API call mất 1 giây
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Theo UC08, giả lập lỗi hệ thống (tỉ lệ 20% mất kết nối mạng)
            if (Math.random() < 0.2) {
                throw new Error('Cập nhật thất bại, vui lòng kiểm tra kết nối');
            }

            // Cập nhật thành công, đổi màu badge thành Xanh lá hoặc Xám
            currentEditingItem.status = newStatus;
            if(newStatus === 'Phòng trống') currentEditingItem.note = 'Đã sẵn sàng đón khách';
            else if(newStatus === 'Đang bảo trì') currentEditingItem.note = 'Nhân viên kỹ thuật đang xử lý';
            
            renderData(mockData);
            statusModal.hide();
            
            if(typeof showToast === 'function') {
                showToast('Cập nhật trạng thái thành công', 'success');
            }

        } catch (error) {
            errorText.textContent = error.message;
            errorBox.classList.remove('d-none');
        } finally {
            btnSave.disabled = false;
            btnSave.innerHTML = 'Lưu';
        }
    });
});
