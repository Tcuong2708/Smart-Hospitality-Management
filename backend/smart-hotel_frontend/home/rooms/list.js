document.addEventListener('DOMContentLoaded', () => {
    const API_ROOMS = 'http://localhost:8080/api/rooms';
    const API_CATEGORIES = 'http://localhost:8080/api/categories';

    const roomListEl = document.getElementById('room-list');
    const noResultsEl = document.getElementById('no-results');
    const categoryFilterEl = document.getElementById('category-filter');
    const priceFilterEl = document.getElementById('price-filter');
    const searchForm = document.getElementById('search-form');
    const searchInput = document.getElementById('searchInput');
    const btnClearFilter = document.getElementById('btn-clear-filter');

    let currentFilters = { maLoai: '', priceRange: '', searchString: '' };
    let categoryMap = {}; // Lưu map maLoai -> Tên Loại để render

    const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN').format(amount);

    const renderCategories = (categories) => {
        let html = `<a href="#" data-id="" class="list-group-item list-group-item-action active">Tất cả</a>`;
        categories.forEach(cat => {
            categoryMap[cat.maLoai] = cat.name; // Lưu vào map
            html += `<a href="#" data-id="${cat.maLoai}" class="list-group-item list-group-item-action">${cat.name}</a>`;
        });
        categoryFilterEl.innerHTML = html;

        categoryFilterEl.querySelectorAll('a').forEach(a => {
            a.addEventListener('click', (e) => {
                e.preventDefault();
                categoryFilterEl.querySelectorAll('a').forEach(el => el.classList.remove('active'));
                a.classList.add('active');
                currentFilters.maLoai = a.getAttribute('data-id');
                fetchRooms();
            });
        });
    };

    priceFilterEl.querySelectorAll('a').forEach(a => {
        a.addEventListener('click', (e) => {
            e.preventDefault();
            priceFilterEl.querySelectorAll('a').forEach(el => el.classList.remove('active'));
            a.classList.add('active');
            currentFilters.priceRange = a.getAttribute('data-price');
            fetchRooms();
        });
    });

    searchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        currentFilters.searchString = searchInput.value.trim();
        fetchRooms();
    });

    btnClearFilter.addEventListener('click', () => {
        currentFilters = { maLoai: '', priceRange: '', searchString: '' };
        searchInput.value = '';
        categoryFilterEl.querySelectorAll('a').forEach(el => el.classList.remove('active'));
        categoryFilterEl.querySelector('a[data-id=""]').classList.add('active');
        priceFilterEl.querySelectorAll('a').forEach(el => el.classList.remove('active'));
        priceFilterEl.querySelector('a[data-price=""]').classList.add('active');
        fetchRooms();
    });

    const mockCategories = [
        { maLoai: 'L01', name: 'Phòng Đơn (Single)' },
        { maLoai: 'L02', name: 'Phòng Đôi (Double)' },
        { maLoai: 'L03', name: 'Phòng Gia Đình (Family)' },
        { maLoai: 'L04', name: 'Phòng Suite (Thượng Gia)' }
    ];

    const mockRooms = [
        { id: 1, maLoai: 'L01', name: 'Phòng Đơn (Single Room)', price: 500000, imageUrl: 'd1.jpg' },
        { id: 2, maLoai: 'L01', name: 'Phòng Đơn (Single Room)', price: 500000, imageUrl: 'd3.jpg' },
        { id: 3, maLoai: 'L02', name: 'Phòng Đôi (Double Room)', price: 800000, imageUrl: 'd4.jpg' },
        { id: 4, maLoai: 'L02', name: 'Phòng Đôi (Double Room)', price: 800000, imageUrl: 'd5.jpg' },
        { id: 5, maLoai: 'L03', name: 'Phòng Gia Đình (Family Room)', price: 1500000, imageUrl: 'gd1.jpg' },
        { id: 6, maLoai: 'L04', name: 'Phòng Suite (Thượng Gia)', price: 3500000, imageUrl: 'su1.jpg' }
    ];

    const fetchCategories = () => {
        renderCategories(mockCategories);
        fetchRooms();
    };

    const fetchRooms = () => {
        let filteredRooms = mockRooms;

        // Filter by category
        if (currentFilters.maLoai) {
            filteredRooms = filteredRooms.filter(r => r.maLoai === currentFilters.maLoai);
        }

        // Filter by price
        if (currentFilters.priceRange === 'lt500') {
            filteredRooms = filteredRooms.filter(r => r.price < 500000);
        } else if (currentFilters.priceRange === 'gt2000') {
            filteredRooms = filteredRooms.filter(r => r.price > 2000000);
        }

        // Filter by search string
        if (currentFilters.searchString) {
            const searchLower = currentFilters.searchString.toLowerCase();
            filteredRooms = filteredRooms.filter(r => r.name.toLowerCase().includes(searchLower));
        }

        renderRooms(filteredRooms);
    };

    const renderRooms = (rooms) => {
        roomListEl.innerHTML = '';
        if (!rooms || rooms.length === 0) {
            roomListEl.classList.add('d-none');
            noResultsEl.classList.remove('d-none');
            return;
        }

        roomListEl.classList.remove('d-none');
        noResultsEl.classList.add('d-none');

        rooms.forEach(p => {
            let imgSrc = p.imageUrl ? `../../images/${p.imageUrl}` : 'https://via.placeholder.com/300x220?text=No+Image';
            if (p.imageUrl && p.imageUrl.startsWith("http")) {
                imgSrc = p.imageUrl;
            }

            const loaiName = categoryMap[p.maLoai] || 'Phòng tiêu chuẩn';
            const detailUrl = `detail.html?id=${p.id}`;

            const html = `
                <div class="col-lg-4 col-md-6">
                    <div class="card h-100 room-card shadow-sm border" style="border-radius: 8px;">
                        <div class="room-img-wrapper position-relative">
                            <span class="badge bg-danger position-absolute top-0 start-0 m-3 px-2 py-1"><i class="bi bi-fire me-1"></i> HOT DEAL</span>
                            <a href="${detailUrl}">
                                <img src="${imgSrc}" class="card-img-top room-img" alt="${p.name}">
                            </a>
                        </div>
                        <div class="card-body d-flex flex-column text-center p-4">
                            <small class="text-muted text-uppercase fw-bold mb-3" style="font-size: 0.75rem;">
                                <i class="bi bi-people-fill me-1 text-secondary"></i> Sức chứa: ${p.maLoai === 'L01' ? '1' : (p.maLoai === 'L02' ? '2' : '4')} người
                            </small>
                            <h5 class="card-title fw-bold mb-3">
                                <a href="${detailUrl}" class="text-decoration-none" style="color: var(--navy-color);">${p.name}</a>
                            </h5>
                            <p class="mb-4">
                                <span class="fw-bold fs-5" style="color: #dc3545;">${formatCurrency(p.price)}</span>
                                <small class="text-muted fw-normal ms-1">đ/ đêm</small>
                            </p>
                            <div class="mt-auto row g-2 align-items-stretch">
                                <div class="col-6">
                                    <a href="${detailUrl}" class="btn btn-outline-dark fw-bold w-100 h-100 d-flex align-items-center justify-content-center text-uppercase" style="border-radius: 4px; font-size: 0.85rem; min-height: 42px;">
                                        Chi tiết
                                    </a>
                                </div>
                                <div class="col-6">
                                    <a href="../booking/checkout.html?maLoai=${p.maLoai}" class="btn btn-gold text-white fw-bold w-100 h-100 d-flex align-items-center justify-content-center text-uppercase" style="background-color: #C5A017; border: none; border-radius: 4px; font-size: 0.85rem; min-height: 42px;">
                                        <i class="bi bi-calendar-check-fill me-1"></i> Đặt ngay
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            roomListEl.innerHTML += html;
        });
    };

    // Khởi động
    fetchCategories();
});
