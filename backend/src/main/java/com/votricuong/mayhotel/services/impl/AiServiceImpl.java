package com.votricuong.mayhotel.services.impl;

import com.votricuong.mayhotel.services.AiService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate;
    private final String AI_SERVER_URL = "http://localhost:5000/api/v1/ai/chat";

    public AiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getAiResponse(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("message", prompt);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(AI_SERVER_URL, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map responseBody = response.getBody();
                
                // Ưu tiên lấy trực tiếp key "reply" hoặc "answer"
                if (responseBody.containsKey("reply") && responseBody.get("reply") != null) {
                    String val = responseBody.get("reply").toString();
                    if (!val.equalsIgnoreCase("success")) return val;
                }
                if (responseBody.containsKey("answer") && responseBody.get("answer") != null) {
                    String val = responseBody.get("answer").toString();
                    if (!val.equalsIgnoreCase("success")) return val;
                }
                if (responseBody.containsKey("response") && responseBody.get("response") != null) {
                    String val = responseBody.get("response").toString();
                    if (!val.equalsIgnoreCase("success")) return val;
                }

                // Duyệt qua tất cả các key, bỏ qua các key status/success để tìm nội dung thực sự
                for (Object key : responseBody.keySet()) {
                    Object val = responseBody.get(key);
                    if (val != null) {
                        String strVal = val.toString();
                        if (!strVal.equalsIgnoreCase("success") && !key.toString().equalsIgnoreCase("status")) {
                            return strVal;
                        }
                    }
                }
            }
            return "Xin lỗi, hệ thống AI hiện không thể phản hồi.";
        } catch (Exception e) {
            return "Lỗi kết nối tới hệ thống AI: " + e.getMessage();
        }
    }
}