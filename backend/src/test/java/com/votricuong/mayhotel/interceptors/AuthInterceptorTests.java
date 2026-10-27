package com.votricuong.mayhotel.interceptors;

import com.votricuong.mayhotel.documents.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthInterceptorTests {

    private final AuthInterceptor interceptor = new AuthInterceptor();

    @Test
    void accountantCanViewAndExportRevenueReports() throws Exception {
        assertTrue(invoke("/admin/statistical").allowed());
        assertTrue(invoke("/admin/statistical/export.xlsx").allowed());
        assertTrue(invoke("/admin/statistical/export.pdf").allowed());
    }

    @Test
    void accountantCannotAccessOtherBackOfficeFunctions() throws Exception {
        Result manager = invoke("/manager/rooms");
        Result receptionist = invoke("/receptionist/invoices");
        Result admin = invoke("/admin/users");

        assertFalse(manager.allowed());
        assertFalse(receptionist.allowed());
        assertFalse(admin.allowed());
        assertEquals("/admin/statistical", manager.redirect());
        assertEquals("/admin/statistical", receptionist.redirect());
        assertEquals("/admin/statistical", admin.redirect());
    }

    private Result invoke(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        User accountant = new User();
        accountant.setId(7001L);
        accountant.setRoleId(7L);
        accountant.setUsername("ketoan");
        request.getSession(true).setAttribute("user", accountant);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(request, response, new Object());
        return new Result(allowed, response.getRedirectedUrl());
    }

    private record Result(boolean allowed, String redirect) {
    }
}
