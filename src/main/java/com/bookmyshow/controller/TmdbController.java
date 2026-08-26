package com.bookmyshow.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.bookmyshow.config.TmdbProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tmdb")
public class TmdbController {

    private static final int MAX_RETRIES = 2;

    private final TmdbProperties tmdbProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TmdbController(TmdbProperties tmdbProperties) {
        this.tmdbProperties = tmdbProperties;
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "CineX/1.0")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Cacheable(value = "tmdbNowPlaying", key = "'all'")
    @GetMapping("/now_playing")
    public ResponseEntity<String> getNowPlaying() {
        log.info("Cache Miss: Fetching TMDB now playing");
        List<JsonNode> all = new ArrayList<>();
        for (String lang : tmdbProperties.getLanguages()) {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(tmdbProperties.getBaseUrl() + "/discover/movie")
                    .queryParam("api_key", getRequiredApiKey())
                    .queryParam("with_original_language", lang)
                    .queryParam("sort_by", "popularity.desc");

            if (!"ta".equals(lang)) {
                builder.queryParam("region", "IN");
                builder.queryParam("with_release_type", "2|3");
            } else {
                builder.queryParam("primary_release_date.gte", "2023-01-01");
            }

            all.addAll(fetchResults(builder.build().toUriString(), "now_playing", lang));
        }
        return buildMergedResponse(all, "now_playing");
    }

    @Cacheable(value = "tmdbTrending", key = "'all'")
    @GetMapping("/trending")
    public ResponseEntity<String> getTrending() {
        log.info("Cache Miss: Fetching TMDB trending");
        List<JsonNode> all = new ArrayList<>();
        for (String lang : tmdbProperties.getLanguages()) {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(tmdbProperties.getBaseUrl() + "/discover/movie")
                    .queryParam("api_key", getRequiredApiKey())
                    .queryParam("with_original_language", lang)
                    .queryParam("sort_by", "popularity.desc")
                    .queryParam("primary_release_date.gte", "2023-06-01");

            if (!"ta".equals(lang)) {
                builder.queryParam("region", "IN");
            }

            all.addAll(fetchResults(builder.build().toUriString(), "trending", lang));
        }
        return buildMergedResponse(all, "trending");
    }

    @Cacheable(value = "tmdbUpcoming", key = "'all'")
    @GetMapping("/upcoming")
    public ResponseEntity<String> getUpcoming() {
        log.info("Cache Miss: Fetching TMDB upcoming");
        List<JsonNode> all = new ArrayList<>();
        for (String lang : tmdbProperties.getLanguages()) {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(tmdbProperties.getBaseUrl() + "/discover/movie")
                    .queryParam("api_key", getRequiredApiKey())
                    .queryParam("with_original_language", lang)
                    .queryParam("sort_by", "popularity.desc")
                    .queryParam("primary_release_date.gte", LocalDate.now().toString());

            if (!"ta".equals(lang)) {
                builder.queryParam("region", "IN");
            }

            all.addAll(fetchResults(builder.build().toUriString(), "upcoming", lang));
        }
        return buildMergedResponse(all, "upcoming");
    }

    private String getRequiredApiKey() {
        String apiKey = tmdbProperties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB API key is not configured");
        }
        return apiKey;
    }

    private List<JsonNode> fetchResults(String url, String category, String lang) {
        String tag = category + "[" + lang + "]";
        List<JsonNode> results = new ArrayList<>();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("TMDB [{}] attempt {}", tag, attempt);
                String body = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);
                JsonNode root = objectMapper.readTree(body);
                if (root.has("results") && root.get("results").isArray()) {
                    root.get("results").forEach(results::add);
                }
                if (!results.isEmpty()) {
                    return results;
                }
            } catch (RestClientException ex) {
                log.warn("TMDB [{}] attempt {} failed: {}", tag, attempt, ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(300L * attempt);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception ex) {
                log.error("TMDB [{}] parse error: {}", tag, ex.getMessage());
                break;
            }
        }

        log.warn("TMDB [{}] returned empty after retries", tag);
        return results;
    }

    private ResponseEntity<String> buildMergedResponse(List<JsonNode> all, String category) {
        if (all.isEmpty()) {
            log.warn("TMDB API returned empty for category {}", category);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to fetch movies from TMDB right now");
        }

        Set<Long> seen = new HashSet<>();
        List<JsonNode> deduped = new ArrayList<>();
        for (JsonNode movie : all) {
            if (movie != null && movie.has("id")) {
                long id = movie.get("id").asLong();
                if (seen.add(id)) {
                    deduped.add(movie);
                }
            }
        }

        deduped.sort(Comparator.comparingDouble(
                (JsonNode movie) -> movie.has("popularity") ? movie.get("popularity").asDouble() : 0.0
        ).reversed());

        ObjectNode envelope = objectMapper.createObjectNode();
        ArrayNode results = objectMapper.createArrayNode();
        deduped.forEach(results::add);
        envelope.set("results", results);
        envelope.put("total_results", deduped.size());

        try {
            return ResponseEntity.ok(objectMapper.writeValueAsString(envelope));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Serialization failed\"}");
        }
    }
}
