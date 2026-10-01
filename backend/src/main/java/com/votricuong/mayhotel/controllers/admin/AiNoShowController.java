package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/receptionist")
public class AiNoShowController extends BaseController {

    @Autowired
    private BookingOrderRepository bookingOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/ai-no-show")
    public String index(Model model) {
        setPageTitle(model, "Trợ lý AI - Rủi ro No-show");

        List<BookingOrder> activeBookings = bookingOrderRepository.findAll();
        List<Map<String, Object>> displayList = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        String aiApiUrl = "http://127.0.0.1:5000/api/v1/ai/predict-noshow";

        for (BookingOrder order : activeBookings) {
            if ("Chờ nhận phòng".equals(order.getStatus()) || "Đã xác nhận cọc".equals(order.getStatus())) {
                // Tự động phân tích AI nếu chưa có
                if (order.getNoShowRiskLevel() == null) {
                    double risk = 0.0;
                    
                    try {
                        // Tính toán các thuộc tính cơ bản
                        long leadTime = 0;
                        if (order.getBookingDate() != null && order.getExpectedIn() != null) {
                            leadTime = (order.getExpectedIn().getTime() - order.getBookingDate().getTime()) / (1000 * 60 * 60 * 24);
                            if (leadTime < 0) leadTime = 0;
                        }
                        
                        int specialRequests = (order.getNotes() != null && !order.getNotes().isEmpty()) ? 1 : 0;
                        
                        // Chuẩn bị payload gửi AI
                        Map<String, Object> dataMap = new HashMap<>();
                        dataMap.put("lead_time", leadTime);
                        dataMap.put("total_of_special_requests", specialRequests);
                        
                        Map<String, Object> requestBody = new HashMap<>();
                        requestBody.put("data", dataMap);
                        
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                        
                        // Gọi API
                        ResponseEntity<Map> response = restTemplate.postForEntity(aiApiUrl, requestEntity, Map.class);
                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            Map<String, Object> body = response.getBody();
                            if ("success".equals(body.get("status"))) {
                                Object riskObj = body.get("risk_percentage");
                                if (riskObj instanceof Number) {
                                    risk = ((Number) riskObj).doubleValue();
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Lỗi gọi AI Model: " + e.getMessage() + ". Sẽ dùng mức rủi ro mặc định.");
                        risk = 15.0; // Dự phòng
                    }
                    order.setNoShowRiskPercentage(Math.round(risk * 10.0) / 10.0);
                    
                    Calendar cal = Calendar.getInstance();
                    if (order.getExpectedIn() != null) {
                        cal.setTime(order.getExpectedIn());
                    }
                    
                    if (risk >= 70.0) {
                        order.setNoShowRiskLevel("Cao");
                        cal.set(Calendar.HOUR_OF_DAY, 14); // Áp dụng quy định hủy sớm (14:00) cho mức độ Cao
                        cal.set(Calendar.MINUTE, 0);
                    } else if (risk >= 40.0) {
                        order.setNoShowRiskLevel("Trung bình");
                        cal.set(Calendar.HOUR_OF_DAY, 18);
                        cal.set(Calendar.MINUTE, 0);
                    } else {
                        order.setNoShowRiskLevel("Thấp");
                        cal.set(Calendar.HOUR_OF_DAY, 18);
                        cal.set(Calendar.MINUTE, 0);
                    }
                    order.setAutoCancelTime(cal.getTime());
                    bookingOrderRepository.save(order);
                }

                Map<String, Object> map = new HashMap<>();
                map.put("order", order);
                
                String customerName = "Khách vãng lai";
                String customerPhone = "";
                if (order.getCustomerId() != null) {
                    User u = userRepository.findById(order.getCustomerId()).orElse(null);
                    if (u != null) {
                        customerName = u.getFullName() != null ? u.getFullName() : u.getUsername();
                        customerPhone = u.getPhone() != null ? u.getPhone() : "Chưa cập nhật";
                    }
                }
                map.put("customerName", customerName);
                map.put("customerPhone", customerPhone);
                displayList.add(map);
            }
        }

        // Sắp xếp theo tỷ lệ rủi ro giảm dần
        displayList.sort((a, b) -> {
            BookingOrder o1 = (BookingOrder) a.get("order");
            BookingOrder o2 = (BookingOrder) b.get("order");
            return Double.compare(o2.getNoShowRiskPercentage(), o1.getNoShowRiskPercentage());
        });

        model.addAttribute("bookings", displayList);
        return render(model, "view/Receptionist/AiNoShow/index");
    }
}
