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
});
