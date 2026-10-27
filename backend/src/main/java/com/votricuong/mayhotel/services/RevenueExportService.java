package com.votricuong.mayhotel.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class RevenueExportService {
    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public byte[] excel(RevenueReportService.Report report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Doanh thu");
            CellStyle title = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 16); title.setFont(titleFont);
            CellStyle header = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true); headerFont.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(headerFont); header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0 \"VNĐ\""));

            int rowIndex = 0;
            Row heading = sheet.createRow(rowIndex++);
            heading.createCell(0).setCellValue("MAY HOTEL - BÁO CÁO THỐNG KÊ DOANH THU");
            heading.getCell(0).setCellStyle(title);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));
            sheet.createRow(rowIndex++).createCell(0).setCellValue("Kỳ báo cáo: " + report.getFromDisplay() + " đến " + report.getToDisplay());
            sheet.createRow(rowIndex++).createCell(0).setCellValue("Thời điểm xuất: " + ZonedDateTime.now(RevenueReportService.HOTEL_ZONE).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            rowIndex++;
            String[][] metrics = {
                    {"Doanh thu gộp", String.valueOf(report.getGrossRevenue())},
                    {"Tiền hoàn", String.valueOf(report.getRefundAmount())},
                    {"Doanh thu thực nhận", String.valueOf(report.getNetRevenue())},
                    {"Hóa đơn đã thanh toán", String.valueOf(report.getPaidInvoiceCount())},
                    {"Booking", String.valueOf(report.getBookingCount())}
            };
            for (String[] metric : metrics) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(metric[0]);
                Cell value = row.createCell(1);
                value.setCellValue(Double.parseDouble(metric[1]));
                if (rowIndex <= 8) value.setCellStyle(money);
            }
            rowIndex++;
            String[] columns = {"Mã HĐ", "Mã booking", "Ngày", "Khách hàng", "SĐT", "Tổng tiền", "Hoàn tiền", "Thực nhận", "Thanh toán", "Trạng thái"};
            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < columns.length; i++) { headerRow.createCell(i).setCellValue(columns[i]); headerRow.getCell(i).setCellStyle(header); }
            int tableStart = rowIndex - 1;
            for (RevenueReportService.Row item : report.getInvoices()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.getId() == null ? "" : item.getId().toString());
                row.createCell(1).setCellValue(item.getBookingId() == null ? "" : item.getBookingId().toString());
                row.createCell(2).setCellValue(item.getDateDisplay());
                row.createCell(3).setCellValue(item.getGuestName());
                row.createCell(4).setCellValue(item.getPhone());
                row.createCell(5).setCellValue(item.getGrossAmount()); row.getCell(5).setCellStyle(money);
                row.createCell(6).setCellValue(item.getRefundAmount()); row.getCell(6).setCellStyle(money);
                row.createCell(7).setCellValue(item.getNetAmount()); row.getCell(7).setCellStyle(money);
                row.createCell(8).setCellValue(item.getPayMethod());
                row.createCell(9).setCellValue(item.getStatus());
            }
            sheet.setAutoFilter(new CellRangeAddress(tableStart, Math.max(tableStart, rowIndex - 1), 0, 9));
            sheet.createFreezePane(0, tableStart + 1);
            for (int i = 0; i < columns.length; i++) { sheet.autoSizeColumn(i); sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 700, 15000)); }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] pdf(RevenueReportService.Report report) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont font = loadFont(document);
            PDFont fallback = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont usedFont = font == null ? fallback : font;
            PdfPageWriter writer = new PdfPageWriter(document, usedFont);
            writer.line("MAY HOTEL - BÁO CÁO THỐNG KÊ DOANH THU", 16, true);
            writer.line("Kỳ báo cáo: " + report.getFromDisplay() + " đến " + report.getToDisplay(), 11, false);
            writer.line("Xuất lúc: " + ZonedDateTime.now(RevenueReportService.HOTEL_ZONE).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), 9, false);
            writer.space(8);
            writer.line("Doanh thu gộp: " + money(report.getGrossRevenue()) + " VNĐ    |    Hoàn tiền: " + money(report.getRefundAmount()) + " VNĐ", 10, false);
            writer.line("Thực nhận: " + money(report.getNetRevenue()) + " VNĐ    |    Hóa đơn: " + report.getPaidInvoiceCount() + "    |    Booking: " + report.getBookingCount(), 10, false);
            writer.space(10);
            writer.line("Mã HĐ | Booking | Ngày | Khách hàng | Tổng | Hoàn | Thực nhận | Phương thức", 8, true);
            for (RevenueReportService.Row row : report.getInvoices()) {
                writer.line(s(row.getId()) + " | " + s(row.getBookingId()) + " | " + row.getDateDisplay() + " | " + trim(row.getGuestName(), 22)
                        + " | " + money(row.getGrossAmount()) + " | " + money(row.getRefundAmount()) + " | "
                        + money(row.getNetAmount()) + " | " + trim(row.getPayMethod(), 14), 7, false);
            }
            writer.close();
            document.save(out);
            return out.toByteArray();
        }
    }

    private PDFont loadFont(PDDocument document) {
        List<Path> paths = List.of(
                Path.of("C:/Windows/Fonts/arial.ttf"),
                Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                Path.of("/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"));
        for (Path path : paths) {
            if (!Files.isRegularFile(path)) continue;
            try (InputStream input = Files.newInputStream(path)) { return PDType0Font.load(document, input); }
            catch (IOException ignored) { }
        }
        return null;
    }

    private String money(double value) { return MONEY.format(Math.round(value)); }
    private String s(Object value) { return value == null ? "" : value.toString(); }
    private String trim(String value, int max) { return value == null ? "" : value.length() <= max ? value : value.substring(0, max - 1) + "…"; }

    private static class PdfPageWriter implements AutoCloseable {
        private final PDDocument document; private final PDFont font;
        private PDPageContentStream stream; private float y;
        PdfPageWriter(PDDocument document, PDFont font) throws IOException { this.document = document; this.font = font; newPage(); }
        void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page); stream = new PDPageContentStream(document, page); y = page.getMediaBox().getHeight() - 42;
        }
        void line(String text, float size, boolean strong) throws IOException {
            if (y < 38) newPage();
            stream.beginText(); stream.setFont(font, strong ? size + 0.4f : size); stream.newLineAtOffset(36, y);
            stream.showText(text == null ? "" : text); stream.endText(); y -= Math.max(12, size + 5);
        }
        void space(float amount) { y -= amount; }
        public void close() throws IOException { if (stream != null) stream.close(); }
    }
}
