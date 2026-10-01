// Script cho Trang chủ (Home)
document.addEventListener('DOMContentLoaded', () => {
    fetchHomeCategories();
});

async function fetchHomeCategories(filteredCapacity = null) {
    const container = document.getElementById('room-categories-container');
    
    // Dữ liệu giả định (Mock Data) sử dụng cứng luôn không gọi API
    let mockCategories = [
        { maLoai: 'L01', name: 'Phòng Đơn (Single Room)', soNguoi: 1, imageUrl: 'images/d1.jpg', price: 500000, hasRoom: true, isHot: false },
        { maLoai: 'L02', name: 'Phòng Đôi (Double Room)', soNguoi: 2, imageUrl: 'images/d3.jpg', price: 800000, hasRoom: true, isHot: true },
        { maLoai: 'L03', name: 'Phòng Gia Đình (Family Room)', soNguoi: 4, imageUrl: 'images/gd1.jpg', price: 1500000, hasRoom: true, isHot: false },
        { maLoai: 'L04', name: 'Phòng Suite (Thượng Gia)', soNguoi: 4, imageUrl: 'images/su1.jpg', price: 3500000, hasRoom: false, isHot: true }
    ];

    if (filteredCapacity) {
        mockCategories = mockCategories.filter(c => c.soNguoi >= filteredCapacity && c.hasRoom);
        if (mockCategories.length === 0) {
            container.innerHTML = `
              <div class="col-12 text-center py-5">
                <div class="text-danger opacity-75">
                  <i class="bi bi-calendar-x fs-1"></i>
                  <h4 class="mt-3 fw-bold">Rất tiếc, khách sạn đã hết phòng phù hợp!</h4>
                  <p>Vui lòng chọn ngày khác hoặc thử giảm số lượng người.</p>
                </div>
              </div>
            `;
            return;
        }
    }
    
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
                    <button class="btn btn-add-cart-home w-100 fw-bold btn-book-online text-white" 
                            style="background-color: #0F2942;"
                            data-id="${maLoai}" 
                            data-name="${name}" 
                            data-capacity="${capacity}" 
                            data-price="${price}" 
                            data-img="${imageUrl}">
                      <i class="bi bi-calendar-check me-2"></i>Đặt ngay
                    </button>
                  ` : `
                    <button class="btn btn-secondary w-100 shadow-sm fw-bold" disabled>
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
    bindBookingEvents();
}

// Xử lý Tìm phòng trống (Tra cứu phòng trống)
document.getElementById('searchRoomForm')?.addEventListener('submit', (e) => {
    e.preventDefault();
    const adults = parseInt(document.getElementById('adults').value) || 2;
    const children = parseInt(document.getElementById('children').value) || 0;
    const checkin = document.getElementById('checkinDate').value;
    const checkout = document.getElementById('checkoutDate').value;
    const totalCapacity = adults + Math.floor(children / 2); // giả sử 2 trẻ em = 1 người lớn
    
    // Chuyển hướng sang trang Danh sách phòng kèm tham số
    const url = `home/rooms/list.html?capacity=${totalCapacity}&checkin=${checkin}&checkout=${checkout}`;
    window.location.href = url;
});

// Xử lý Đặt phòng trực tuyến (Modal)
function bindBookingEvents() {
    let onlineBookingModal;
    let qrPaymentModal;

    document.querySelectorAll('.btn-book-online').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const target = e.currentTarget;
            document.getElementById('bookingRoomName').textContent = target.dataset.name;
            document.getElementById('bookingCapacity').textContent = target.dataset.capacity;
            document.getElementById('bookingPrice').textContent = new Intl.NumberFormat('vi-VN').format(target.dataset.price) + ' đ';
            document.getElementById('bookingRoomImg').src = target.dataset.img;

            if (!onlineBookingModal) {
                onlineBookingModal = new bootstrap.Modal(document.getElementById('onlineBookingModal'));
                // append modal to body if not already
                document.body.appendChild(document.getElementById('onlineBookingModal'));
                document.body.appendChild(document.getElementById('qrPaymentModal'));
            }
            onlineBookingModal.show();
        });
    });

    document.getElementById('btnConfirmBooking')?.addEventListener('click', () => {
        const form = document.getElementById('onlineBookingForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const method = document.getElementById('paymentMethod').value;
        onlineBookingModal.hide();

        if (method === 'qr') {
            if (!qrPaymentModal) {
                qrPaymentModal = new bootstrap.Modal(document.getElementById('qrPaymentModal'));
            }
            qrPaymentModal.show();
            
            // Giả lập sau 3 giây thanh toán thành công
            setTimeout(() => {
                qrPaymentModal.hide();
                alert('Thanh toán đặt cọc thành công! Email xác nhận đã được gửi đến bạn.');
            }, 3000);
        } else {
            alert('Đặt phòng thành công (Giữ chỗ)! Vui lòng thanh toán khi nhận phòng.');
        }
    });
}
