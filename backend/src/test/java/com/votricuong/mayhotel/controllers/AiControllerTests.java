package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.services.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiControllerTests {

    @Test
    void supportsMobileAliasAndCompatibleResponseFields() throws Exception {
        AiService aiService = mock(AiService.class);
        when(aiService.getAiResponse(eq("xin giá"), eq("mobile-123")))
                .thenReturn("Vui lòng cho biết ngày nhận và trả phòng.");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AiController(aiService)).build();

        mockMvc.perform(post("/api/reception/chatbot")
                        .contentType("application/json")
                        .content("{\"message\":\"xin giá\",\"user_id\":\"mobile-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Vui lòng cho biết ngày nhận và trả phòng."))
                .andExpect(jsonPath("$.answer").value("Vui lòng cho biết ngày nhận và trả phòng."))
                .andExpect(jsonPath("$.reply").value("Vui lòng cho biết ngày nhận và trả phòng."));
    }
}
