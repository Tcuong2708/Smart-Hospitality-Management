document.addEventListener('DOMContentLoaded', () => {
    // Sửa lỗi backdrop che modal
    ['customerModal', 'customerDetailModal'].forEach(id => {
        const m = document.getElementById(id);
        if (m) {
            document.body.appendChild(m);
            if (id === 'customerModal') {
                m.addEventListener('hidden.bs.modal', () => {
                    document.getElementById('customerForm').reset();
                    document.getElementById('customerModalTitle').innerText = 'Thêm Khách Hàng';
                });
            }
        }
    });

    const mockData = [
        { 
            id: 1, name: 'Nguyễn Văn A', email: 'nva@gmail.com', phone: '0901234567', 
            dob: '1990-05-15', cccd: '079090123456', points: 1500, expiry: '2027-12-31',
            tier: 'Vàng', status: 1,
            bookings: [{ id: 'B101', date: '2026-05-10', range: '15/06 - 18/06', status: 'Hoàn thành' }],
            invoices: [{ id: 'INV101', date: '2026-06-18', total: '3,600,000', status: 'Đã thanh toán' }],
            reviews: [{ room: 'Phòng 101', rate: 5, content: 'Phòng sạch sẽ, phục vụ tốt.' }]
        },
        { 
            id: 2, name: 'Trần Thị B', email: 'ttb@gmail.com', phone: '0912345678', 
            dob: '1985-08-20', cccd: '079085123789', points: 500, expiry: '2026-10-15',
            tier: 'Bạc', status: 1,
            bookings: [], invoices: [], reviews: []
        }
    ];

    const tableBody = document.getElementById('table-body');
    
    function formatDate(dateStr) {
        if(!dateStr) return '';
        const d = new Date(dateStr);
        return `${d.getDate().toString().padStart(2,'0')}/${(d.getMonth()+1).toString().padStart(2,'0')}/${d.getFullYear()}`;
    }

    function renderData(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        if(data.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">Không tìm thấy khách hàng nào.</td></tr>';
            return;
        }

        data.forEach(item => {
            const tr = document.createElement('tr');
            
            let tierBadge = 'bg-secondary';
            if(item.tier === 'Vàng') tierBadge = 'bg-warning text-dark';
            else if(item.tier === 'Bạc') tierBadge = 'bg-info text-dark';
            else if(item.tier === 'Kim Cương') tierBadge = 'bg-primary';

            tr.innerHTML = `
                <td class="text-center fw-bold text-navy">${item.name}</td>
                <td class="text-center">${formatDate(item.dob)}</td>
                <td class="text-center fw-bold text-muted">${item.cccd}</td>
                <td class="text-center">
                    <div class="small"><i class="bi bi-telephone text-muted me-1"></i>${item.phone}</div>
                    <div class="small"><i class="bi bi-envelope text-muted me-1"></i>${item.email}</div>
                </td>
                <td class="text-center">
                    <span class="badge ${tierBadge} mb-1">${item.tier}</span>
                    <div class="small text-danger fw-bold">${item.points.toLocaleString()} điểm</div>
                </td>
                <td class="text-center"><span class="badge bg-success">Hoạt động</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-info text-white btn-view" data-id="${item.id}" title="Xem chi tiết"><i class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-warning text-white btn-edit" data-id="${item.id}" title="Sửa"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-danger text-white btn-delete" title="Xóa"><i class="bi bi-trash"></i></button>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        // Event listeners cho các nút thao tác
        document.querySelectorAll('.btn-view').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = parseInt(e.currentTarget.dataset.id);
                const customer = mockData.find(c => c.id === id);
                if(customer) showCustomerDetail(customer);
            });
        });
        
        document.querySelectorAll('.btn-edit').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = parseInt(e.currentTarget.dataset.id);
                const c = mockData.find(c => c.id === id);
                if (c) {
                    document.getElementById('customerModalTitle').innerText = 'Cập Nhật Khách Hàng';
                    document.getElementById('form-name').value = c.name;
                    document.getElementById('form-dob').value = c.dob;
                    document.getElementById('form-cccd').value = c.cccd;
                    document.getElementById('form-phone').value = c.phone;
                    document.getElementById('form-email').value = c.email;
                    document.getElementById('form-tier').value = c.tier;
                    document.getElementById('form-points').value = c.points;
                    new bootstrap.Modal(document.getElementById('customerModal')).show();
                }
            });
        });
        
        document.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', () => {
                if(confirm('Bạn có chắc chắn muốn xóa khách hàng này?')) alert('Đã xóa!');
            });
        });
    }

    function showCustomerDetail(c) {
        document.getElementById('detail-name').textContent = c.name;
        document.getElementById('detail-tier').textContent = 'Thẻ ' + c.tier;
        document.getElementById('detail-dob').textContent = formatDate(c.dob);
        document.getElementById('detail-cccd').textContent = c.cccd;
        document.getElementById('detail-phone').textContent = c.phone;
        document.getElementById('detail-email').textContent = c.email;
        document.getElementById('detail-points').textContent = c.points.toLocaleString();
        document.getElementById('detail-expiry').textContent = formatDate(c.expiry);

        // Render Bookings
        const bookingList = document.getElementById('detail-booking-list');
        bookingList.innerHTML = c.bookings.length ? c.bookings.map(b => `
            <tr><td>#${b.id}</td><td>${formatDate(b.date)}</td><td>${b.range}</td><td><span class="badge bg-success">${b.status}</span></td></tr>
        `).join('') : '<tr><td colspan="4" class="text-center text-muted">Chưa có giao dịch.</td></tr>';

        // Render Invoices
        const invoiceList = document.getElementById('detail-invoice-list');
        invoiceList.innerHTML = c.invoices.length ? c.invoices.map(i => `
            <tr><td>#${i.id}</td><td>${formatDate(i.date)}</td><td class="text-danger fw-bold">${i.total} đ</td><td><span class="badge bg-success">${i.status}</span></td></tr>
        `).join('') : '<tr><td colspan="4" class="text-center text-muted">Chưa có hóa đơn.</td></tr>';

        // Render Reviews
        const reviewList = document.getElementById('detail-review-list');
        reviewList.innerHTML = c.reviews.length ? c.reviews.map(r => `
            <div class="p-3 bg-white border rounded">
                <div class="d-flex justify-content-between mb-2">
                    <strong class="text-navy">${r.room}</strong>
                    <span class="text-warning">${'★'.repeat(r.rate)}${'☆'.repeat(5-r.rate)}</span>
                </div>
                <p class="mb-0 text-muted fst-italic">"${r.content}"</p>
            </div>
        `).join('') : '<div class="text-center text-muted">Chưa có đánh giá nào.</div>';

        new bootstrap.Modal(document.getElementById('customerDetailModal')).show();
    }

    // Lọc và Tìm kiếm
    const searchInput = document.getElementById('search-input');
    const thFilterTier = document.getElementById('thFilterTier');
    
    if (thFilterTier) {
        const tiers = ['Đồng', 'Bạc', 'Vàng', 'Kim Cương'];
        thFilterTier.innerHTML = tiers.map((tier, idx) => `
            <li>
                <div class="form-check mb-1 ms-1">
                    <input class="form-check-input th-cb-tier" type="checkbox" value="${tier}" id="th_tier_${idx}">
                    <label class="form-check-label" for="th_tier_${idx}">Thẻ ${tier}</label>
                </div>
            </li>
        `).join('');

        document.querySelectorAll('.th-cb-tier').forEach(cb => {
            cb.addEventListener('change', filterData);
        });
    }

    function filterData() {
        const q = (searchInput ? searchInput.value : '').toLowerCase();
        const selectedTiers = Array.from(document.querySelectorAll('.th-cb-tier:checked')).map(cb => cb.value);

        const filtered = mockData.filter(c => {
            const matchQuery = c.name.toLowerCase().includes(q) || c.phone.includes(q) || c.cccd.includes(q);
            const matchTier = selectedTiers.length === 0 || selectedTiers.includes(c.tier);
            return matchQuery && matchTier;
        });
        renderData(filtered);
    }
    
    if(searchInput) searchInput.addEventListener('input', filterData);

    renderData(mockData);
});
