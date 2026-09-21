// Dữ liệu mẫu ban đầu
let reviews = [
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

let currentIndex = 0;

function renderTestimonial() {
    if (reviews.length === 0) {
        document.querySelector('.testimonial-box').style.display = 'none';
        return;
    } else {
        document.querySelector('.testimonial-box').style.display = 'block';
    }

    const review = reviews[currentIndex];
    document.getElementById("name").innerText = review.name;
    document.getElementById("text").innerText = review.text;

    // Render sao
    let starHTML = "";
    for (let i = 0; i < review.stars; i++) {
        starHTML += "<i class='bi bi-star-fill'></i> ";
    }
    for (let i = review.stars; i < 5; i++) {
        starHTML += "<i class='bi bi-star text-muted opacity-25'></i> ";
    }
    document.getElementById("stars").innerHTML = starHTML;

    // Render Avatar
    document.getElementById("avatar").src = review.avatar || `https://ui-avatars.com/api/?name=${review.name}&background=C5A017&color=fff`;
}

function nextTestimonial() {
    currentIndex = (currentIndex + 1) % reviews.length;
    renderTestimonial();
}

function prevTestimonial() {
    currentIndex = (currentIndex - 1 + reviews.length) % reviews.length;
    renderTestimonial();
}

function deleteTestimonial() {
    if (confirm("Bạn có chắc chắn muốn xóa đánh giá này?")) {
        reviews.splice(currentIndex, 1);
        if (reviews.length > 0) {
            currentIndex = currentIndex % reviews.length; // Điều chỉnh index
            renderTestimonial();
        } else {
            renderTestimonial(); // Ẩn box
        }
    }
}

function submitReview(event) {
    event.preventDefault();

    const name = document.getElementById("userName").value;
    const stars = parseInt(document.getElementById("userStars").value);
    const text = document.getElementById("userText").value;

    const newReview = {
        name: name,
        stars: stars,
        text: text,
        avatar: `https://ui-avatars.com/api/?name=${name}&background=0F2942&color=fff`
    };

    reviews.push(newReview);
    currentIndex = reviews.length - 1; // Chuyển đến review mới nhất
    renderTestimonial();

    // Reset form
    document.getElementById("reviewForm").reset();
    alert("Cảm ơn bạn đã gửi đánh giá!");
}

// Khởi chạy lần đầu
document.addEventListener("DOMContentLoaded", function () {
    renderTestimonial();
});

setInterval(() => {
    nextTestimonial();
}, 5000);