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
                Object answer = response.getBody().get("response");
                if (answer != null) {
                    return answer.toString();
                }
                for (Object value : response.getBody().values()) {
                    if (value != null) return value.toString();
                }
            }
            return "Xin lỗi, hệ thống AI hiện không thể phản hồi.";
        } catch (Exception e) {
            return "Lỗi kết nối tới hệ thống AI: " + e.getMessage();
        }
    }
}