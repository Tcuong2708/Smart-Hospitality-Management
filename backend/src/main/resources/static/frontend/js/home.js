// Script cho Trang chủ (Home)
document.addEventListener('DOMContentLoaded', () => {
    fetchHomeCategories();
});

async function fetchHomeCategories() {
    const container = document.getElementById('room-categories-container');
    
    // Dữ liệu giả định (Mock Data) sử dụng cứng luôn không gọi API
    const mockCategories = [
        { maLoai: 'L01', name: 'Phòng Đơn (Single Room)', soNguoi: 1, imageUrl: 'images/d1.jpg', price: 500000, hasRoom: true, isHot: false },
        { maLoai: 'L02', name: 'Phòng Đôi (Double Room)', soNguoi: 2, imageUrl: 'images/d3.jpg', price: 800000, hasRoom: true, isHot: true },
        { maLoai: 'L03', name: 'Phòng Gia Đình (Family Room)', soNguoi: 4, imageUrl: 'images/gd1.jpg', price: 1500000, hasRoom: true, isHot: false },
        { maLoai: 'L04', name: 'Phòng Suite (Thượng Gia)', soNguoi: 4, imageUrl: 'images/su1.jpg', price: 3500000, hasRoom: false, isHot: true }
    ];
    
    renderRoomCategories(mockCategories, container);
}

function renderRoomCategories(categories, container) {
    if (!categories || categories.length === 0) {
        container.innerHTML = `
          <div class="col-12 text-center py-5">
            <div class="text-muted opacity-50">
              <i class="bi bi-journal-x fs-1"></i>
              <p class="mt-2">Hiện tại chưa có hạng phòng nào.</p>
            </div>
          </div>
        `;
        return;
    }

    let html = '';
    
    categories.forEach(item => {
        // Điều chỉnh key để phù hợp với cả mock data và API data thực tế
        // API có thể trả về tenLoai thay vì name, v.v.
        const name = item.name || item.tenLoai || 'Loại phòng chưa tên';
        const price = item.price || item.giaDaiDien || 0;
        const capacity = item.soNguoi || 2;
        const imageUrl = item.imageUrl || 'https://via.placeholder.com/400x300?text=May+Hotel';
        const isHot = item.isHot || (price > 0 && price < 1500000);
        // Kiểm tra xem phòng có trống để đặt không
        const hasRoom = item.hasRoom !== undefined ? item.hasRoom : true;
        const maLoai = item.maLoai || item.id || 1;

        const formatCurrency = (val) => new Intl.NumberFormat('vi-VN').format(val);

        html += `
          <div class="col-lg-3 col-md-4 col-6 mb-4">
            <div class="card card-product h-100">
              
              <div class="product-img-wrapper">
                <a href="home/rooms/detail.html?id=${1}">
                  <img src="${imageUrl}" class="product-img" alt="${name}" />
                </a>
                ${isHot ? `<span class="badge-hot position-absolute top-0 start-0 m-2 px-2 py-1 rounded text-white fw-bold" style="background: #dc3545; font-size: 0.7rem;"><i class="bi bi-fire me-1"></i>Hot Deal</span>` : ''}
              </div>

              <div class="card-body text-center d-flex flex-column p-3">
                <small class="text-muted mb-1 text-uppercase fw-bold" style="font-size: 0.7rem; letter-spacing: 0.5px;">
                  <i class="bi bi-people-fill me-1"></i>Sức chứa: <span>${capacity}</span> người
                </small>

                <a href="home/rooms/detail.html?id=${maLoai}" class="text-decoration-none text-navy fw-bold mt-2 mb-2" style="font-size: 1.1rem; line-height: 1.3;">
                  ${name}
                </a>

                <div class="price-tag mb-3 text-danger fw-bold mt-auto">
                  <span>${formatCurrency(price)}</span>
                  <small class="text-muted fw-normal" style="font-size: 0.8rem">đ/ đêm</small>
                </div>

                <div class="card-footer bg-white border-0 p-0 pt-3">
                  ${hasRoom ? `
                    <a href="home/booking/checkout.html?maLoai=${maLoai}" class="btn btn-add-cart-home text-center d-block">
                      <i class="bi bi-calendar-check me-2"></i>Đặt ngay
                    </a>
                  ` : `
                    <button class="btn btn-secondary w-100 shadow-sm" disabled>
                      <i class="bi bi-x-circle me-2"></i>Hết phòng
                    </button>
                  `}
                </div>
              </div>

            </div>
          </div>
        `;
    });

    container.innerHTML = html;
}
