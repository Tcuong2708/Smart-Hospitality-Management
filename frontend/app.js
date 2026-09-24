/**
 * LÃµi Frontend Javascript Vanilla (Thuáº§n) káº¿t ná»‘i Backend Java & Python AI
 * Thay tháº¿ hoÃ n toÃ n JQuery & Thymeleaf
 */

document.addEventListener("DOMContentLoaded", () => {
    // 1. Khá»Ÿi táº¡o UI chung
    initUI();

    // 2. Fetch dá»¯ liá»‡u ngÆ°á»i dÃ¹ng tá»« Backend Java (localhost:8080)
    fetchUserData();

    // 3. Khá»Ÿi táº¡o luá»“ng Chatbot káº¿t ná»‘i Python (localhost:5000)
    setupChatBot();
});

/**
 * Xá»­ lÃ½ cÃ¡c UI khá»Ÿi táº¡o
 */
function initUI() {
    // Náº¿u cÃ³ query param success tá»« backend redirect vá», hiá»ƒn thá»‹ toast
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('success')) {
        showToast(urlParams.get('success'), 'success');
    }
    if (urlParams.has('error')) {
        showToast(urlParams.get('error'), 'error');
    }
}

/**
 * Hiá»ƒn thá»‹ thÃ´ng bÃ¡o Toast Ä‘áº¹p máº¯t cá»§a Bootstrap 5
 */
function showToast(message, type = 'success') {
    const toastNode = document.getElementById('toastNotification');
    const iconNode = document.getElementById('toastIcon');
    const messageNode = document.getElementById('toastMessage');

    if (!toastNode) return;

    messageNode.innerText = message;
    toastNode.classList.remove('bg-navy', 'bg-danger', 'bg-secondary');

    if (type === 'success') {
        toastNode.style.backgroundColor = 'var(--primary-color)';
        iconNode.className = "bi bi-check-circle-fill me-3 fs-4 text-warning";
    } else if (type === 'error') {
        toastNode.classList.add('bg-danger');
        iconNode.className = "bi bi-exclamation-triangle-fill me-3 fs-4 text-white";
    } else {
        toastNode.classList.add('bg-secondary');
        iconNode.className = "bi bi-info-circle-fill me-3 fs-4 text-white";
    }

    const bsToast = new bootstrap.Toast(toastNode, { delay: 3500 });
    bsToast.show();
}

/**
 * Hàm lấy dữ liệu người dùng qua API của Java Backend bằng fetch
 */
async function fetchUserData() {
    // Ưu tiên dùng Mock Data từ localStorage để test phân quyền Frontend
    const mockRole = localStorage.getItem('userRole');
    const mockUsername = localStorage.getItem('username');

    if (mockRole) {
        let roleID = 3; // guest
        if (mockRole === 'admin') roleID = 1; // Quản trị viên
        else if (mockRole === 'director' || mockRole === 'manager') roleID = 7; // Bộ phận quản lý
        else if (mockRole === 'accountant') roleID = 6; // Kế toán
        else if (mockRole === 'housekeeping') roleID = 4; // Buồng phòng
        else if (mockRole === 'staff' || mockRole === 'receptionist') roleID = 2; // Lễ tân

        const mockData = {
            isLoggedIn: true,
            user: { username: mockUsername || mockRole, roleID: roleID },
            cartQuantity: 0
        };
        renderUserMenu(mockData);
        return; // Dừng, không call API để mock chạy trơn tru
    }

    try {
        const API_URL = "http://localhost:8080/api/user/profile";

        const response = await fetch(API_URL, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });

        if (response.ok) {
            const data = await response.json();
            renderUserMenu(data);
        } else {
            renderUserMenu(null); // Render menu khách (chưa đăng nhập)
        }

    } catch (error) {
        console.warn("API Backend chưa chạy, hiển thị menu Khách:", error.message);
        renderUserMenu(null);
    }
}

function renderUserMenu(data) {
    const basePath = window.APP_BASE_PATH || '';

    const dropdownMenu = document.getElementById('user-dropdown-menu');
    const userIcon = document.getElementById('user-icon');

    let html = [];

    if (!data || !data.isLoggedIn) {
        // --- GIAO DIỆN CHƯA ĐĂNG NHẬP ---
        userIcon.style.opacity = '0.6';
        
        // Hiển thị lại Menu chính và Chatbot cho Khách vãng lai
        document.querySelectorAll('.navbar-nav .nav-item:not(#user-menu-container)').forEach(el => el.style.display = 'block');
        const chatCircle = document.getElementById('chat-circle');
        if (chatCircle) chatCircle.style.display = 'flex';
        const footer = document.querySelector('footer');
        if (footer) footer.style.display = 'block';

        html.push(`
            <li><a class="dropdown-item" href="${basePath}account/login.html"><i class="bi bi-box-arrow-in-right me-2"></i>Đăng nhập</a></li>
            <li><a class="dropdown-item" href="${basePath}account/register.html"><i class="bi bi-pencil-square me-2"></i>Đăng ký</a></li>
        `);
    } else {
        // --- GIAO DIỆN ĐÃ ĐĂNG NHẬP ---
        userIcon.style.opacity = '1';
        const roleID = data.user.roleID;
        const username = data.user.username;

        // Xử lý hiển thị Menu chính và Chatbot dựa trên role
        if (roleID !== 3) {
            // Ẩn Menu chính và Chatbot đối với Quản lý, Admin, Lễ tân...
            document.querySelectorAll('.navbar-nav .nav-item:not(#user-menu-container)').forEach(el => el.style.display = 'none');
            const chatCircle = document.getElementById('chat-circle');
            if (chatCircle) chatCircle.style.display = 'none';
            const footer = document.querySelector('footer');
            if (footer) footer.style.display = 'none';
        } else {
            // Hiện đầy đủ đối với Khách hàng
            document.querySelectorAll('.navbar-nav .nav-item:not(#user-menu-container)').forEach(el => el.style.display = 'block');
            const chatCircle = document.getElementById('chat-circle');
            if (chatCircle) chatCircle.style.display = 'flex';
            const footer = document.querySelector('footer');
            if (footer) footer.style.display = 'block';
        }

        // Header xin chào
        html.push(`
            <li>
                <div class="dropdown-header-user px-3 py-2 fw-bold text-primary-custom border-bottom" style="background-color: #f8f9fa;">
                    Xin chào, ${username}
                </div>
            </li>
        `);

        if (roleID === 1) {
            // QUẢN TRỊ VIÊN HỆ THỐNG
            html.push(`
                <li><div class="dropdown-header text-uppercase text-primary-custom fw-bold mt-2" style="font-size: 0.75rem; padding-left: 1rem;">Quản trị hệ thống</div></li>
                <li><a class="dropdown-item" href="${basePath}admin/users/index.html"><i class="bi bi-people-fill me-2" style="color: var(--accent-color);"></i>Quản lý Người dùng</a></li>
                <li><a class="dropdown-item" href="${basePath}admin/backup/index.html"><i class="bi bi-database-check me-2" style="color: var(--accent-color);"></i>Quản lý Sao lưu</a></li>
                <li><a class="dropdown-item" href="${basePath}admin/predictions/index.html"><i class="bi bi-robot me-2 text-danger"></i>Trợ lý AI - Rủi ro No-show</a></li>
                <li><hr class="dropdown-divider" /></li>
                <li><a class="dropdown-item" href="${basePath}account/profile.html"><i class="bi bi-person-circle me-2" style="color: var(--accent-color);"></i>Hồ sơ của tôi</a></li>
                <li><a class="dropdown-item text-danger fw-bold mt-2" href="#" id="btnLogout"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
            `);
        } else if (roleID === 7) {
            // BỘ PHẬN QUẢN LÝ
            html.push(`
                <li><div class="dropdown-header text-uppercase text-primary-custom fw-bold mt-2" style="font-size: 0.75rem; padding-left: 1rem;">Bộ phận Quản lý</div></li>
                <li><a class="dropdown-item" href="${basePath}manager/rooms/index.html"><i class="bi bi-houses-fill me-2" style="color: var(--accent-color);"></i>Quản lý Phòng</a></li>
                <li><a class="dropdown-item" href="${basePath}manager/category/index.html"><i class="bi bi-tags-fill me-2" style="color: var(--accent-color);"></i>Quản lý Loại phòng</a></li>
                <li><a class="dropdown-item" href="${basePath}manager/service/index.html"><i class="bi bi-stars me-2" style="color: var(--accent-color);"></i>Quản lý Dịch vụ</a></li>
                <li><a class="dropdown-item" href="${basePath}manager/customers/index.html"><i class="bi bi-person-badge-fill me-2" style="color: var(--accent-color);"></i>Quản lý Khách hàng</a></li>

                <li><a class="dropdown-item" href="${basePath}manager/promotions/index.html"><i class="bi bi-gift-fill me-2" style="color: var(--accent-color);"></i>Quản lý Khuyến mãi</a></li>
                <li><a class="dropdown-item" href="${basePath}manager/loyalty/index.html"><i class="bi bi-star-fill me-2" style="color: var(--accent-color);"></i>Chính sách Tích điểm</a></li>
                <li><a class="dropdown-item" href="${basePath}manager/statistical/index.html"><i class="bi bi-graph-up-arrow me-2" style="color: var(--accent-color);"></i>Thống kê Doanh thu</a></li>
                <li><a class="dropdown-item" href="${basePath}manager/reports/index.html"><i class="bi bi-file-earmark-bar-graph-fill me-2" style="color: var(--accent-color);"></i>Xuất Báo cáo thống kê</a></li>
                <li><a class="dropdown-item" href="${basePath}admin/predictions/index.html"><i class="bi bi-robot me-2 text-danger"></i>Trợ lý AI - Rủi ro No-show</a></li>
                <li><hr class="dropdown-divider" /></li>
                <li><a class="dropdown-item" href="${basePath}account/profile.html"><i class="bi bi-person-circle me-2" style="color: var(--accent-color);"></i>Hồ sơ của tôi</a></li>
                <li><a class="dropdown-item text-danger fw-bold mt-2" href="#" id="btnLogout"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
            `);
        } else if (roleID === 6) {
            // KẾ TOÁN
            html.push(`
                <li><div class="dropdown-header text-uppercase text-primary-custom fw-bold mt-2" style="font-size: 0.75rem; padding-left: 1rem;">Nghiệp vụ Kế toán</div></li>
                <li><a class="dropdown-item" href="${basePath}manager/statistical/index.html"><i class="bi bi-graph-up-arrow me-2" style="color: var(--accent-color);"></i>Thống kê Doanh thu</a></li>
                <li><a class="dropdown-item" href="${basePath}manager/reports/index.html"><i class="bi bi-file-earmark-bar-graph-fill me-2" style="color: var(--accent-color);"></i>Xuất Báo cáo thống kê</a></li>
                <li><hr class="dropdown-divider" /></li>
                <li><a class="dropdown-item" href="${basePath}account/profile.html"><i class="bi bi-person-circle me-2" style="color: var(--accent-color);"></i>Hồ sơ của tôi</a></li>
                <li><a class="dropdown-item text-danger fw-bold mt-2" href="#" id="btnLogout"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
            `);

        } else if (roleID === 4) {
            // NHÂN VIÊN BUỒNG PHÒNG
            html.push(`
                <li><div class="dropdown-header text-uppercase text-primary-custom fw-bold mt-2" style="font-size: 0.75rem; padding-left: 1rem;">Nghiệp vụ Buồng phòng</div></li>
                <li><a class="dropdown-item" href="${basePath}staff/room-status/index.html"><i class="bi bi-arrow-repeat me-2" style="color: var(--accent-color);"></i>Cập nhật Trạng thái phòng</a></li>
                <li><hr class="dropdown-divider" /></li>
                <li><a class="dropdown-item" href="${basePath}account/profile.html"><i class="bi bi-person-circle me-2" style="color: var(--accent-color);"></i>Hồ sơ của tôi</a></li>
                <li><a class="dropdown-item text-danger fw-bold mt-2" href="#" id="btnLogout"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
            `);
        } else if (roleID === 2) {
            // LỄ TÂN
            html.push(`
                <li><div class="dropdown-header text-uppercase text-primary-custom fw-bold mt-1" style="font-size: 0.75rem; padding-left: 1rem;">Nghiệp vụ lễ tân</div></li>
                <li><a class="dropdown-item" href="${basePath}staff/room-map/index.html"><i class="bi bi-grid-3x3-gap-fill me-2" style="color: var(--accent-color);"></i>Sơ đồ phòng trực quan</a></li>
                <li><a class="dropdown-item" href="${basePath}receptionist/booking/index.html"><i class="bi bi-calendar-check-fill me-2" style="color: var(--accent-color);"></i>Quản lý Đặt phòng</a></li>
                <li><a class="dropdown-item" href="${basePath}receptionist/invoice/index.html"><i class="bi bi-journal-bookmark-fill me-2" style="color: var(--accent-color);"></i>Quản lý Hoá Đơn</a></li>
                <li><a class="dropdown-item" href="${basePath}receptionist/check-in/index.html"><i class="bi bi-box-arrow-in-right me-2" style="color: var(--accent-color);"></i>Làm thủ tục Nhận phòng</a></li>
                <li><a class="dropdown-item" href="${basePath}receptionist/check-out/index.html"><i class="bi bi-box-arrow-left me-2" style="color: var(--accent-color);"></i>Làm thủ tục Trả phòng</a></li>
                <li><a class="dropdown-item fw-bold" style="color: #c5a017; background-color: #fdfaf0;" href="${basePath}admin/predictions/index.html"><i class="bi bi-robot me-2 text-danger"></i>Trợ lý AI - Rủi ro No-show</a></li>
                <li><hr class="dropdown-divider" /></li>
                <li><a class="dropdown-item" href="${basePath}account/profile.html"><i class="bi bi-person-circle me-2" style="color: var(--accent-color);"></i>Hồ sơ của tôi</a></li>
                <li><a class="dropdown-item text-danger fw-bold mt-2" href="#" id="btnLogout"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
            `);
        } else {
            // KHÁCH HÀNG
            html.push(`
                <li><div class="dropdown-header text-uppercase text-primary-custom fw-bold mt-1" style="font-size: 0.75rem; padding-left: 1rem;">Khách hàng</div></li>
                <li><a class="dropdown-item" href="${basePath}home/booking/history.html"><i class="bi bi-clock-history me-2" style="color: var(--accent-color);"></i>Lịch sử đặt phòng</a></li>
                <li><hr class="dropdown-divider" /></li>
                <li><a class="dropdown-item" href="${basePath}account/profile.html"><i class="bi bi-person-circle me-2" style="color: var(--accent-color);"></i>Hồ sơ của tôi</a></li>
                <li><a class="dropdown-item text-danger fw-bold mt-2" href="#" id="btnLogout"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
            `);
        }
    }

    dropdownMenu.innerHTML = html.join('');
    dropdownMenu.classList.add('dropdown-menu-scrollable');

    // Bắt sự kiện Đăng xuất
    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) {
        btnLogout.addEventListener('click', async (e) => {
            e.preventDefault();

            // Xóa dữ liệu mock trong local storage
            localStorage.removeItem('userRole');
            localStorage.removeItem('username');

            // Call API đăng xuất nếu có
            try {
                // await fetch('http://localhost:8080/api/auth/logout', { method: 'POST' }); // Tạm ẩn để test FrontEnd
                window.location.href = basePath + 'index.html'; // redirect về trang chủ
            } catch (err) {
            }
        });
    }
}

/**
 * Thuáº§n hoÃ¡ JQuery Chatbot thÃ nh Vanilla JS
 */
function setupChatBot() {
    const chatCircle = document.getElementById('chat-circle');
    const chatBox = document.getElementById('chat-box');
    const chatClose = document.getElementById('chat-box-close');
    const chatInput = document.getElementById('chat-input');
    const chatSubmit = document.getElementById('chat-submit');
    const chatLogs = document.getElementById('chat-logs');

    // Báº­t Khung chat
    chatCircle.addEventListener('click', () => {
        chatCircle.style.display = 'none';
        chatBox.style.display = 'flex';
        scrollToBottom();
    });

    // Táº¯t Khung chat
    chatClose.addEventListener('click', () => {
        // ThÃªm animation fadeOut (tÃ¹y chá»n)
        chatBox.style.display = 'none';
        chatCircle.style.display = 'flex';
    });

    // NÃºt Gá»­i
    chatSubmit.addEventListener('click', sendMessage);

    // GÃµ Enter
    chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            sendMessage();
        }
    });

    async function sendMessage() {
        const msg = chatInput.value.trim();
        if (!msg) return;

        // 1. In tin nháº¯n user lÃªn mÃ n hÃ¬nh (pháº£i)
        appendMessage(msg, 'msg-user');
        chatInput.value = '';

        // 2. Hiá»‡u á»©ng Loading
        const loadingId = 'loading-' + Date.now();
        appendMessage(
            '<span class="spinner-border spinner-border-sm text-warning me-2"></span> Luna Ä‘ang xá»­ lÃ½...',
            'msg-bot text-muted fst-italic',
            loadingId
        );

        try {
            // 3. Fetch API tá»›i Python AI Server
            const response = await fetch("http://127.0.0.1:5000/predict", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ message: msg })
            });

            if (!response.ok) throw new Error("Network response was not ok");

            const data = await response.json();

            // 4. XÃ³a loading, hiá»ƒn thá»‹ káº¿t quáº£
            document.getElementById(loadingId)?.remove();

            // Xá»­ lÃ½ xuá»‘ng dÃ²ng
            const formatAnswer = data.answer.replace(/\n/g, "<br>");
            appendMessage(formatAnswer, 'msg-bot shadow-sm');

        } catch (error) {
            document.getElementById(loadingId)?.remove();
            appendMessage('<i class="bi bi-wifi-off me-2"></i>Lá»—i káº¿t ná»‘i Python Server (Port 5000) - AI Äang Ngá»§!', 'msg-bot bg-danger text-white border-0');
            console.error("AI Fetch Error:", error);
        }
    }

    function appendMessage(html, className, id = null) {
        const div = document.createElement('div');
        div.className = className;
        div.innerHTML = html;
        if (id) div.id = id;
        chatLogs.appendChild(div);
        scrollToBottom();
    }

    function scrollToBottom() {
        chatLogs.scrollTop = chatLogs.scrollHeight;
    }
}

