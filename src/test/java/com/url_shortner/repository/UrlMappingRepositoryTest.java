package com.url_shortner.repository;

import com.url_shortner.model.UrlMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UrlMappingRepositoryTest {

    @Autowired
    private UrlMappingRepository repository;

    private UrlMapping saved;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl("https://www.example.com/path");
        mapping.setShortCode("XPLEARN");
        saved = repository.save(mapping);
    }

    // ─── findByShortCode() ────────────────────────────────────────────────────

    @Test
    void findByShortCode_existingCode_returnsMapping() {
        Optional<UrlMapping> result = repository.findByShortCode("XPLEARN");

        assertThat(result).isPresent();
        assertThat(result.get().getLongUrl()).isEqualTo("https://www.example.com/path");
    }

    @Test
    void findByShortCode_unknownCode_returnsEmpty() {
        Optional<UrlMapping> result = repository.findByShortCode("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void findByShortCode_isCaseInsensitive() {
        Optional<UrlMapping> result = repository.findByShortCode("xplearn");

        assertThat(result).isPresent();
        assertThat(result.get().getShortCode()).isEqualTo("XPLEARN");
    }

    // ─── findByLongUrl() ─────────────────────────────────────────────────────

    @Test
    void findByLongUrl_existingUrl_returnsMapping() {
        Optional<UrlMapping> result = repository.findByLongUrl("https://www.example.com/path");

        assertThat(result).isPresent();
        assertThat(result.get().getShortCode()).isEqualTo("XPLEARN");
    }

    @Test
    void findByLongUrl_unknownUrl_returnsEmpty() {
        Optional<UrlMapping> result = repository.findByLongUrl("https://unknown.com");

        assertThat(result).isEmpty();
    }

    // ─── existsByShortCode() ─────────────────────────────────────────────────

    @Test
    void existsByShortCode_existingCode_returnsTrue() {
        assertThat(repository.existsByShortCode("XPLEARN")).isTrue();
    }

    @Test
    void existsByShortCode_unknownCode_returnsFalse() {
        assertThat(repository.existsByShortCode("NOTEXIST")).isFalse();
    }

    // ─── save() & findAll() ───────────────────────────────────────────────────

    @Test
    void save_persistsAllFields() {
        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl("https://www.google.com");
        mapping.setShortCode("GGOOGLE");
        mapping.setClickCount(10L);

        UrlMapping result = repository.save(mapping);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getLongUrl()).isEqualTo("https://www.google.com");
        assertThat(result.getShortCode()).isEqualTo("GGOOGLE");
        assertThat(result.getClickCount()).isEqualTo(10L);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAll_returnsAllMappings() {
        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl("https://www.google.com");
        mapping.setShortCode("GGOOGLE");
        repository.save(mapping);

        List<UrlMapping> all = repository.findAll();

        assertThat(all).hasSize(2);
    }

    // ─── delete() ─────────────────────────────────────────────────────────────

    @Test
    void delete_removesMapping() {
        repository.delete(saved);

        assertThat(repository.findByShortCode("XPLEARN")).isEmpty();
    }

    @Test
    void delete_onlyRemovesTargetMapping() {
        UrlMapping other = new UrlMapping();
        other.setLongUrl("https://www.google.com");
        other.setShortCode("GGOOGLE");
        repository.save(other);

        repository.delete(saved);

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findByShortCode("GGOOGLE")).isPresent();
    }

    // ─── unique constraint ────────────────────────────────────────────────────

    @Test
    void save_duplicateShortCode_throwsException() {
        UrlMapping duplicate = new UrlMapping();
        duplicate.setLongUrl("https://www.another.com");
        duplicate.setShortCode("XPLEARN");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }

    // ─── clickCount update ────────────────────────────────────────────────────

    @Test
    void save_updatesClickCount() {
        saved.setClickCount(saved.getClickCount() + 1);
        repository.save(saved);

        Optional<UrlMapping> updated = repository.findByShortCode("XPLEARN");

        assertThat(updated).isPresent();
        assertThat(updated.get().getClickCount()).isEqualTo(1L);
    }
}
