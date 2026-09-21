package com.votricuong.mayhotel.controllers;

import org.springframework.ui.Model;

public abstract class BaseController {

    protected String render(Model model, String viewPath) {
        model.addAttribute("view", viewPath);
        return "layout/layout";
    }

    protected void setPageTitle(Model model, String title) {
        model.addAttribute("pageTitle", title);
    }

    protected void setExtraCSS(Model model, String cssPath) {
        model.addAttribute("extra_css", cssPath);
    }

    protected void setExtraJS(Model model, String jsPath) {
        model.addAttribute("extra_js", jsPath);
    }
}
