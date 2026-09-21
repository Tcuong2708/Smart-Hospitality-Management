document.addEventListener('DOMContentLoaded', () => {
    // URL API Backend (Sửa thành link API thật của bạn)
    const API_URL = 'http://localhost:8080/api/reviews';
    
    let reviewList = [];
    let currentIndex = 0;
    let autoPlayInterval;

    // Các phần tử DOM
    const testimonialBox = document.getElementById('testimonialBox');
    const avatarEl = document.getElementById('avatar');
    const nameEl = document.getElementById('name');
    const textEl = document.getElementById('text');
    const starsEl = document.getElementById('stars');
    const btnPrev = document.getElementById('btn-prev');
    const btnNext = document.getElementById('btn-next');
    const reviewForm = document.getElementById('review-form');
    const alertMessage = document.getElementById('alert-message');

    // Hàm hiển thị thông báo alert
    const showAlert = (message, type = 'success') => {
        alertMessage.textContent = message;
        alertMessage.className = `alert alert-${type} border-0 shadow-sm text-center mb-4`;
        alertMessage.classList.remove('d-none');
        setTimeout(() => alertMessage.classList.add('d-none'), 5000); // Ẩn sau 5s
    };

    // Hàm render review hiện tại theo `currentIndex`
    const renderTestimonial = () => {
        if (!reviewList || reviewList.length === 0) {
            testimonialBox.innerHTML = `<p class="text-muted py-4 m-0"><i class="bi bi-chat-dots me-2"></i>Chưa có phản hồi công khai nào từ khách hàng.</p>`;
            return;
        }

        const item = reviewList[currentIndex];
        
        nameEl.innerText = item.name;
        textEl.innerText = item.text;
        
        // Tạo avatar từ URL hoặc dùng ui-avatars fallback
        avatarEl.src = item.avatar || `https://ui-avatars.com/api/?name=${encodeURIComponent(item.name)}&background=0F2942&color=fff`;

        // Render số sao đánh giá
        let starHTML = "";
        for (let i = 0; i < item.stars; i++) {
            starHTML += "<i class='bi bi-star-fill me-1'></i>";
        }
        for (let i = item.stars; i < 5; i++) {
            starHTML += "<i class='bi bi-star text-muted opacity-25 me-1'></i>";
        }
        starsEl.innerHTML = starHTML;
    };

    // Điều hướng Next / Prev
    const nextTestimonial = () => {
        if (reviewList.length <= 1) return;
        currentIndex = (currentIndex + 1) % reviewList.length;
        renderTestimonial();
    };

    const prevTestimonial = () => {
        if (reviewList.length <= 1) return;
        currentIndex = (currentIndex - 1 + reviewList.length) % reviewList.length;
        renderTestimonial();
    };

    btnNext.addEventListener('click', () => {
        nextTestimonial();
        resetAutoPlay();
    });

    btnPrev.addEventListener('click', () => {
        prevTestimonial();
        resetAutoPlay();
    });

    // Tự động chuyển slide mỗi 5s
    const resetAutoPlay = () => {
        clearInterval(autoPlayInterval);
        autoPlayInterval = setInterval(nextTestimonial, 5000);
    };

    // [GET] Fetch danh sách đánh giá từ Backend
    const fetchReviews = async () => {
        try {
            /* 
            // BỎ COMMENT ĐOẠN NÀY ĐỂ GỌI API THẬT
            const response = await fetch(API_URL);
            if (!response.ok) throw new Error('Network error');
            const data = await response.json();
            
            // Map dữ liệu từ Spring (VD: User object) sang định dạng của JS
            reviewList = data.map(r => ({
                name: r.user ? r.user.hoTen : 'Khách hàng',
                stars: r.soSao,
                text: r.noiDung
            }));
            */
            
            // DỮ LIỆU MẪU (Mock Data) khi chưa nối API
            reviewList = [
                {
                    name: "Nguyễn Văn An",
                    stars: 5,
                    text: "Khách sạn tuyệt vời! Phòng ốc sạch sẽ, nhân viên thân thiện. Vị trí rất thuận tiện để đi lại.",
                    avatar: "https://ui-avatars.com/api/?name=Nguyen+An&background=0F2942&color=fff"
                },
                {
                    name: "Trần Thị Bích",
                    stars: 4,
                    text: "Dịch vụ tốt, đồ ăn sáng ngon. Tuy nhiên wifi ở tầng cao hơi yếu một chút. Sẽ quay lại!",
                    avatar: "https://ui-avatars.com/api/?name=Tran+Bich&background=0F2942&color=fff"
                },
                {
                    name: "Le Hoang Nam",
                    stars: 5,
                    text: "Trải nghiệm đẳng cấp 5 sao thực thụ. Hồ bơi và Spa rất đẹp. Rất đáng tiền.",
                    avatar: "https://ui-avatars.com/api/?name=Le+Nam&background=0F2942&color=fff"
                }
            ];

            renderTestimonial();
            resetAutoPlay();
        } catch (error) {
            console.error("Lỗi khi tải đánh giá:", error);
            showAlert("Không thể tải dữ liệu đánh giá từ máy chủ.", "danger");
        }
    };

    // [POST] Gửi đánh giá mới lên Backend
    reviewForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const nameInput = document.getElementById('reviewer-name').value;
        const starsInput = parseInt(document.getElementById('review-stars').value);
        const textInput = document.getElementById('review-content').value;

        // Dữ liệu sẽ gửi đi
        const newReviewData = {
            name: nameInput,
            stars: starsInput,
            text: textInput
        };

        try {
            /*
            // BỎ COMMENT ĐOẠN NÀY ĐỂ GỬI API THẬT
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(newReviewData)
            });
            if (!response.ok) throw new Error('Save failed');
            */
            
            // --- Giả lập UI: Đẩy dữ liệu mới vào mảng local
            newReviewData.avatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(newReviewData.name)}&background=0F2942&color=fff`;
            reviewList.unshift(newReviewData); // Đưa bài đánh giá mới nhất lên đầu tiên
            currentIndex = 0; // Chuyển con trỏ về 0 để xem liền
            
            renderTestimonial();
            resetAutoPlay();
            
            // Xóa form và hiện thông báo
            reviewForm.reset();
            showAlert("Đánh giá của bạn đã được gửi thành công!", "success");

        } catch (error) {
            console.error("Lỗi khi gửi đánh giá:", error);
            showAlert("Đã xảy ra lỗi khi gửi đánh giá. Vui lòng thử lại sau.", "danger");
        }
    });

    // Khởi chạy khi tải trang
    fetchReviews();
});
