package com.bookmyshow.service;

import com.bookmyshow.config.TmdbProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fetches TMDB's official Now Playing catalogue for India (region=IN).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbNowPlayingService {

    public static final String REGION = "IN";
    public static final String LANGUAGE = "en-US";
    private static final int MAX_RETRIES = 2;
    private static final int DEFAULT_MAX_PAGES = 3;

    private final TmdbProperties tmdbProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder()
            .defaultHeader("User-Agent", "CineX/1.0")
            .defaultHeader("Accept", "application/json")
            .build();

    public List<JsonNode> fetchNowPlayingMovies() {
        return fetchNowPlayingMovies(DEFAULT_MAX_PAGES);
    }

    public List<JsonNode> fetchNowPlayingMovies(int maxPages) {
        String apiKey = tmdbProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("TMDB API key is not configured");
            return List.of();
        }

        List<JsonNode> collected = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (int page = 1; page <= Math.max(1, maxPages); page++) {
            List<JsonNode> pageResults = fetchPage(apiKey, page);
            if (pageResults.isEmpty()) {
                break;
            }
            for (JsonNode movie : pageResults) {
                if (movie != null && movie.has("id")) {
                    long id = movie.get("id").asLong();
                    if (seen.add(id)) {
                        collected.add(movie);
                    }
                }
            }
        }

        collected.sort(Comparator.comparingDouble(
                (JsonNode movie) -> movie.has("popularity") ? movie.get("popularity").asDouble() : 0.0
        ).reversed());

        log.info("TMDB now_playing region={} returned {} movies", REGION, collected.size());
        return collected;
    }

    private List<JsonNode> fetchPage(String apiKey, int page) {
        String url = UriComponentsBuilder
                .fromUriString(tmdbProperties.getBaseUrl() + "/movie/now_playing")
                .queryParam("api_key", apiKey)
                .queryParam("region", REGION)
                .queryParam("language", LANGUAGE)
                .queryParam("page", page)
                .build()
                .toUriString();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String body = restClient.get().uri(url).retrieve().body(String.class);
                JsonNode root = objectMapper.readTree(body);
                if (root.has("results") && root.get("results").isArray()) {
                    List<JsonNode> results = new ArrayList<>();
                    root.get("results").forEach(results::add);
                    return results;
                }
                return List.of();
            } catch (RestClientException ex) {
                log.warn("TMDB now_playing page {} attempt {} failed: {}", page, attempt, ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepQuietly(300L * attempt);
                }
            } catch (Exception ex) {
                log.error("TMDB now_playing page {} parse error: {}", page, ex.getMessage());
                return List.of();
            }
        }
        return List.of();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
