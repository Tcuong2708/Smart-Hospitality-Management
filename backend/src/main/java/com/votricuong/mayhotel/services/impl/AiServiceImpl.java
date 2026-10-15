package com.votricuong.mayhotel.services.impl;

import com.votricuong.mayhotel.services.AiService;
import org.springframework.beans.factory.annotation.Value;
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
    private final String aiChatUrl;

    public AiServiceImpl(
            RestTemplate restTemplate,
            @Value("${ai.server.url:http://localhost:5000}") String aiServerUrl) {
        this.restTemplate = restTemplate;
        this.aiChatUrl = aiServerUrl.replaceAll("/+$", "") + "/api/v1/ai/chat";
    }

    @Override
    public String getAiResponse(String prompt, String conversationId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("message", prompt);
            requestBody.put("user_id", conversationId);
            requestBody.put("session_id", conversationId);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(aiChatUrl, request, Map.class);
            
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
            return "Xin lỗi, hệ thống AI hiện không thể phản hồi. Vui lòng thử lại hoặc liên hệ lễ tân.";
        } catch (Exception e) {
            return "Trợ lý AI đang tạm thời gián đoạn. Vui lòng thử lại sau hoặc liên hệ lễ tân để được hỗ trợ.";
        }
    }
}
