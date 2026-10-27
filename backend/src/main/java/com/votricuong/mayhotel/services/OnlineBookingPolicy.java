package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

public final class OnlineBookingPolicy {

    public static final double DEPOSIT_RATE = 0.10D;
    public static final int PAYMENT_HOLD_MINUTES = 15;
    public static final String DEPOSIT_PENDING = "PENDING";
    public static final String DEPOSIT_PAID = "PAID";
    public static final String DEPOSIT_FAILED = "FAILED";
    public static final String BOOKING_AWAITING_DEPOSIT = "Chờ thanh toán cọc";
    public static final String BOOKING_DEPOSIT_CONFIRMED = "Đã xác nhận cọc";

    private OnlineBookingPolicy() {
    }

    public static int totalGuests(int adults, int children, int infants) {
        if (adults < 1) throw new IllegalArgumentException("Số người lớn phải từ 1 trở lên");
        if (children < 0) throw new IllegalArgumentException("Số trẻ em không được âm");
        if (infants < 0) throw new IllegalArgumentException("Số em bé không được âm");
        return Math.addExact(Math.addExact(adults, children), infants);
    }

    public static int requiredIdentityVerifications(BookingOrder order) {
        if (order == null) return 1;
        if (order.getAdultCount() != null && order.getAdultCount() > 0) {
            return order.getAdultCount();
        }
        return order.getGuestCount() == null || order.getGuestCount() < 1 ? 1 : order.getGuestCount();
    }

    public static long depositAmount(double netRoomAmount) {
        if (netRoomAmount <= 0D) return 0L;
        return BigDecimal.valueOf(netRoomAmount)
                .multiply(BigDecimal.valueOf(DEPOSIT_RATE))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public static Date paymentExpiryFromNow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, PAYMENT_HOLD_MINUTES);
        return calendar.getTime();
    }

    public static long payableDeposit(Invoice invoice) {
        if (invoice == null) return 0L;
        if (invoice.getDepositRequiredAmount() != null) return invoice.getDepositRequiredAmount();
        return invoice.getTotalAmount() == null ? 0L : Math.round(invoice.getTotalAmount());
    }
}
