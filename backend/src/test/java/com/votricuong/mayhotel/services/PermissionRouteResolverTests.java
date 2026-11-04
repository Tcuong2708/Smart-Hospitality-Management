package com.votricuong.mayhotel.services;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionRouteResolverTests {

    private final PermissionRouteResolver resolver = new PermissionRouteResolver();

    @Test
    void customerServiceCatalogAndOrdersUseCustomerPermissions() {
        assertEquals("MOBILE.CUSTOMER_SERVICE.VIEW",
                resolve("GET", "/api/v1/mobile/services"));
        assertEquals("MOBILE.CUSTOMER_SERVICE.UPDATE",
                resolve("POST", "/api/v1/mobile/services/order"));
        assertEquals("MOBILE.CUSTOMER_SERVICE.UPDATE",
                resolve("POST", "/api/v1/mobile/services/order-batch"));
    }

    @Test
    void serviceStaffTicketsKeepStaffOnlyPermissions() {
        assertEquals("MOBILE.SERVICE.VIEW",
                resolve("GET", "/api/v1/mobile/service-staff/tickets"));
        assertEquals("MOBILE.SERVICE.UPDATE",
                resolve("PATCH", "/api/v1/mobile/service-staff/tickets/10/status"));
    }

    private String resolve(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return resolver.resolve(request);
    }
}
