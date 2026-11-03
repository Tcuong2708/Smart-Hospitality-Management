package com.votricuong.mayhotel.services;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class PermissionRouteResolver {
    public String resolve(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String action = "GET".equalsIgnoreCase(request.getMethod()) ? "VIEW" : actionFor(request.getMethod(), uri);

        if (uri.startsWith("/admin/roles")) return "SYSTEM.PERMISSIONS." + action;
        if (uri.startsWith("/admin/users") || uri.startsWith("/api/v1/mobile/admin/users")) return "SYSTEM.USERS." + action;
        if (uri.startsWith("/admin/backup") || uri.startsWith("/api/v1/mobile/admin/backup")) return "SYSTEM.BACKUP." + action;
        if (uri.startsWith("/manager/rooms") || uri.contains("/manager/rooms")) return "MANAGER.ROOMS." + action;
        if (uri.startsWith("/manager/categories") || uri.contains("/manager/room-types")) return "MANAGER.ROOM_TYPES." + action;
        if (uri.startsWith("/manager/services") || uri.contains("/manager/services")) return "MANAGER.SERVICES." + action;
        if (uri.startsWith("/manager/staff") || uri.contains("/manager/staff")) return "MANAGER.STAFF." + action;
        if (uri.startsWith("/manager/customers") || uri.contains("/manager/customers")) return "MANAGER.CUSTOMERS." + action;
        if (uri.startsWith("/manager/promotions") || uri.contains("/manager/promotions")) return "MANAGER.PROMOTIONS." + action;
        if (uri.startsWith("/manager/reviews")) return "MANAGER.REVIEWS." + action;
        if (uri.startsWith("/manager/loyalty")) return "MANAGER.LOYALTY." + action;
        if (uri.contains("/statistical") || uri.contains("/reports/revenue"))
            return "REPORT.REVENUE." + (uri.endsWith(".csv") ? "EXPORT" : action);
        if (uri.contains("/room-map")) return "RECEPTION.ROOM_MAP.VIEW";
        if (uri.contains("/check-in") || uri.contains("/assign-room") || uri.contains("manual-verification"))
            return "RECEPTION.CHECK_IN." + action;
        if (uri.contains("/check-out") || uri.endsWith("/checkout")) return "RECEPTION.CHECK_OUT." + action;
        if (uri.contains("/invoices")) return "RECEPTION.INVOICES." + (uri.contains("/print") ? "PRINT" : action);
        if (uri.contains("/ai-no-show") || uri.contains("/no-show")) return "RECEPTION.NO_SHOW." + action;
        if (uri.startsWith("/receptionist/booking") || uri.contains("/reception/direct-bookings")
                || uri.contains("/change-room")) return "RECEPTION.BOOKINGS." + action;
        if (uri.contains("/mobile/operations/reception/bookings") || uri.contains("/mobile/operations/reception/room-types")
                || uri.contains("/mobile/operations/reception/available-rooms")) return "RECEPTION.BOOKINGS." + action;
        if (uri.contains("/mobile/housekeeping")) return "MOBILE.HOUSEKEEPING." + action;
        if (uri.contains("/mobile/services") || uri.contains("service-staff")) return "MOBILE.SERVICE." + action;
        return null;
    }

    private String actionFor(String method, String uri) {
        if ("DELETE".equalsIgnoreCase(method) || uri.contains("/delete")) return "DELETE";
        if ("POST".equalsIgnoreCase(method) && (uri.endsWith("/create") || uri.matches(".*/(rooms|room-types|services|customers|staff|promotions)$"))) return "CREATE";
        return "UPDATE";
    }
}
