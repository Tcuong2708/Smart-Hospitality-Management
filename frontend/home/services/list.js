document.addEventListener('DOMContentLoaded', () => {
    const services = [
        { id: 1, name: 'Trị liệu Spa Hoàng Gia', category: 'spa', price: 1500000, img: 'https://images.unsplash.com/photo-1540555700478-4be289fbecef?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', desc: 'Gói massage toàn thân thư giãn với tinh dầu cao cấp trong 90 phút.' },
        { id: 2, name: 'Bữa tối Lãng mạn nến & hoa', category: 'dining', price: 2500000, img: 'https://images.unsplash.com/photo-1559339352-11d035aa65de?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', desc: 'Set menu 5 món Âu cao cấp tại nhà hàng tầng thượng với view toàn thành phố.' },
        { id: 3, name: 'Xe đưa đón Sân bay 4 chỗ', category: 'transport', price: 350000, img: 'https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', desc: 'Dịch vụ xe riêng đón hoặc tiễn sân bay Tân Sơn Nhất, an toàn và tiện lợi.' },
        { id: 4, name: 'Tắm hơi đá muối Himalaya', category: 'spa', price: 500000, img: 'https://images.unsplash.com/photo-1515377905703-c4788e51af15?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', desc: 'Thanh lọc cơ thể, giảm căng thẳng với phòng xông hơi đá muối đặc biệt.' },
        { id: 5, name: 'Buffet sáng Tiêu chuẩn', category: 'dining', price: 400000, img: 'https://images.unsplash.com/photo-1550989460-0adf9ea622e2?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', desc: 'Thưởng thức hơn 50 món ăn Á-Âu tại nhà hàng chính của khách sạn.' },
        { id: 6, name: 'Cho thuê xe máy theo ngày', category: 'transport', price: 150000, img: 'https://images.unsplash.com/photo-1558981403-c5f9899a28bc?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', desc: 'Khám phá thành phố dễ dàng với dịch vụ thuê xe tay ga đời mới.' }
    ];

    const formatMoney = (val) => new Intl.NumberFormat('vi-VN').format(val) + ' đ';
    const container = document.getElementById('service-list');
    
    function renderServices(filter = 'all') {
        container.innerHTML = '';
        const filtered = filter === 'all' ? services : services.filter(s => s.category === filter);
        
        filtered.forEach(s => {
            container.innerHTML += `
                <div class="col-md-6 col-lg-4">
                    <div class="card service-card position-relative">
                        <span class="price-tag">${formatMoney(s.price)}</span>
                        <img src="${s.img}" class="card-img-top service-img" alt="${s.name}">
                        <div class="card-body d-flex flex-column">
                            <h5 class="fw-bold text-navy mb-2" style="color: #0F2942;">${s.name}</h5>
                            <p class="text-muted small flex-grow-1">${s.desc}</p>
                            <button class="btn btn-gold w-100 fw-bold mt-2 btn-book-trigger" 
                                data-id="${s.id}" data-name="${s.name}" data-price="${s.price}">
                                Đặt dịch vụ này
                            </button>
                        </div>
                    </div>
                </div>
            `;
        });

        // Add event listeners for booking buttons
        document.querySelectorAll('.btn-book-trigger').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const target = e.target;
                openBookingModal(target.dataset.name, parseInt(target.dataset.price));
            });
        });
    }

    renderServices();

    // Filters logic
    const filterBtns = document.querySelectorAll('#category-filters button');
    filterBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            filterBtns.forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');
            renderServices(e.target.dataset.category);
        });
    });

    // Modal Logic
    let currentPrice = 0;
    const qtyInput = document.getElementById('service-quantity');
    const totalEl = document.getElementById('modal-total-price');
    let modalInstance = null;

    function openBookingModal(name, price) {
        document.getElementById('modal-service-name').textContent = name;
        currentPrice = price;
        qtyInput.value = 1;
        totalEl.textContent = formatMoney(price);
        
        // Default to today
        document.getElementById('service-date').valueAsDate = new Date();

        if(!modalInstance) {
            modalInstance = new bootstrap.Modal(document.getElementById('bookingServiceModal'));
        }
        modalInstance.show();
    }

    qtyInput.addEventListener('input', (e) => {
        let val = parseInt(e.target.value) || 1;
        if(val < 1) val = 1;
        e.target.value = val;
        totalEl.textContent = formatMoney(val * currentPrice);
    });

    document.getElementById('btn-confirm-book').addEventListener('click', () => {
        modalInstance.hide();
        // Giả sử có hàm showAlert trong layout hoặc tự tạo một thông báo nhỏ
        alert(`Bạn đã đặt thành công dịch vụ: ${document.getElementById('modal-service-name').textContent}!`);
    });
});
