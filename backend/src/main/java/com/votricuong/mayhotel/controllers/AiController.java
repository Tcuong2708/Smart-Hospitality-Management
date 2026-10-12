package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.services.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // This endpoint can be called by Flutter app
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askAi(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        String aiResponse = aiService.getAiResponse(prompt);
        return ResponseEntity.ok(Map.of("response", aiResponse));
    }
    
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatAi(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            message = request.get("prompt"); 
        }
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        String aiResponse = aiService.getAiResponse(message);
        return ResponseEntity.ok(Map.of("response", aiResponse));
    }    
}
