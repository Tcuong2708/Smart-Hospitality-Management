package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPromotionController extends BaseController {

    @GetMapping("/promotion")
    public String index(Model model) {
        setPageTitle(model, "Quản Lý Khuyến Mãi - Admin");
        return render(model, "view/Admin/Promotion/index");
    }
}
