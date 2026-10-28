package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.services.RevenueExportService;
import com.votricuong.mayhotel.services.RevenueReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping({"/admin/statistical", "/manager/statistical"})
public class AdminStatisticalController extends BaseController {
    private final RevenueReportService reportService;
    private final RevenueExportService exportService;

    public AdminStatisticalController(RevenueReportService reportService, RevenueExportService exportService) {
        this.reportService = reportService;
        this.exportService = exportService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String from,
                        @RequestParam(required = false) String to,
                        Model model) {
        setPageTitle(model, "Thống kê doanh thu");
        try {
            RevenueReportService.Report report = reportService.build(from, to);
            addReport(model, report);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            addReport(model, reportService.build(null, null));
        }
        setExtraCSS(model, "view/manager/statistical/index :: extra_css");
        setExtraJS(model, "view/manager/statistical/index :: extra_js");
        return render(model, "view/manager/statistical/index");
    }

    private void addReport(Model model, RevenueReportService.Report report) {
        model.addAttribute("report", report);
        model.addAttribute("chartLabels", report.getBuckets().stream().map(RevenueReportService.Bucket::getLabel).toList());
        model.addAttribute("chartRevenue", report.getBuckets().stream().map(RevenueReportService.Bucket::getRevenue).toList());
        model.addAttribute("chartBookings", report.getBuckets().stream().map(RevenueReportService.Bucket::getBookingCount).toList());
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> excel(@RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to) throws Exception {
        RevenueReportService.Report report = reportService.build(from, to);
        return download(exportService.excel(report), report, "xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to) throws Exception {
        RevenueReportService.Report report = reportService.build(from, to);
        return download(exportService.pdf(report), report, "pdf", MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> download(byte[] bytes, RevenueReportService.Report report, String extension, MediaType type) {
        DateTimeFormatter compact = DateTimeFormatter.ofPattern("yyyyMMdd");
        String name = "may-hotel-doanh-thu_" + compact.format(report.getFromDate()) + "-" + compact.format(report.getToDate()) + "." + extension;
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(name, StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }
}
