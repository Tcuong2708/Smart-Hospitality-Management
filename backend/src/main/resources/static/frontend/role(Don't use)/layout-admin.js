// Tự động nhận diện basePath của frontend
const scriptTags = document.getElementsByTagName('script');
let currentScriptSrc = '';
for (let tag of scriptTags) {
    const srcAttr = tag.getAttribute('src');
    if (srcAttr && srcAttr.includes('layout-admin.js')) {
        currentScriptSrc = srcAttr;
        break;
    }
}
const basePath = currentScriptSrc.substring(0, currentScriptSrc.indexOf('role/layout-admin.js'));

const ADMIN_NAVBAR = `
  <nav class="navbar navbar-expand-lg sticky-top bg-dark navbar-dark shadow-sm">
    <div class="container-fluid px-4">
      <a class="navbar-brand text-warning fw-bold" href="${basePath}admin/statistical/index.html">
        <i class="bi bi-shield-lock-fill me-2"></i>MAY ADMIN
      </a>
      
      <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#adminNav">
        <span class="navbar-toggler-icon"></span>
      </button>
      
      <div class="collapse navbar-collapse" id="adminNav">
        <ul class="navbar-nav ms-auto align-items-center">
          <li class="nav-item"><a class="nav-link" href="${basePath}admin/statistical/index.html"><i class="bi bi-graph-up me-1"></i>Thống kê</a></li>
          <li class="nav-item"><a class="nav-link" href="${basePath}admin/rooms/index.html"><i class="bi bi-door-open me-1"></i>Phòng</a></li>
          <li class="nav-item"><a class="nav-link" href="${basePath}admin/category/index.html"><i class="bi bi-tags me-1"></i>Loại phòng</a></li>
          <li class="nav-item"><a class="nav-link" href="${basePath}admin/service/index.html"><i class="bi bi-cup-hot me-1"></i>Dịch vụ</a></li>
          <li class="nav-item"><a class="nav-link" href="${basePath}admin/invoice/index.html"><i class="bi bi-receipt me-1"></i>Hóa đơn</a></li>
          
          <li class="nav-item dropdown ms-3">
            <a class="nav-link dropdown-toggle d-flex align-items-center" href="#" data-bs-toggle="dropdown">
              <i class="bi bi-person-circle fs-4 text-warning"></i>
            </a>
            <ul class="dropdown-menu dropdown-menu-end shadow border-0">
              <li class="dropdown-header border-bottom pb-2">
                <span class="fw-bold d-block">Administrator</span>
                <small class="text-muted">Quản trị viên</small>
              </li>
              <li><a class="dropdown-item mt-2" href="${basePath}index.html"><i class="bi bi-house me-2"></i>Xem trang khách</a></li>
              <li><a class="dropdown-item text-danger" href="${basePath}index.html"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
            </ul>
          </li>
        </ul>
      </div>
    </div>
  </nav>
`;

const ADMIN_FOOTER = `
  <footer class="bg-light py-3 mt-auto text-center border-top">
    <div class="container-fluid">
      <span class="text-muted small">&copy; 2026 May Hotel Internal System. All rights reserved.</span>
    </div>
  </footer>
`;

function injectAdminLayout() {
    const head = document.head;
    
    // Thêm Bootstrap và Icons nếu chưa có
    if (!document.querySelector('link[href*="bootstrap.min.css"]')) {
        head.insertAdjacentHTML('beforeend', '<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">');
    }
    if (!document.querySelector('link[href*="bootstrap-icons.css"]')) {
        head.insertAdjacentHTML('beforeend', '<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">');
    }
    
    // Tiêm Navbar lên đầu
    document.body.insertAdjacentHTML('afterbegin', ADMIN_NAVBAR);
    
    // Bọc nội dung hiện tại vào main-content (nếu cần để footer luôn ở dưới)
    let mainContent = document.querySelector('.main-content') || document.querySelector('.container');
    if (mainContent) {
        document.body.style.display = 'flex';
        document.body.style.flexDirection = 'column';
        document.body.style.minHeight = '100vh';
        mainContent.style.flex = '1';
    }

    // Tiêm Footer xuống cuối
    document.body.insertAdjacentHTML('beforeend', ADMIN_FOOTER);
}

// Chạy hàm
injectAdminLayout();
