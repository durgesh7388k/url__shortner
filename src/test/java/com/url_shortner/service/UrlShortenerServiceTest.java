package com.url_shortner.service;

import com.url_shortner.dto.UrlRequest;
import com.url_shortner.dto.UrlResponse;
import com.url_shortner.exception.AliasAlreadyExistsException;
import com.url_shortner.exception.UrlNotFoundException;
import com.url_shortner.model.UrlMapping;
import com.url_shortner.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @InjectMocks
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
    }

    private UrlMapping buildMapping(String longUrl, String shortCode, long clickCount) {
        UrlMapping m = new UrlMapping();
        m.setLongUrl(longUrl);
        m.setShortCode(shortCode);
        m.setClickCount(clickCount);
        return m;
    }

    // ─── shorten() ───────────────────────────────────────────────────────────

    @Test
    void shorten_newUrl_savesAndReturnsShortUrl() {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");

        when(repository.findByLongUrl("https://www.example.com/path")).thenReturn(Optional.empty());
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse response = service.shorten(request);

        assertThat(response.getLongUrl()).isEqualTo("https://www.example.com/path");
        assertThat(response.getShortUrl()).startsWith("http://localhost:8080/");
        assertThat(response.getShortCode()).hasSize(7);
        verify(repository).save(any(UrlMapping.class));
    }

    @Test
    void shorten_existingUrl_returnsExistingShortUrl() {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");

        UrlMapping existing = buildMapping("https://www.example.com/path", "XPLEARN", 5L);
        when(repository.findByLongUrl("https://www.example.com/path")).thenReturn(Optional.of(existing));

        UrlResponse response = service.shorten(request);

        assertThat(response.getShortCode()).isEqualTo("XPLEARN");
        assertThat(response.getClickCount()).isEqualTo(5L);
        verify(repository, never()).save(any());
    }

    @Test
    void shorten_withCustomAlias_savesWithCustomCode() {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");
        request.setCustomAlias("myalias");

        when(repository.findByLongUrl(anyString())).thenReturn(Optional.empty());
        when(repository.existsByShortCode("myalias")).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse response = service.shorten(request);

        assertThat(response.getShortCode()).isEqualTo("myalias");
        ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getShortCode()).isEqualTo("myalias");
    }

    @Test
    void shorten_customAliasTaken_throwsAliasAlreadyExistsException() {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");
        request.setCustomAlias("taken");

        when(repository.findByLongUrl(anyString())).thenReturn(Optional.empty());
        when(repository.existsByShortCode("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.shorten(request))
                .isInstanceOf(AliasAlreadyExistsException.class)
                .hasMessageContaining("taken");
    }

    @Test
    void shorten_blankCustomAlias_generatesRandomCode() {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");
        request.setCustomAlias("   ");

        when(repository.findByLongUrl(anyString())).thenReturn(Optional.empty());
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse response = service.shorten(request);

        assertThat(response.getShortCode()).hasSize(7);
    }

    @Test
    void shorten_nullCustomAlias_generatesRandomCode() {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");
        request.setCustomAlias(null);

        when(repository.findByLongUrl(anyString())).thenReturn(Optional.empty());
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse response = service.shorten(request);

        assertThat(response.getShortCode()).hasSize(7);
    }

    @Test
    void shorten_codeCollision_retriesUntilUnique() {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");

        when(repository.findByLongUrl(anyString())).thenReturn(Optional.empty());
        // First 2 codes collide, 3rd is unique
        when(repository.existsByShortCode(anyString()))
                .thenReturn(true).thenReturn(true).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse response = service.shorten(request);

        assertThat(response.getShortCode()).isNotNull();
        verify(repository, times(3)).existsByShortCode(anyString());
    }

    @Test
    void shorten_shortUrlContainsBaseUrlAndCode() {
        UrlRequest request = new UrlRequest();
        request.setLongUrl("https://www.example.com/path");

        when(repository.findByLongUrl(anyString())).thenReturn(Optional.empty());
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse response = service.shorten(request);

        assertThat(response.getShortUrl())
                .startsWith("http://localhost:8080/")
                .endsWith(response.getShortCode());
    }

    // ─── redirect() ──────────────────────────────────────────────────────────

    @Test
    void redirect_validCode_returnsLongUrlAndIncrementsClickCount() {
        UrlMapping mapping = buildMapping("https://www.example.com/path", "XPLEARN", 3L);
        when(repository.findByShortCode("XPLEARN")).thenReturn(Optional.of(mapping));
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        String result = service.redirect("XPLEARN");

        assertThat(result).isEqualTo("https://www.example.com/path");
        assertThat(mapping.getClickCount()).isEqualTo(4L);
        verify(repository).save(mapping);
    }

    @Test
    void redirect_unknownCode_throwsUrlNotFoundException() {
        when(repository.findByShortCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redirect("UNKNOWN"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void redirect_firstVisit_clickCountBecomesOne() {
        UrlMapping mapping = buildMapping("https://www.example.com/path", "XPLEARN", 0L);
        when(repository.findByShortCode("XPLEARN")).thenReturn(Optional.of(mapping));
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        service.redirect("XPLEARN");

        assertThat(mapping.getClickCount()).isEqualTo(1L);
    }

    // ─── getStats() ──────────────────────────────────────────────────────────

    @Test
    void getStats_validCode_returnsUrlResponse() {
        UrlMapping mapping = buildMapping("https://www.example.com/path", "XPLEARN", 10L);
        when(repository.findByShortCode("XPLEARN")).thenReturn(Optional.of(mapping));

        UrlResponse response = service.getStats("XPLEARN");

        assertThat(response.getShortCode()).isEqualTo("XPLEARN");
        assertThat(response.getClickCount()).isEqualTo(10L);
        assertThat(response.getLongUrl()).isEqualTo("https://www.example.com/path");
    }

    @Test
    void getStats_unknownCode_throwsUrlNotFoundException() {
        when(repository.findByShortCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStats("UNKNOWN"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void getStats_doesNotIncrementClickCount() {
        UrlMapping mapping = buildMapping("https://www.example.com/path", "XPLEARN", 5L);
        when(repository.findByShortCode("XPLEARN")).thenReturn(Optional.of(mapping));

        service.getStats("XPLEARN");

        assertThat(mapping.getClickCount()).isEqualTo(5L);
        verify(repository, never()).save(any());
    }

    // ─── getAllUrls() ─────────────────────────────────────────────────────────

    @Test
    void getAllUrls_returnsMappedResponseList() {
        UrlMapping m1 = buildMapping("https://example.com", "AAAAAAA", 0L);
        UrlMapping m2 = buildMapping("https://google.com", "BBBBBBB", 3L);
        when(repository.findAll()).thenReturn(List.of(m1, m2));

        List<UrlResponse> result = service.getAllUrls();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getShortCode()).isEqualTo("AAAAAAA");
        assertThat(result.get(1).getShortCode()).isEqualTo("BBBBBBB");
    }

    @Test
    void getAllUrls_emptyRepository_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<UrlResponse> result = service.getAllUrls();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllUrls_responseContainsFullShortUrl() {
        UrlMapping m = buildMapping("https://example.com", "AAAAAAA", 0L);
        when(repository.findAll()).thenReturn(List.of(m));

        List<UrlResponse> result = service.getAllUrls();

        assertThat(result.get(0).getShortUrl()).isEqualTo("http://localhost:8080/AAAAAAA");
    }

    // ─── deleteUrl() ─────────────────────────────────────────────────────────

    @Test
    void deleteUrl_validCode_deletesMapping() {
        UrlMapping mapping = buildMapping("https://www.example.com/path", "XPLEARN", 0L);
        when(repository.findByShortCode("XPLEARN")).thenReturn(Optional.of(mapping));

        service.deleteUrl("XPLEARN");

        verify(repository).delete(mapping);
    }

    @Test
    void deleteUrl_unknownCode_throwsUrlNotFoundException() {
        when(repository.findByShortCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUrl("UNKNOWN"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("UNKNOWN");

        verify(repository, never()).delete(any());
    }
}
