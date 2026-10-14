document.addEventListener('DOMContentLoaded', async () => {
    const tbody = document.getElementById('points-history-body');
    const userPointsEl = document.getElementById('user-points');
    const userVndEl = document.getElementById('user-vnd');
    const progressLabelEl = document.getElementById('tier-progress-label');
    const progressBarEl = document.getElementById('tier-progress-bar');
    const progressRemainingEl = document.getElementById('tier-progress-remaining');
    const filterBtns = document.querySelectorAll('.filter-btn');
    const searchInput = document.getElementById('search-history');

    let currentFilter = 'all';

    // Format currency
    const formatNumber = (num) => new Intl.NumberFormat('vi-VN').format(num);

    async function loadPoints() {
        const response = await fetch('/account/api/v1/points', {
            credentials: 'same-origin',
            cache: 'no-store'
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) throw new Error(payload.message || 'Không tải được điểm thưởng.');
        const pointsData = payload.data;
        const currentPoints = Math.max(0, Number(pointsData.currentPoints) || 0);
        const nextTierPoints = 10000;
        const progressPercent = Math.min(100, (currentPoints / nextTierPoints) * 100);
        const remainingPoints = Math.max(0, nextTierPoints - currentPoints);
        if (userPointsEl) userPointsEl.innerHTML = `${formatNumber(currentPoints)} <span class="fs-4 text-white">Điểm</span>`;
        if (userVndEl) userVndEl.innerHTML = `${formatNumber(currentPoints * 1000)} VNĐ`;
        if (progressLabelEl) progressLabelEl.textContent = `${formatNumber(currentPoints)} / ${formatNumber(nextTierPoints)} điểm`;
        if (progressBarEl) {
            progressBarEl.style.width = `${progressPercent}%`;
            progressBarEl.setAttribute('aria-valuenow', String(currentPoints));
            progressBarEl.setAttribute('aria-valuemax', String(nextTierPoints));
        }
        if (progressRemainingEl) progressRemainingEl.textContent = `${formatNumber(remainingPoints)} điểm`;
        const tierEl = document.getElementById('user-tier');
        if (tierEl) tierEl.innerHTML = `<i class="bi bi-star-fill me-1"></i>${pointsData.membershipTier}`;

        function renderHistory() {
        if (!tbody) return;
        const searchTerm = (searchInput ? searchInput.value : '').toLowerCase().trim();

        const filtered = pointsData.history.filter(item => {
            const matchFilter = currentFilter === 'all' || item.type === currentFilter;
            const matchSearch = item.id.toLowerCase().includes(searchTerm) || item.title.toLowerCase().includes(searchTerm) || item.note.toLowerCase().includes(searchTerm);
            return matchFilter && matchSearch;
        });

        if (filtered.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-5 text-muted">
                        <i class="bi bi-inbox fs-2 d-block mb-2"></i>
                        Không tìm thấy lịch sử giao dịch phù hợp.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = filtered.map(item => {
            const isEarn = item.type === 'EARN';
            const badgeClass = isEarn ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-danger-subtle text-danger border border-danger-subtle';
            const badgeText = isEarn ? 'Tích điểm' : 'Đổi điểm';
            const sign = isEarn ? '+' : '';
            const pointClass = isEarn ? 'text-success' : 'text-danger';

            return `
                <tr>
                    <td class="ps-4 py-3">
                        <div class="fw-bold text-navy">${item.title}</div>
                        <div class="small text-muted"><i class="bi bi-hash me-1"></i>${item.id} - ${item.note}</div>
                    </td>
                    <td class="text-center text-muted small">${item.date}</td>
                    <td class="text-center">
                        <span class="badge ${badgeClass} px-3 py-2 rounded-pill fw-bold">${badgeText}</span>
                    </td>
                    <td class="text-end pe-4 fw-bold fs-6 ${pointClass}">
                        ${sign}${formatNumber(item.points)}
                    </td>
                </tr>
            `;
        }).join('');
        }

        renderHistory();

        filterBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                filterBtns.forEach(b => b.classList.remove('active'));
                e.currentTarget.classList.add('active');
                currentFilter = e.currentTarget.dataset.filter;
                renderHistory();
            });
        });

        if (searchInput) searchInput.addEventListener('input', renderHistory);
    }

    loadPoints().catch(error => {
        console.error('[Points] Không thể tải dữ liệu điểm:', error);
        if (tbody) tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-danger">Không thể tải dữ liệu điểm thưởng.</td></tr>';
    });
});
