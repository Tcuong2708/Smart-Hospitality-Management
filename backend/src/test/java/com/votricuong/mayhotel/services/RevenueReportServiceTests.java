package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RevenueReportServiceTests {
    private final InvoiceRepository repository = mock(InvoiceRepository.class);
    private final RevenueReportService service = new RevenueReportService(repository);

    @Test
    void reportsBySuccessfulPaymentDateAndSubtractsRefund() {
        Invoice invoice = Invoice.builder().id(1L).bookingId(10L).isPaid(true)
                .createdAt(date(1, 10, 2026)).totalAmount(1_000_000D).refundAmount(200_000D)
                .paymentHistory(List.of(Invoice.PaymentTransaction.builder().status("SUCCESS")
                        .paymentDate(date(15, 10, 2026)).build())).build();
        when(repository.findPaidCandidatesForRevenue(any(), any())).thenReturn(List.of(invoice));

        RevenueReportService.Report report = service.build("15-10-2026", "15-10-2026");

        assertThat(report.getGrossRevenue()).isEqualTo(1_000_000D);
        assertThat(report.getRefundAmount()).isEqualTo(200_000D);
        assertThat(report.getNetRevenue()).isEqualTo(800_000D);
        assertThat(report.getPaidInvoiceCount()).isEqualTo(1);
        assertThat(report.getInvoices().get(0).getDateDisplay()).isEqualTo("15-10-2026");
    }

    @Test
    void rejectsNonVietnameseDateFormatAndReversedRange() {
        assertThatThrownBy(() -> service.build("2026-10-15", "16-10-2026"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dd-MM-yyyy");
        assertThatThrownBy(() -> service.build("16-10-2026", "15-10-2026"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Ngày bắt đầu");
    }

    @Test
    void exportsValidExcelAndPdfPayloads() throws Exception {
        when(repository.findPaidCandidatesForRevenue(any(), any())).thenReturn(List.of());
        RevenueReportService.Report report = service.build("01-10-2026", "31-10-2026");
        RevenueExportService export = new RevenueExportService();
        byte[] excel = export.excel(report);
        byte[] pdf = export.pdf(report);
        assertThat(excel).startsWith((byte) 0x50, (byte) 0x4B);
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    private Date date(int day, int month, int year) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
    }
}
