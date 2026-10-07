const API_URL = 'http://localhost:8080/api/invoices';

const MOCK_INVOICES = {
    "1": {
        id: 1,
        hoTen: "Nguyễn Văn A",
        sdt: "0901234567",
        diaChi: "123 Nguyễn Trãi, Phường 2, Quận 5, TP. Hồ Chí Minh",
        ngayDat: "2026-06-15",
        ngayCheckIn: "2026-06-18",
        ngayCheckOut: "2026-06-20",
        totalPrice: 3600000,
        phong: { name: "Phòng 101 - Deluxe Ocean View", price: 1800000, id: 101 }
    },
    "2": {
        id: 2,
        hoTen: "Trần Thị B",
        sdt: "0987654321",
        diaChi: "45 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
        ngayDat: "2026-06-16",
        ngayCheckIn: "2026-06-17",
        ngayCheckOut: "2026-06-19",
        totalPrice: 2400000,
        phong: { name: "Phòng 202 - Standard Double", price: 1200000, id: 202 }
    },
    "3": {
        id: 3,
        hoTen: "Lê Hoàng C",
        sdt: "0912345678",
        diaChi: "78 Điện Biên Phủ, Phường 15, Quận Bình Thạnh, TP. Hồ Chí Minh",
        ngayDat: "2026-06-10",
        ngayCheckIn: "2026-06-12",
        ngayCheckOut: "2026-06-14",
        totalPrice: 5400000,
        phong: { name: "Phòng 305 - Suite President", price: 2700000, id: 305 }
    }
};

function getMockInvoice(id) {
    const key = String(id || '1');
    if (MOCK_INVOICES[key]) return MOCK_INVOICES[key];
    const numId = parseInt(id) || 1;
    return {
        id: numId,
        hoTen: `Khách hàng #${numId}`,
        sdt: "0901234567",
        diaChi: "140 Lê Trọng Tấn, Q. Tân Phú, TP. HCM",
        ngayDat: "2026-06-15",
        ngayCheckIn: "2026-06-18",
        ngayCheckOut: "2026-06-20",
        totalPrice: 3600000,
        phong: { name: `Phòng ${100 + numId} - Deluxe View`, price: 1800000, id: 100 + numId }
    };
}

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const invoiceId = urlParams.get('id') || '1';
    fetchInvoicePrintData(invoiceId);
});

async function fetchInvoicePrintData(id) {
    let invoice = null;

    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 1500);
        const response = await fetch(`${API_URL}/${id}`, { signal: controller.signal });
        clearTimeout(timeoutId);
        if (response.ok) {
            invoice = await response.json();
        }
    } catch (error) {
        console.warn('API fetch failed, using fallback mock data', error);
    }

    if (!invoice) {
        invoice = getMockInvoice(id);
    }

    const spinner = document.getElementById('loading-spinner');
    if (spinner) spinner.style.display = 'none';

    const controls = document.getElementById('controls');
    if (controls) controls.style.display = 'block';

    const content = document.getElementById('invoice-content');
    if (content) content.style.display = 'block';

    const now = new Date();
    const elPrintDate = document.getElementById('print-date');
    if (elPrintDate) elPrintDate.textContent = `Ngày xuất: ${now.toLocaleDateString('vi-VN')} ${now.toLocaleTimeString('vi-VN')}`;

    const elId = document.getElementById('invoice-id');
    if (elId) elId.textContent = `#HD-${invoice.id}`;

    const elName = document.getElementById('customer-name');
    if (elName) elName.textContent = invoice.hoTen || '';

    const elPhone = document.getElementById('customer-phone');
    if (elPhone) elPhone.textContent = `SĐT: ${invoice.sdt || ''}`;

    const elAddr = document.getElementById('customer-address');
    if (elAddr) elAddr.textContent = invoice.diaChi ? `Địa chỉ: ${invoice.diaChi}` : 'Địa chỉ: Chưa cập nhật';

    const elCheckin = document.getElementById('checkin-date');
    if (elCheckin) elCheckin.textContent = invoice.ngayCheckIn || '';

    const elCheckout = document.getElementById('checkout-date');
    if (elCheckout) elCheckout.textContent = invoice.ngayCheckOut || '';

    let nights = 1;
    if (invoice.ngayCheckIn && invoice.ngayCheckOut) {
        const checkin = new Date(invoice.ngayCheckIn);
        const checkout = new Date(invoice.ngayCheckOut);
        const diffTime = Math.abs(checkout - checkin);
        nights = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        if (isNaN(nights) || nights === 0) nights = 1;
    }
    const elNights = document.getElementById('nights');
    if (elNights) elNights.textContent = `(${nights} đêm)`;

    const roomName = invoice.phong ? invoice.phong.name : (invoice.maPhong ? `Phòng #${invoice.maPhong}` : 'Phòng VIP');
    const elRoomName = document.getElementById('room-name');
    if (elRoomName) elRoomName.textContent = roomName;

    const elRoomNights = document.getElementById('room-nights');
    if (elRoomNights) elRoomNights.textContent = nights;

    const formatMoney = (val) => new Intl.NumberFormat('vi-VN').format(val || 0) + ' đ';
    const roomPrice = invoice.phong ? formatMoney(invoice.phong.price) : '-';
    const elRoomPrice = document.getElementById('room-price');
    if (elRoomPrice) elRoomPrice.textContent = roomPrice;

    const totalPrice = formatMoney(invoice.totalPrice || invoice.totalAmount || 3600000);
    const elRoomTotal = document.getElementById('room-total');
    if (elRoomTotal) elRoomTotal.textContent = totalPrice;

    const elInvTotal = document.getElementById('invoice-total');
    if (elInvTotal) elInvTotal.textContent = totalPrice;

    // Trigger print dialog after small delay to render
    setTimeout(() => {
        window.print();
    }, 500);
}
