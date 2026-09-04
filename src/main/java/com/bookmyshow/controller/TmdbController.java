package com.bookmyshow.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.bookmyshow.config.TmdbProperties;
import com.bookmyshow.service.TmdbMovieDetailsService;
import com.bookmyshow.service.TmdbNowPlayingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tmdb")
@RequiredArgsConstructor
public class TmdbController {

    private static final int MAX_RETRIES = 2;

    private final TmdbProperties tmdbProperties;
    private final TmdbNowPlayingService tmdbNowPlayingService;
    private final TmdbMovieDetailsService tmdbMovieDetailsService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.builder()
            .defaultHeader("User-Agent", "CineX/1.0")
            .defaultHeader("Accept", "application/json")
            .build();

    @Cacheable(value = "tmdbNowPlaying", key = "'all'")
    @GetMapping(value = "/now_playing", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getNowPlaying() {
        log.info("Cache Miss: Fetching TMDB now playing (region={}, language={})",
                TmdbNowPlayingService.REGION, TmdbNowPlayingService.LANGUAGE);
        getRequiredApiKey();
        List<JsonNode> movies = tmdbNowPlayingService.fetchNowPlayingMovies();
        if (movies.isEmpty()) {
            log.warn("TMDB now_playing returned empty for region {}", TmdbNowPlayingService.REGION);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to fetch movies from TMDB right now");
        }
        ObjectNode envelope = objectMapper.createObjectNode();
        ArrayNode results = objectMapper.createArrayNode();
        movies.forEach(results::add);
        envelope.set("results", results);
        envelope.put("total_results", movies.size());
        envelope.put("region", TmdbNowPlayingService.REGION);
        envelope.put("language", TmdbNowPlayingService.LANGUAGE);
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception ex) {
            log.error("TMDB now_playing serialization failed", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred. Please try again later.");
        }
    }

    @GetMapping(value = "/movie/{tmdbId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getMovieDetails(@PathVariable long tmdbId) {
        return tmdbMovieDetailsService.fetchMovieDetails(tmdbId)
                .map(this::serializeJsonNode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Movie not found in TMDB"));
    }

    @GetMapping(value = "/movie/{tmdbId}/credits", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getMovieCredits(@PathVariable long tmdbId) {
        return tmdbMovieDetailsService.fetchMovieCredits(tmdbId)
                .map(this::serializeJsonNode)
                .orElseGet(() -> "{\"id\":" + tmdbId + ",\"cast\":[],\"crew\":[]}");
    }

    @GetMapping(value = "/movie/{tmdbId}/similar", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getSimilarMovies(@PathVariable long tmdbId) {
        List<JsonNode> similar = tmdbMovieDetailsService.fetchSimilarOrRecommendedMovies(tmdbId);
        ObjectNode envelope = objectMapper.createObjectNode();
        ArrayNode results = objectMapper.createArrayNode();
        similar.forEach(results::add);
        envelope.set("results", results);
        envelope.put("total_results", similar.size());
        return serializeJsonNode(envelope);
    }

    @Cacheable(value = "tmdbTrending", key = "'all'")
    @GetMapping(value = "/trending", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getTrending() {
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
    @GetMapping(value = "/upcoming", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getUpcoming() {
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

    private String serializeJsonNode(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred. Please try again later.");
        }
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

    private String buildMergedResponse(List<JsonNode> all, String category) {
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
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception ex) {
            log.error("TMDB [{}] serialization failed", category, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred. Please try again later.");
        }
    }
}
