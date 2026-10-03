document.addEventListener('DOMContentLoaded', () => {
    // Chỉ giữ lại logic điền dữ liệu tìm kiếm (checkin, checkout, adults, children) từ URL vào Form sidebar
    const urlParams = new URLSearchParams(window.location.search);
    const checkinParam = urlParams.get('checkin');
    const checkoutParam = urlParams.get('checkout');
    const adultsParam = urlParams.get('adults');
    const childrenParam = urlParams.get('children');

    if (checkinParam && document.getElementById('sbCheckinDate')) {
        document.getElementById('sbCheckinDate').value = checkinParam;
    }
    if (checkoutParam && document.getElementById('sbCheckoutDate')) {
        document.getElementById('sbCheckoutDate').value = checkoutParam;
    }
    if (adultsParam && document.getElementById('sbAdults')) {
        document.getElementById('sbAdults').value = adultsParam;
    }
    if (childrenParam && document.getElementById('sbChildren')) {
        document.getElementById('sbChildren').value = childrenParam;
    }

    // Logic filter phòng và render danh sách đã được xử lý bằng Thymeleaf (Server-Side Rendering)

    // Thêm ràng buộc logic cho ngày tháng
    const checkin = document.getElementById('sbCheckinDate');
    const checkout = document.getElementById('sbCheckoutDate');
    
    if (checkin && checkout) {
        // Ràng buộc min cho ngày nhận phòng
        const today = new Date().toISOString().split('T')[0];
        checkin.setAttribute('min', today);

        // Hàm cập nhật min cho ngày trả phòng
        function updateCheckoutMin() {
            if (checkin.value) {
                const checkinDate = new Date(checkin.value);
                checkinDate.setDate(checkinDate.getDate() + 1);
                const nextDay = checkinDate.toISOString().split('T')[0];
                checkout.setAttribute('min', nextDay);
                
                // Tự động đẩy ngày trả phòng lên nếu nó đang nhỏ hơn hoặc bằng ngày nhận phòng
                if (checkout.value && checkout.value <= checkin.value) {
                    checkout.value = nextDay;
                }
            }
        }

        // Cập nhật ngay khi load (trường hợp có param truyền vào)
        updateCheckoutMin();

        // Lắng nghe sự kiện khi người dùng thay đổi ngày nhận phòng
        checkin.addEventListener('change', updateCheckoutMin);
    }
});
