package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.services.AiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/ai", "/api/reception"})
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // This endpoint can be called by Flutter app
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askAi(
            @RequestBody Map<String, String> request,
            HttpSession httpSession) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        String aiResponse = aiService.getAiResponse(prompt, resolveConversationId(request, httpSession));
        return ResponseEntity.ok(responseBody(aiResponse));
    }
    
    @PostMapping({"/chat", "/chatbot"})
    public ResponseEntity<Map<String, String>> chatAi(
            @RequestBody Map<String, String> request,
            HttpSession httpSession) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            message = request.get("prompt"); 
        }
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        String aiResponse = aiService.getAiResponse(message, resolveConversationId(request, httpSession));
        return ResponseEntity.ok(responseBody(aiResponse));
    }

    private String resolveConversationId(Map<String, String> request, HttpSession httpSession) {
        String suppliedId = request.get("user_id");
        if (suppliedId == null || suppliedId.isBlank()) {
            suppliedId = request.get("session_id");
        }
        return suppliedId == null || suppliedId.isBlank() ? httpSession.getId() : suppliedId.trim();
    }

    private Map<String, String> responseBody(String aiResponse) {
        // Giữ ba tên trường để web, mobile và các client cũ cùng tương thích.
        return Map.of(
                "response", aiResponse,
                "answer", aiResponse,
                "reply", aiResponse);
    }
}
