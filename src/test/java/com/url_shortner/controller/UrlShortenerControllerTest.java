package com.url_shortner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url_shortner.dto.UrlRequest;
import com.url_shortner.dto.UrlResponse;
import com.url_shortner.exception.AliasAlreadyExistsException;
import com.url_shortner.exception.UrlNotFoundException;
import com.url_shortner.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UrlShortenerControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private UrlShortenerService service;

    private UrlResponse sampleResponse;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        sampleResponse = new UrlResponse(
                "https://www.example.com/some/very/long/path",
                "http://localhost:8080/XPLEARN",
                "XPLEARN",
                now,
                0L
        );
    }

    // ─── POST /api/shorten ───────────────────────────────────────────────────

    @Test
    void shorten_validRequest_returns201WithShortUrl() throws Exception {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/some/very/long/path");

        when(service.shorten(any(UrlRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("XPLEARN"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/XPLEARN"))
                .andExpect(jsonPath("$.longUrl").value("https://www.example.com/some/very/long/path"))
                .andExpect(jsonPath("$.clickCount").value(0));
    }

    @Test
    void shorten_withCustomAlias_returns201WithCustomShortUrl() throws Exception {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");
        request.setCustomAlias("myalias");

        UrlResponse customResponse = new UrlResponse(
                "https://www.example.com/path",
                "http://localhost:8080/myalias",
                "myalias", now, 0L
        );
        when(service.shorten(any(UrlRequest.class))).thenReturn(customResponse);

        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("myalias"));
    }

    @Test
    void shorten_blankUrl_returns400() throws Exception {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("");

        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shorten_nullUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shorten_invalidUrlFormat_returns400() throws Exception {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("not-a-valid-url");

        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("https://")));
    }

    @Test
    void shorten_aliasAlreadyExists_returns409() throws Exception {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");
        request.setCustomAlias("taken");

        when(service.shorten(any(UrlRequest.class)))
                .thenThrow(new AliasAlreadyExistsException("taken"));

        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("taken")));
    }

    @Test
    void shorten_withHttpsUrl_returns201() throws Exception {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://example.com");

        when(service.shorten(any(UrlRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shorten_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shorten_serviceThrowsException_returns500() throws Exception {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");

        when(service.shorten(any(UrlRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("An unexpected error occurred"));
    }

    // ─── GET /{shortCode} ────────────────────────────────────────────────────

    @Test
    void redirect_validShortCode_returns302WithLocationHeader() throws Exception {
        when(service.redirect("XPLEARN"))
                .thenReturn("https://www.example.com/some/very/long/path");

        mockMvc.perform(get("/XPLEARN"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.example.com/some/very/long/path"));
    }

    @Test
    void redirect_unknownShortCode_returns404() throws Exception {
        when(service.redirect("UNKNOWN"))
                .thenThrow(new UrlNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("UNKNOWN")));
    }

    // ─── GET /api/stats/{shortCode} ──────────────────────────────────────────

    @Test
    void getStats_validShortCode_returns200WithStats() throws Exception {
        UrlResponse statsResponse = new UrlResponse(
                "https://www.example.com/path",
                "http://localhost:8080/XPLEARN",
                "XPLEARN", now, 42L
        );
        when(service.getStats("XPLEARN")).thenReturn(statsResponse);

        mockMvc.perform(get("/api/stats/XPLEARN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("XPLEARN"))
                .andExpect(jsonPath("$.clickCount").value(42));
    }

    @Test
    void getStats_unknownShortCode_returns404() throws Exception {
        when(service.getStats("UNKNOWN"))
                .thenThrow(new UrlNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/stats/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("UNKNOWN")));
    }

    @Test
    void getStats_returnsZeroClickCount() throws Exception {
        when(service.getStats("XPLEARN")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/stats/XPLEARN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(0));
    }

    // ─── GET /api/urls ───────────────────────────────────────────────────────

    @Test
    void getAllUrls_returnsListOf200() throws Exception {
        UrlResponse second = new UrlResponse(
                "https://www.google.com", "http://localhost:8080/ABCDEFG",
                "ABCDEFG", now, 5L
        );
        when(service.getAllUrls()).thenReturn(List.of(sampleResponse, second));

        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].shortCode").value("XPLEARN"))
                .andExpect(jsonPath("$[1].shortCode").value("ABCDEFG"));
    }

    @Test
    void getAllUrls_emptyList_returns200WithEmptyArray() throws Exception {
        when(service.getAllUrls()).thenReturn(List.of());

        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllUrls_serviceThrowsException_returns500() throws Exception {
        when(service.getAllUrls()).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isInternalServerError());
    }

    // ─── DELETE /api/urls/{shortCode} ────────────────────────────────────────

    @Test
    void deleteUrl_validShortCode_returns204() throws Exception {
        doNothing().when(service).deleteUrl("XPLEARN");

        mockMvc.perform(delete("/api/urls/XPLEARN").with(csrf()))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deleteUrl("XPLEARN");
    }

    @Test
    void deleteUrl_unknownShortCode_returns404() throws Exception {
        doThrow(new UrlNotFoundException("UNKNOWN")).when(service).deleteUrl("UNKNOWN");

        mockMvc.perform(delete("/api/urls/UNKNOWN").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("UNKNOWN")));
    }

    @Test
    void deleteUrl_serviceThrowsException_returns500() throws Exception {
        doThrow(new RuntimeException("DB error")).when(service).deleteUrl(eq("XPLEARN"));

        mockMvc.perform(delete("/api/urls/XPLEARN").with(csrf()))
                .andExpect(status().isInternalServerError());
    }
}
