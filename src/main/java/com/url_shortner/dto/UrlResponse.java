package com.url_shortner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UrlResponse {

    private String longUrl;
    private String shortUrl;
    private String shortCode;
    private LocalDateTime createdAt;
    private long clickCount;
}
