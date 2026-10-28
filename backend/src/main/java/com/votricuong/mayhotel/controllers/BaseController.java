package com.votricuong.mayhotel.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

public abstract class BaseController {

    protected String render(Model model, String viewPath) {
        model.addAttribute("view", viewPath);
        if (isBackOfficeView(viewPath)) {
            return "layout/admin";
        }
        return "layout/layout";
    }

    @ModelAttribute
    public void addCommonLayoutAttributes(HttpServletRequest request, Model model) {
        model.addAttribute("currentPath", request.getRequestURI());
    }

    protected void setPageTitle(Model model, String title) {
        model.addAttribute("pageTitle", title);
    }

    protected void setExtraCSS(Model model, String cssPath) {
        model.addAttribute("extra_css", cssPath);
        setFragmentAttribute(model, "extra_css", cssPath);
    }

    protected void setExtraJS(Model model, String jsPath) {
        model.addAttribute("extra_js", jsPath);
        setFragmentAttribute(model, "extra_js", jsPath);
    }

    private boolean isBackOfficeView(String viewPath) {
        return viewPath != null && (viewPath.startsWith("view/admin/")
                || viewPath.startsWith("view/manager/")
                || viewPath.startsWith("view/receptionist/"));
    }

    private void setFragmentAttribute(Model model, String attribute, String fragmentPath) {
        if (fragmentPath == null || fragmentPath.isBlank()) {
            return;
        }
        String[] parts = fragmentPath.split("\\s*::\\s*", 2);
        model.addAttribute(attribute + "_template", parts[0].trim());
        model.addAttribute(attribute + "_fragment", parts.length > 1 ? parts[1].trim() : attribute);
    }
}
