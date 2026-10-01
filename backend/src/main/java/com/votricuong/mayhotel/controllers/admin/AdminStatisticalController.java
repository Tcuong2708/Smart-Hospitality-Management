package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.repositories.InvoiceRepository;

import java.util.Calendar;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminStatisticalController extends BaseController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping("/statistical")
    public String index(Model model) {
        setPageTitle(model, "Thống Kê Doanh Thu - Admin");

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        
        Double[] doanhThuArr = new Double[12];
        Integer[] soDonArr = new Integer[12];
        for (int i = 0; i < 12; i++) {
            doanhThuArr[i] = 0.0;
            soDonArr[i] = 0;
        }

        List<Invoice> allInvoices = invoiceRepository.findAll();
        for (Invoice inv : allInvoices) {
            if (inv.getIsPaid() != null && inv.getIsPaid() && inv.getCreatedAt() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(inv.getCreatedAt());
                if (cal.get(Calendar.YEAR) == currentYear) {
                    int month = cal.get(Calendar.MONTH);
                    doanhThuArr[month] += (inv.getTotalAmount() != null ? inv.getTotalAmount() : 0.0);
                    soDonArr[month]++;
                }
            }
        }

        Double tongDoanhThu = 0.0;
        Integer tongSoDon = 0;
        for (int i = 0; i < 12; i++) {
            tongDoanhThu += doanhThuArr[i];
            tongSoDon += soDonArr[i];
        }

        Double trungBinhThang = tongDoanhThu / 12;

        model.addAttribute("doanhThuArr", doanhThuArr);
        model.addAttribute("soDonArr", soDonArr);
        model.addAttribute("nam", currentYear);
        model.addAttribute("tongDoanhThu", tongDoanhThu);
        model.addAttribute("tongSoDon", tongSoDon);
        model.addAttribute("trungBinhThang", trungBinhThang);

        return render(model, "view/Admin/Statistical/index");
    }
}
