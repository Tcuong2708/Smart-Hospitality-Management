package com.votricuong.mayhotel.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.votricuong.mayhotel.services.AiService;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceImpl.class);

    private final RestTemplate restTemplate;
    private final String aiChatUrl;
    private final String internalToken;

    // Constructor 2 tham số tương thích với Unit Test
    public AiServiceImpl(RestTemplate restTemplate, String aiServerUrl) {
        this(restTemplate, aiServerUrl, "mayhotel_internal_token_2026");
    }

    // Đánh dấu @Autowired để Spring biết dùng constructor này khi khởi tạo Bean
    @Autowired
    public AiServiceImpl(
            RestTemplate restTemplate,
            @Value("${ai.server.url:http://127.0.0.1:5000}") String aiServerUrl,
            @Value("${chatbot.internal.token:mayhotel_internal_token_2026}") String internalToken) {
        this.restTemplate = restTemplate;
        this.aiChatUrl = aiServerUrl.replaceAll("/+$", "") + "/api/v1/ai/chat";
        this.internalToken = internalToken;
    }

    @Override
    public String getAiResponse(String prompt, String conversationId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (internalToken != null && !internalToken.isBlank()) {
                headers.set("Authorization", "Bearer " + internalToken);
                headers.set("X-Internal-Token", internalToken);
            }

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("message", prompt);
            requestBody.put("user_id", conversationId);
            requestBody.put("session_id", conversationId);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(aiChatUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map responseBody = response.getBody();

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
            logger.error("Lỗi kết nối tới AI Server tại URL {}: {}", aiChatUrl, e.getMessage(), e);
            return "Trợ lý AI đang tạm thời gián đoạn. Vui lòng thử lại sau hoặc liên hệ lễ tân để được hỗ trợ.";
        }
    }
}