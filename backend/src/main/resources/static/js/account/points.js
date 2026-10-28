/**
 * Trang điểm thưởng (/account/points) — lấy dữ liệu thật từ GET /account/api/v1/points
 * Không dùng Bootstrap. Dữ liệu mong đợi (payload.data):
 *   currentPoints, membershipTier, nextTier, nextTierThreshold, pointsToNextTier,
 *   history: [{ id, title, note, date, type: 'EARN' | 'REDEEM', points }]
 */
document.addEventListener('DOMContentLoaded', async () => {
    const $ = (id) => document.getElementById(id);
    const tbody = $('points-history-body');
    const searchInput = $('search-history');
    const progressText = document.querySelector('.points-progress-text');
    const progressBar = document.querySelector('.points-progress-bar');
    const nextMessage = document.querySelector('.points-next-message');

    let pointsData = { currentPoints: 0, history: [] };
    let currentFilter = 'all';

    const VND_PER_POINT = 1000;
    const formatNumber = (value) => new Intl.NumberFormat('vi-VN').format(value || 0);
    // Mọi chuỗi từ server đều qua đây trước khi đưa vào innerHTML
    const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    function tierKeyOf(name) {
        const n = String(name || '');
        if (n.includes('Bạch Kim')) return 'platinum';
        if (n.includes('Vàng')) return 'gold';
        if (n.includes('Bạc')) return 'silver';
        return 'bronze';
    }

    function tierImageOf(key) {
        return {
            bronze: '/images/icons/bronze.png',
            silver: '/images/icons/iron.png',
            gold: '/images/icons/gold.png',
            platinum: '/images/icons/platinum.png'
        }[key] || '/images/icons/bronze.png';
    }

    function renderSummary() {
        const pts = pointsData.currentPoints || 0;
        const history = pointsData.history || [];

        $('user-points').innerHTML = `${formatNumber(pts)} <small>điểm</small>`;
        $('user-vnd').textContent = `${formatNumber(pts * VND_PER_POINT)} VNĐ`;
        $('hero-points').textContent = formatNumber(pts);
        $('hero-value').textContent = `${formatNumber(pts * VND_PER_POINT)} đ`;
        $('hero-transactions').textContent = formatNumber(history.length);

        const tierName = pointsData.membershipTier || 'Hạng Đồng';
        $('user-tier').querySelector('b').textContent = tierName;

        const key = tierKeyOf(tierName);
        const tierIcon = $('user-tier-icon');
        if (tierIcon) tierIcon.src = tierImageOf(key);
        document.querySelectorAll('[data-tier-card]').forEach((card) => {
            card.classList.toggle('is-current', card.dataset.tierCard === key);
        });

        const threshold = pointsData.nextTierThreshold || pts;
        const progress = threshold ? Math.min(100, (pts / threshold) * 100) : 100;
        if (progressText) progressText.textContent = `${formatNumber(pts)} / ${formatNumber(threshold)} điểm`;
        if (progressBar) progressBar.style.width = `${progress}%`;
        if (nextMessage) {
            nextMessage.innerHTML = pointsData.pointsToNextTier > 0
                ? `Tích lũy thêm <strong>${formatNumber(pointsData.pointsToNextTier)} điểm</strong> để lên <strong>${esc(pointsData.nextTier)}</strong>.`
                : 'Bạn đang ở hạng thành viên cao nhất.';
        }
    }

    function renderHistory() {
        if (!tbody) return;
        const term = (searchInput?.value || '').toLowerCase().trim();
        const rows = (pointsData.history || []).filter((item) => {
            const text = `${item.id} ${item.title} ${item.note}`.toLowerCase();
            return (currentFilter === 'all' || item.type === currentFilter) && text.includes(term);
        });

        if (!rows.length) {
            tbody.innerHTML = `<tr><td colspan="4" class="mh-pt-empty">
                <i class="fa-solid fa-inbox"></i>Chưa có lịch sử giao dịch điểm.</td></tr>`;
            return;
        }

        tbody.innerHTML = rows.map((item) => {
            const earn = item.type === 'EARN';
            const cls = earn ? 'earn' : 'redeem';
            const sub = [item.id, item.note].filter(Boolean).map(esc).join(' · ');
            return `<tr>
                <td>
                    <div class="mh-pt-row-title">${esc(item.title)}</div>
                    ${sub ? `<div class="mh-pt-row-sub"><i class="fa-solid fa-hashtag"></i>${sub}</div>` : ''}
                </td>
                <td class="center"><span class="mh-pt-date">${esc(item.date)}</span></td>
                <td class="center"><span class="mh-pt-badge ${cls}">${earn ? 'Tích điểm' : 'Đổi điểm'}</span></td>
                <td class="right"><span class="mh-pt-amount ${cls}">${earn ? '+' : ''}${formatNumber(item.points)}</span></td>
            </tr>`;
        }).join('');
    }

    try {
        const response = await fetch('/account/api/v1/points', { credentials: 'same-origin' });
        const payload = await response.json();
        if (!response.ok || !payload.success) throw new Error(payload.message || 'Không thể tải điểm thưởng.');
        pointsData = payload.data;
        renderSummary();
        renderHistory();
    } catch (error) {
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="4" class="mh-pt-empty" style="color:var(--danger)">${esc(error.message)}</td></tr>`;
        }
    }

    document.querySelectorAll('.filter-btn').forEach((button) => button.addEventListener('click', () => {
        document.querySelectorAll('.filter-btn').forEach((b) => b.classList.remove('active'));
        button.classList.add('active');
        currentFilter = button.dataset.filter;
        renderHistory();
    }));
    searchInput?.addEventListener('input', renderHistory);
});