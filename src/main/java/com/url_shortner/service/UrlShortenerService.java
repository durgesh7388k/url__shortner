package com.url_shortner.service;

import com.url_shortner.dto.UrlRequest;
import com.url_shortner.dto.UrlResponse;
import com.url_shortner.exception.AliasAlreadyExistsException;
import com.url_shortner.exception.UrlNotFoundException;
import com.url_shortner.model.UrlMapping;
import com.url_shortner.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UrlMappingRepository repository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public UrlResponse shorten(UrlRequest request) {
        // Return existing short URL if long URL already shortened
        return repository.findByLongUrl(request.getLongUrl())
                .map(this::toResponse)
                .orElseGet(() -> createNew(request));
    }

    @Transactional
    public String redirect(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        mapping.setClickCount(mapping.getClickCount() + 1);
        repository.save(mapping);
        return mapping.getLongUrl();
    }

    public UrlResponse getStats(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return toResponse(mapping);
    }

    public List<UrlResponse> getAllUrls() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteUrl(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        repository.delete(mapping);
    }

    private UrlResponse createNew(UrlRequest request) {
        String code = resolveCode(request.getCustomAlias());
        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl(request.getLongUrl());
        mapping.setShortCode(code);
        repository.save(mapping);
        return toResponse(mapping);
    }

    private String resolveCode(String customAlias) {
        if (customAlias != null && !customAlias.isBlank()) {
            if (repository.existsByShortCode(customAlias)) {
                throw new AliasAlreadyExistsException(customAlias);
            }
            return customAlias;
        }
        String code;
        do {
            code = generateCode();
        } while (repository.existsByShortCode(code));
        return code;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private UrlResponse toResponse(UrlMapping mapping) {
        return new UrlResponse(
                mapping.getLongUrl(),
                baseUrl + "/" + mapping.getShortCode(),
                mapping.getShortCode(),
                mapping.getCreatedAt(),
                mapping.getClickCount()
        );
    }
}
