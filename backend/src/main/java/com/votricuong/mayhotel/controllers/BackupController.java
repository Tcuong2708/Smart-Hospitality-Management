package com.votricuong.mayhotel.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/backup")
public class BackupController extends BaseController {

    @GetMapping
    public String index(Model model) {
        setPageTitle(model, "Sao lưu và Phục hồi Hệ thống");
        return render(model, "view/Admin/Backup/index");
    }
}
