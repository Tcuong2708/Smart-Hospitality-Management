document.addEventListener('DOMContentLoaded', () => {
    // Mock data theo class CanhBaoRuiRo
    const mockWarnings = [
        { 
            id: 'CB001', bookingId: 'PDP1023', customerName: 'Lê Trường An', customerPhone: '0901234567',
            riskRatio: 85.5, level: 'Cao', cancelTime: '14:00:00', timerEnabled: true,
            reason: 'Khách chưa thanh toán cọc. Lịch sử No-show.'
        },
        { 
            id: 'CB002', bookingId: 'PDP1025', customerName: 'Trần Thị Bích', customerPhone: '0988765432',
            riskRatio: 92.0, level: 'Cao', cancelTime: '14:00:00', timerEnabled: true,
            reason: 'Tài khoản từng có lịch sử đặt phòng ảo (No-show).'
        },
        { 
            id: 'CB003', bookingId: 'PDP1040', customerName: 'Nguyễn Hải Nam', customerPhone: '0912345678',
            riskRatio: 65.0, level: 'Trung bình', cancelTime: '18:00:00', timerEnabled: true,
            reason: 'Thời gian đặt phòng sát giờ, thẻ bị từ chối.'
        },
        { 
            id: 'CB004', bookingId: 'PDP1055', customerName: 'Phạm Minh Tú', customerPhone: '0977654321',
            riskRatio: 40.0, level: 'Thấp', cancelTime: '18:00:00', timerEnabled: false, // đã liên hệ
            reason: 'Khách thông báo có thể đến trễ do chuyến bay delay.'
        }
    ];

    const tableBody = document.getElementById('table-body');
    let selectedRisks = []; // Cao, Trung bình, Thấp

    function initFilters() {
        const filterDropdown = document.getElementById('filterRiskDropdown');
        if (!filterDropdown) return;
        
        const levels = ['Cao', 'Trung bình', 'Thấp'];
        filterDropdown.innerHTML = levels.map((lvl, idx) => `
            <li>
                <div class="form-check mb-1">
                    <input class="form-check-input filter-risk-cb" type="checkbox" value="${lvl}" id="cb_risk_${idx}">
                    <label class="form-check-label" for="cb_risk_${idx}">Mức độ ${lvl}</label>
                </div>
            </li>
        `).join('');

        filterDropdown.addEventListener('click', e => e.stopPropagation());

        document.querySelectorAll('.filter-risk-cb').forEach(cb => {
            cb.addEventListener('change', () => {
                selectedRisks = Array.from(document.querySelectorAll('.filter-risk-cb:checked')).map(x => x.value);
                renderWarnings(mockWarnings);
            });
        });
    }
    
    function formatDate(dateString) {
        const d = new Date(dateString);
        return `${d.getDate().toString().padStart(2,'0')}/${(d.getMonth()+1).toString().padStart(2,'0')}/${d.getFullYear()} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`;
    }

    function renderWarnings(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';

        let filteredData = data;
        if (selectedRisks.length > 0) {
            filteredData = filteredData.filter(item => selectedRisks.includes(item.level));
        }
        
        if(filteredData.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-success fw-bold"><i class="bi bi-check-circle-fill me-2"></i>Tuyệt vời! Hiện không có cảnh báo rủi ro nào phù hợp với bộ lọc.</td></tr>';
            return;
        }

        // Sort descending by risk ratio
        filteredData.sort((a,b) => b.riskRatio - a.riskRatio);

        filteredData.forEach(item => {
            const tr = document.createElement('tr');
            
            let rowClass = '';
            let riskBadge = 'bg-secondary';
            if(item.level === 'Cao') {
                rowClass = 'risk-high';
                riskBadge = 'bg-danger';
            } else if(item.level === 'Trung bình') {
                rowClass = 'risk-medium';
                riskBadge = 'bg-warning text-dark';
            } else {
                riskBadge = 'bg-info text-dark';
            }

            // HTML Timer setup
            let timerHtml = `<div class="small text-muted fst-italic"><i class="bi bi-clock-history me-1"></i>Hủy lúc ${item.cancelTime}</div>`;
            if (item.timerEnabled) {
                timerHtml += `<div class="text-danger fw-bold countdown-timer" data-time="${item.cancelTime}"><i class="bi bi-stopwatch"></i> Đang đếm ngược...</div>`;
            } else {
                timerHtml += `<div class="text-success fw-bold"><i class="bi bi-pause-circle"></i> Đã dừng đếm ngược</div>`;
            }

            tr.className = rowClass;
            tr.innerHTML = `
                <td class="text-center fw-bold text-muted">${item.id}</td>
                <td class="text-center fw-bold text-navy">
                    <a href="#" class="text-decoration-none text-navy">${item.bookingId}</a>
                </td>
                <td class="text-center">
                    <div class="fw-bold">${item.customerName}</div>
                    <div class="small text-muted"><i class="bi bi-telephone-fill me-1"></i>${item.customerPhone}</div>
                </td>
                <td class="text-center">
                    <span class="badge ${riskBadge} fs-6 mb-1">${item.riskRatio}%</span>
                    <div class="small fw-bold text-muted">Mức độ: ${item.level}</div>
                </td>
                <td class="text-start small fst-italic text-muted">
                    ${item.reason}
                </td>
                <td class="text-center">
                    ${timerHtml}
                </td>
                <td class="text-center align-middle px-3" style="width: 140px;">
                    <div class="btn-group-vertical w-100 shadow-sm rounded" role="group">
                        ${item.timerEnabled ? `
                        <button type="button" class="btn btn-sm btn-light border btn-stop-timer text-start fw-bold py-2 text-secondary" data-id="${item.id}" title="Khách báo trễ">
                            <i class="bi bi-pause-circle-fill me-1"></i> Dừng đếm
                        </button>
                        ` : ''}
                        <button type="button" class="btn btn-sm btn-light border btn-notify text-start fw-bold py-2 text-primary" data-id="${item.id}">
                            <i class="bi bi-envelope-paper-fill me-1"></i> Nhắc nhở
                        </button>
                        <button type="button" class="btn btn-sm btn-danger btn-cancel text-start fw-bold py-2" data-id="${item.id}" style="background-color: #dc3545;">
                            <i class="bi bi-x-octagon-fill me-1 text-white"></i> Hủy ngay
                        </button>
                    </div>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        // Event listeners
        document.querySelectorAll('.btn-stop-timer').forEach(btn => {
            btn.addEventListener('click', (e) => {
                if(confirm('Khách hàng đã liên hệ xin nhận phòng trễ? Hệ thống sẽ HỦY bỏ bộ đếm ngược tự động hủy phòng cho đơn này.')) {
                    const id = e.currentTarget.dataset.id;
                    const warn = mockWarnings.find(w => w.id === id);
                    if(warn) warn.timerEnabled = false;
                    renderWarnings(mockWarnings);
                }
            });
        });
        
        document.querySelectorAll('.btn-cancel').forEach(btn => {
            btn.addEventListener('click', (e) => {
                if(confirm('Hệ thống AI đề xuất hủy phòng này do rủi ro quá cao. Bạn có chắc chắn muốn Hủy Phiếu Đặt Phòng này không?')) {
                    alert(`Đã hủy phiếu đặt phòng thành công.`);
                    const id = e.currentTarget.dataset.id;
                    const idx = mockWarnings.findIndex(w => w.id === id);
                    if(idx > -1) mockWarnings.splice(idx, 1);
                    renderWarnings(mockWarnings);
                }
            });
        });
    }

    // Khởi tạo bộ lọc trước
    initFilters();

    // Simulate AI loading
    setTimeout(() => {
        renderWarnings(mockWarnings);
    }, 1500);

    const btnReanalyze = document.getElementById('btn-reanalyze');
    if(btnReanalyze) {
        btnReanalyze.addEventListener('click', () => {
            tableBody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted"><div class="spinner-border text-primary"></div><div class="mt-2">Trợ lý ảo đang phân tích lại...</div></td></tr>';
            setTimeout(() => {
                renderWarnings(mockWarnings);
            }, 2000);
        });
    }
});
