package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.services.impl.AiServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiServiceImplTests {

    @Test
    void usesConfiguredServerAndForwardsConversationId() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AiServiceImpl service = new AiServiceImpl(restTemplate, "http://ai.example:5000/");
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(
                eq("http://ai.example:5000/api/v1/ai/chat"),
                requestCaptor.capture(),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("reply", "Xin chào")));

        String result = service.getAiResponse("xin giá", "conversation-123");

        assertThat(result).isEqualTo("Xin chào");
        assertThat(requestCaptor.getValue().getBody()).isEqualTo(Map.of(
                "message", "xin giá",
                "user_id", "conversation-123",
                "session_id", "conversation-123"));
    }
}
