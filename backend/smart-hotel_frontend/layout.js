// Auto-detect the base path of the vanilla-frontend folder using relative path
const scriptTags = document.getElementsByTagName('script');
let currentScriptSrc = '';
for (let tag of scriptTags) {
    const srcAttr = tag.getAttribute('src');
    if (srcAttr && srcAttr.includes('layout.js')) {
        currentScriptSrc = srcAttr;
        break;
    }
}
const basePath = currentScriptSrc.substring(0, currentScriptSrc.indexOf('layout.js'));

const NAVBAR_HTML = `
  <nav class="navbar navbar-expand-lg sticky-top glass-navbar">
    <div class="container">
      <a class="navbar-brand" href="${basePath}index.html">
        <i class="bi bi-buildings-fill me-2"></i>MAY HOTEL
      </a>
      
      <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu">
        <span class="navbar-toggler-icon"></span>
      </button>
      
      <div class="collapse navbar-collapse" id="navMenu">
        <ul class="navbar-nav ms-auto align-items-center">
          <li class="nav-item"><a class="nav-link" href="${basePath}index.html">Trang chủ</a></li>
          <li class="nav-item"><a class="nav-link" href="${basePath}home/info/info.html">Thông tin</a></li>
          <li class="nav-item"><a class="nav-link" href="${basePath}home/rooms/list.html">Phòng nghỉ</a></li>
          <li class="nav-item"><a class="nav-link" href="${basePath}home/review/review.html">Đánh giá</a></li>

          <!-- Dropdown Tài khoản User -->
          <li class="nav-item dropdown" id="user-menu-container">
            <a class="nav-icon-btn dropdown-toggle p-0" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              <i class="bi bi-person-circle" id="user-icon" style="opacity: 0.6;"></i>
            </a>
            <ul class="dropdown-menu dropdown-menu-end dropdown-menu-custom" id="user-dropdown-menu">
              <!-- Rendered via app.js -->
            </ul>
          </li>
        </ul>
      </div>
    </div>
  </nav>
`;

const FOOTER_HTML = `
  <footer>
    <div class="container">
      <div class="row gy-5">
        <div class="col-md-4">
          <h5 class="brand-footer"><i class="bi bi-buildings-fill me-2"></i>MAY HOTEL</h5>
          <p>Trải nghiệm sự sang trọng và tiện nghi bậc nhất. Chúng tôi cam kết mang đến cho bạn những kỳ nghỉ không thể nào quên.</p>
        </div>
        <div class="col-md-4">
          <h5>Liên hệ</h5>
          <p class="mb-2"><i class="bi bi-envelope me-2"></i> Email: MayHotel.hotel@gmail.com</p>
          <p class="mb-2"><i class="bi bi-telephone me-2"></i> Hotline: 1800 9327</p>
          <p><i class="bi bi-headset me-2"></i> CSKH: 038 8305167</p>
        </div>
        <div class="col-md-4">
          <h5>Kết nối & Địa chỉ</h5>
          <div class="social-icons mb-4">
            <a href="#"><i class="bi bi-facebook"></i></a>
            <a href="#"><i class="bi bi-instagram"></i></a>
            <a href="#"><i class="bi bi-twitter"></i></a>
          </div>
          <p class="mb-2"><i class="bi bi-geo-alt me-2"></i> CN1: 140 Lê Trọng Tấn, TP. HCM</p>
          <p><i class="bi bi-geo-alt me-2"></i> CN2: 86 Tân Hòa Đông, Q.6, TP. HCM</p>
        </div>
      </div>
      <hr class="mt-5 mb-4 footer-divider" />
      <div class="row">
        <div class="col-12 text-center text-white-50 small">
          &copy; 2025 MAY HOTEL KINGS. All rights reserved.
        </div>
      </div>
    </div>
  </footer>
`;

const TOAST_HTML = `
  <div class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 9999;">
    <div id="toastNotification" class="toast align-items-center shadow-lg text-white border-0 bg-navy" role="alert" aria-live="assertive" aria-atomic="true">
      <div class="d-flex">
        <div class="toast-body fs-6 fw-medium px-4 py-3 d-flex align-items-center">
          <i id="toastIcon" class="bi bi-check-circle-fill me-3 fs-4 text-warning"></i>
          <div id="toastMessage">Thông báo từ hệ thống!</div>
        </div>
        <button type="button" class="btn-close btn-close-white me-3 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
      </div>
    </div>
  </div>
`;

const CHATBOT_HTML = `
  <div id="chat-circle">
    <i class="bi bi-chat-dots-fill fs-3"></i>
  </div>
  
  <div class="chat-box" id="chat-box">
    <div class="chat-box-header">
      <div>
        <i class="bi bi-robot me-2 fs-5"></i>
        <span class="fw-bold">Hỗ trợ trực tuyến</span>
      </div>
      <span id="chat-box-close" class="fs-5" style="cursor:pointer; transition: 0.3s;"><i class="bi bi-x-lg"></i></span>
    </div>
    
    <div class="chat-box-body" id="chat-logs">
      <div class="msg-bot shadow-sm">
        Xin chào! Tôi là trợ lý ảo AI của May Hotel. Tôi có thể giúp gì cho bạn? <br />
        <small class="text-muted mt-1 d-block">(Ví dụ: "Giá phòng bao nhiêu", "Có hồ bơi không")</small>
      </div>
    </div>
    
    <div class="chat-input-area">
      <input type="text" id="chat-input" class="form-control" placeholder="Nhập tin nhắn..." autocomplete="off" />
      <button id="chat-submit" class="btn-send"><i class="bi bi-send-fill"></i></button>
    </div>
  </div>
`;

function injectLayout() {
    // Thêm Google Fonts, Bootstrap CSS, Bootstrap Icons, style.css vào head nếu chưa có
    const head = document.head;
    
    // Tìm thẻ link CSS đầu tiên của trang để chèn Bootstrap vào TRƯỚC nó
    // Như vậy custom CSS của trang sẽ không bị Bootstrap đè lên (Fix lỗi Focus outline màu xanh)
    const firstLink = document.querySelector('link[rel="stylesheet"]');
    
    const insertCSS = (html) => {
        if (firstLink) {
            firstLink.insertAdjacentHTML('beforebegin', html);
        } else {
            head.insertAdjacentHTML('beforeend', html);
        }
    };

    // ========================================================
    // CSS đã được chuyển trực tiếp vào thẻ <head> của HTML 
    // để tránh hiện tượng FOUC (chớp/nháy giao diện).
    // ========================================================

    // CSS ACTIVE MENU đã được chuyển vào HTML để tăng tốc render

    // Thêm Bootstrap JS Bundle nếu chưa có (cần thiết cho Dropdown)
    if (!document.querySelector('script[src*="bootstrap.bundle"]')) {
        const bsScript = document.createElement('script');
        bsScript.src = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js";
        document.body.appendChild(bsScript);
    }

    // Tiêm Navbar vào đầu trang
    document.body.insertAdjacentHTML('afterbegin', NAVBAR_HTML);

    // Bọc nội dung hiện tại của trang vào thẻ <main class="main-content"> nếu chưa bọc
    // (Bỏ qua các trang đã có sẵn main-content)
    let hasMain = false;
    let mainContent = document.querySelector('.main-content');
    if (!mainContent) {
        // Lấy tất cả nội dung hiện tại giữa navbar và script
        const children = Array.from(document.body.children);
        mainContent = document.createElement('div');
        mainContent.className = 'main-content fade-in-box';
        
        children.forEach(child => {
            if (child.tagName !== 'NAV' && child.tagName !== 'SCRIPT' && child.tagName !== 'LINK') {
                mainContent.appendChild(child);
            }
        });
        
        const nav = document.querySelector('.navbar');
        if (nav) {
            nav.insertAdjacentElement('afterend', mainContent);
        } else {
            document.body.insertAdjacentElement('afterbegin', mainContent);
        }
    }

    // Tiêm Footer, Toast, Chatbot vào cuối trang
    document.body.insertAdjacentHTML('beforeend', FOOTER_HTML);
    document.body.insertAdjacentHTML('beforeend', TOAST_HTML);
    document.body.insertAdjacentHTML('beforeend', CHATBOT_HTML);

    // Cung cấp biến basePath cho app.js sử dụng
    window.APP_BASE_PATH = basePath;

    // ========================================================
    // 2. LÔ-GÍC JAVASCRIPT TỰ ĐỘNG NHẬN DIỆN TRANG NẰM Ở ĐÂY
    // ========================================================
    // Lấy đường dẫn hiện tại của trình duyệt
    const currentUrl = window.location.href.split('?')[0].split('#')[0];
    
    // Tìm tất cả các thẻ <a> trong thanh menu
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link');
    
    navLinks.forEach(link => {
        // Lấy đường dẫn đích của từng thẻ <a>
        const linkUrl = link.href.split('?')[0].split('#')[0];
        
        // Nếu đường dẫn hiện tại khớp với đường dẫn của menu
        if (currentUrl === linkUrl) {
            link.classList.add('active-menu'); // Bật hiệu ứng
        }
    });
}

// Thực thi ngay lập tức khi load xong layout.js
injectLayout();