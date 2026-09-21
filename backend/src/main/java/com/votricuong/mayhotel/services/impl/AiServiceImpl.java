package com.votricuong.mayhotel.services.impl;

import com.votricuong.mayhotel.services.AiService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate;

    public AiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getAiResponse(String prompt) {
        // TODO: Replace with actual LLM API URL (e.g., Gemini, OpenAI) and API Key logic.
        // Example logic:
        // String apiUrl = "https://api.openai.com/v1/completions";
        // HttpEntity<String> request = new HttpEntity<>(buildRequestBody(prompt), headers);
        // ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
        // return response.getBody();
        
        return "This is a simulated AI response for the prompt: " + prompt;
    }
}
