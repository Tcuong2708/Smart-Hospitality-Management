document.addEventListener('DOMContentLoaded', () => {
    // API endpoint của Java Backend
    const API_URL = 'http://localhost:8080/api/hotel-info';

    // Hàm gắn (bind) dữ liệu nhận được lên giao diện HTML
    const renderData = (data) => {
        // 1. Render Header
        document.getElementById('header-image').src = data.header.bgImage;
        document.getElementById('header-title').textContent = data.header.title;
        document.getElementById('header-subtitle').textContent = data.header.subtitle;

        // 2. Render Câu Chuyện Thương Hiệu
        const storyContent = document.getElementById('story-content');
        storyContent.innerHTML = `
            <p>
                <span class="drop-cap">${data.about.dropCap}</span>
                <b>${data.header.title}</b> ${data.about.content}
            </p>
            <p>Triết lý của chúng tôi: <i class="text-navy fw-bold">"${data.about.philosophy}"</i>.</p>
        `;
        document.getElementById('story-image').src = data.about.image;

        // 3. Render Timeline
        document.getElementById('timeline-image').src = data.timeline.image;
        const timelineContainer = document.getElementById('timeline-container');
        timelineContainer.innerHTML = ''; // Xóa sạch trước khi đổ dữ liệu mới
        data.timeline.items.forEach(item => {
            timelineContainer.innerHTML += `
                <div class="timeline-item">
                    <div class="timeline-title">${item.title}</div>
                    <p class="text-muted">${item.description}</p>
                </div>
            `;
        });

        // 4. Render Gallery
        const galleryContainer = document.getElementById('gallery-container');
        galleryContainer.innerHTML = '';
        data.gallery.forEach(imgSrc => {
            galleryContainer.innerHTML += `
                <div class="col-md-4 mb-4">
                    <img src="${imgSrc}" class="img-fluid gallery-img w-100 h-100 object-fit-cover" style="min-height: 250px;" alt="Không gian sang trọng" />
                </div>
            `;
        });

        // 5. Render Quote cuối trang
        document.getElementById('footer-quote').textContent = `"${data.footerQuote}"`;
    };

    // Hàm gọi API
    const fetchHotelInfo = async () => {
        try {
            /* 
             * Mở comment dòng fetch dưới đây để lấy API thật từ Java Backend.
             * API Java nên có cấu trúc JSON tương tự như biến mockData bên dưới.
             */
             
            // const response = await fetch(API_URL);
            // if (!response.ok) throw new Error('Network response was not ok');
            // const data = await response.json();
            
            // --- DỮ LIỆU MẪU (Mock Data) --- 
            // Tạm thời dùng dữ liệu này để hiển thị UI nếu Backend chưa viết xong API
            const mockData = {
                header: {
                    title: "MAY HOTEL",
                    subtitle: "Nơi Khởi Nguồn Của Mọi Hành Trình Hoàn Hảo",
                    bgImage: "../../images/1.jpg"
                },
                about: {
                    dropCap: "L",
                    content: "được xây dựng với khát vọng mang đến một trải nghiệm nghỉ dưỡng vượt trội. Nơi đây không chỉ là một điểm dừng chân, mà còn là một hành trình kết nối cảm xúc, nơi từng chi tiết thiết kế đều được chăm chút tỉ mỉ...",
                    philosophy: "Đánh thức mọi giác quan, nâng tầm trải nghiệm",
                    image: "../../images/4.jpg"
                },
                timeline: {
                    image: "../../images/5.jpg",
                    items: [
                        { title: "1. Khởi Dựng Tầm Nhìn Đẳng Cấp", description: "Ý tưởng về MAY HOTEL ra đời từ mong muốn tạo ra một không gian lưu trú sang trọng..." },
                        { title: "2. Chạm Đến Sự Hoàn Mỹ", description: "Mỗi không gian, dịch vụ đều được thiết kế để vượt qua mọi tiêu chuẩn khắt khe nhất." }
                    ]
                },
                gallery: [
                    "../../images/a1.jpg",
                    "../../images/a2.jpg",
                    "../../images/a3.jpg"
                ],
                footerQuote: "Chạm vào giấc mơ ngọt dịu giữa không gian thanh lịch."
            };

            // Gọi render (thay mockData bằng data từ API)
            renderData(mockData); 
            
        } catch (error) {
            console.error("Lỗi khi gọi API tới localhost:8080 - ", error);
            document.getElementById('header-title').textContent = "Lỗi kết nối máy chủ";
            document.getElementById('header-subtitle').textContent = "Vui lòng kiểm tra lại Backend Java (localhost:8080)";
        }
    };

    // Khởi chạy
    fetchHotelInfo();
});
