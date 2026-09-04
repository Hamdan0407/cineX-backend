package com.bookmyshow.service;

import com.bookmyshow.config.TmdbProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbMovieDetailsService {

    private static final int DEFAULT_SIMILAR_LIMIT = 12;
    private static final int MAX_RETRIES = 2;

    private final TmdbProperties tmdbProperties;
    private final ObjectMapper objectMapper;
    private final TmdbNowPlayingService tmdbNowPlayingService;
    private final RestClient restClient = RestClient.builder()
            .defaultHeader("User-Agent", "CineX/1.0")
            .defaultHeader("Accept", "application/json")
            .build();

    public Optional<JsonNode> fetchMovieDetails(long tmdbId) {
        if (!hasRealApiKey()) {
            return findInNowPlaying(tmdbId);
        }

        String url = UriComponentsBuilder
                .fromUriString(tmdbProperties.getBaseUrl() + "/movie/" + tmdbId)
                .queryParam("api_key", tmdbProperties.getApiKey())
                .queryParam("language", TmdbNowPlayingService.LANGUAGE)
                .build()
                .toUriString();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String body = restClient.get()
                        .uri(url)
                        .retrieve()
                        .onStatus(status -> status.isError(), (request, response) -> {
                            throw new IllegalStateException("TMDB details HTTP " + response.getStatusCode());
                        })
                        .body(String.class);
                JsonNode node = objectMapper.readTree(body);
                if (node != null && node.has("id") && node.get("id").asLong() == tmdbId) {
                    return Optional.of(node);
                }
                return Optional.empty();
            } catch (RestClientException ex) {
                log.warn("TMDB details id {} attempt {} failed: {}", tmdbId, attempt, ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepQuietly(300L * attempt);
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch TMDB movie details for id {}: {}", tmdbId, ex.getMessage());
                break;
            }
        }
        return findInNowPlaying(tmdbId);
    }

    public Optional<JsonNode> fetchMovieCredits(long tmdbId) {
        if (!hasRealApiKey()) {
            return Optional.empty();
        }

        String url = UriComponentsBuilder
                .fromUriString(tmdbProperties.getBaseUrl() + "/movie/" + tmdbId + "/credits")
                .queryParam("api_key", tmdbProperties.getApiKey())
                .queryParam("language", TmdbNowPlayingService.LANGUAGE)
                .build()
                .toUriString();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String body = restClient.get()
                        .uri(url)
                        .retrieve()
                        .onStatus(status -> status.isError(), (request, response) -> {
                            throw new IllegalStateException("TMDB credits HTTP " + response.getStatusCode());
                        })
                        .body(String.class);
                JsonNode node = objectMapper.readTree(body);
                if (node != null && (node.has("cast") || node.has("crew"))) {
                    return Optional.of(node);
                }
                return Optional.empty();
            } catch (RestClientException ex) {
                log.warn("TMDB credits id {} attempt {} failed: {}", tmdbId, attempt, ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepQuietly(300L * attempt);
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch TMDB credits for id {}: {}", tmdbId, ex.getMessage());
                break;
            }
        }
        return Optional.empty();
    }

    public List<JsonNode> fetchSimilarOrRecommendedMovies(long tmdbId) {
        return fetchSimilarOrRecommendedMovies(tmdbId, DEFAULT_SIMILAR_LIMIT);
    }

    public List<JsonNode> fetchSimilarOrRecommendedMovies(long tmdbId, int limit) {
        if (!hasRealApiKey() || limit <= 0) {
            return Collections.emptyList();
        }

        List<JsonNode> similar = fetchMovieListFromEndpoint(tmdbId, "similar", limit);
        if (!similar.isEmpty()) {
            return similar;
        }
        return fetchMovieListFromEndpoint(tmdbId, "recommendations", limit);
    }

    private List<JsonNode> fetchMovieListFromEndpoint(long tmdbId, String endpoint, int limit) {
        String url = UriComponentsBuilder
                .fromUriString(tmdbProperties.getBaseUrl() + "/movie/" + tmdbId + "/" + endpoint)
                .queryParam("api_key", tmdbProperties.getApiKey())
                .queryParam("language", TmdbNowPlayingService.LANGUAGE)
                .queryParam("page", 1)
                .build()
                .toUriString();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String body = restClient.get()
                        .uri(url)
                        .retrieve()
                        .onStatus(status -> status.isError(), (request, response) -> {
                            throw new IllegalStateException("TMDB " + endpoint + " HTTP " + response.getStatusCode());
                        })
                        .body(String.class);
                JsonNode root = objectMapper.readTree(body);
                if (root == null || !root.has("results") || !root.get("results").isArray()) {
                    return Collections.emptyList();
                }
                List<JsonNode> results = new ArrayList<>();
                root.get("results").forEach(results::add);
                return results.size() <= limit ? results : results.subList(0, limit);
            } catch (RestClientException ex) {
                log.warn("TMDB {} id {} attempt {} failed: {}", endpoint, tmdbId, attempt, ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepQuietly(300L * attempt);
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch TMDB {} for id {}: {}", endpoint, tmdbId, ex.getMessage());
                break;
            }
        }
        return Collections.emptyList();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean hasRealApiKey() {
        String apiKey = tmdbProperties.getApiKey();
        return apiKey != null && !apiKey.isBlank() && !"test-key".equals(apiKey);
    }

    private Optional<JsonNode> findInNowPlaying(long tmdbId) {
        return tmdbNowPlayingService.fetchNowPlayingMovies().stream()
                .filter(node -> node != null && node.has("id") && node.get("id").asLong() == tmdbId)
                .findFirst();
    }
}
