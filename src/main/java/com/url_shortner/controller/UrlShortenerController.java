package com.url_shortner.controller;

import com.url_shortner.dto.UrlRequest;
import com.url_shortner.dto.UrlResponse;
import com.url_shortner.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService service;

    @PostMapping("/api/shorten")
    public ResponseEntity<UrlResponse> shorten(@Valid @RequestBody UrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.shorten(request));
    }

    // GET /{shortCode}  — redirect to original URL
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String longUrl = service.redirect(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, longUrl);
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    // GET /api/stats/{shortCode}  — get click stats for a short URL
    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<UrlResponse> getStats(@PathVariable String shortCode) {
        return ResponseEntity.ok(service.getStats(shortCode));
    }

    // GET /api/urls  — list all shortened URLs
    @GetMapping("/api/urls")
    public ResponseEntity<List<UrlResponse>> getAllUrls() {
        return ResponseEntity.ok(service.getAllUrls());
    }

    // DELETE /api/urls/{shortCode}  — delete a short URL
    @DeleteMapping("/api/urls/{shortCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String shortCode) {
        service.deleteUrl(shortCode);
        return ResponseEntity.noContent().build();
    }
}
