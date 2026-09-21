// Tự động nhận diện basePath của frontend
const scriptTags = document.getElementsByTagName('script');
let currentScriptSrc = '';
for (let tag of scriptTags) {
    const srcAttr = tag.getAttribute('src');
    if (srcAttr && srcAttr.includes('layout-staff.js')) {
        currentScriptSrc = srcAttr;
        break;
    }
}
const basePath = currentScriptSrc.substring(0, currentScriptSrc.indexOf('role/layout-staff.js'));

const STAFF_NAVBAR = `
  <nav class="navbar navbar-expand-lg sticky-top navbar-dark shadow-sm" style="background-color: #0F2942;">
    <div class="container-fluid px-4">
      <a class="navbar-brand fw-bold" href="${basePath}staff/room-map/index.html" style="color: #C5A017;">
        <i class="bi bi-person-badge me-2"></i>MAY STAFF
      </a>
      
      <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#staffNav">
        <span class="navbar-toggler-icon"></span>
      </button>
      
      <div class="collapse navbar-collapse" id="staffNav">
        <ul class="navbar-nav ms-auto align-items-center">
          <li class="nav-item"><a class="nav-link text-white" href="${basePath}staff/room-map/index.html"><i class="bi bi-map me-1"></i>Sơ đồ phòng</a></li>
          <li class="nav-item"><a class="nav-link text-white" href="${basePath}admin/check-in/index.html"><i class="bi bi-box-arrow-in-right me-1"></i>Check-in</a></li>
          <li class="nav-item"><a class="nav-link text-white" href="${basePath}admin/check-out/index.html"><i class="bi bi-box-arrow-right me-1"></i>Check-out</a></li>
          
          <li class="nav-item dropdown ms-3">
            <a class="nav-link dropdown-toggle d-flex align-items-center" href="#" data-bs-toggle="dropdown">
              <i class="bi bi-person-circle fs-4" style="color: #C5A017;"></i>
            </a>
            <ul class="dropdown-menu dropdown-menu-end shadow border-0">
              <li class="dropdown-header border-bottom pb-2">
                <span class="fw-bold d-block">Receptionist</span>
                <small class="text-muted">Nhân viên Lễ tân</small>
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

const STAFF_FOOTER = `
  <footer class="py-3 mt-auto text-center border-top" style="background-color: #f8f9fa;">
    <div class="container-fluid">
      <span class="text-muted small">&copy; 2026 May Hotel Internal System. All rights reserved.</span>
    </div>
  </footer>
`;

function injectStaffLayout() {
    const head = document.head;
    
    if (!document.querySelector('link[href*="bootstrap.min.css"]')) {
        head.insertAdjacentHTML('beforeend', '<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">');
    }
    if (!document.querySelector('link[href*="bootstrap-icons.css"]')) {
        head.insertAdjacentHTML('beforeend', '<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">');
    }
    
    document.body.insertAdjacentHTML('afterbegin', STAFF_NAVBAR);
    
    let mainContent = document.querySelector('.main-content') || document.querySelector('.container');
    if (mainContent) {
        document.body.style.display = 'flex';
        document.body.style.flexDirection = 'column';
        document.body.style.minHeight = '100vh';
        mainContent.style.flex = '1';
    }

    document.body.insertAdjacentHTML('beforeend', STAFF_FOOTER);
}

injectStaffLayout();
