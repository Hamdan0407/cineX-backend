package com.bookmyshow.service;



import com.bookmyshow.config.TmdbProperties;

import com.bookmyshow.entity.Movie;

import com.bookmyshow.repository.MovieRepository;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.cache.annotation.CacheEvict;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDate;

import java.util.ArrayList;

import java.util.HashSet;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.Set;



/**

 * Keeps the CineX movie inventory aligned with TMDB now-playing catalogue.

 * Legacy seeded titles (e.g. Interstellar) are deactivated and excluded from customer APIs.

 */

@Slf4j

@Service

@RequiredArgsConstructor

public class TmdbCatalogSyncService {



    private static final int DEFAULT_SYNC_LIMIT = 60;

    private final TmdbProperties tmdbProperties;
    private final MovieRepository movieRepository;
    private final TmdbNowPlayingService tmdbNowPlayingService;
    private final TmdbMovieDetailsService tmdbMovieDetailsService;



    @Value("${spring.profiles.active:}")

    private String activeProfiles;



    @Transactional

    @CacheEvict(value = {"tmdbNowPlaying", "tmdbTrending", "tmdbUpcoming"}, allEntries = true)

    public Map<Long, Movie> syncAndGetActiveMovies(int limit) {

        List<TmdbMovieSnapshot> snapshots = fetchNowPlayingSnapshots(limit);

        Map<Long, Movie> activeByTmdbId = new LinkedHashMap<>();

        Set<Long> activeIds = new HashSet<>();



        for (TmdbMovieSnapshot snapshot : snapshots) {

            Movie movie = movieRepository.findByTmdbId(snapshot.tmdbId())

                    .orElseGet(Movie::new);

            applySnapshot(movie, snapshot);

            movie.setCatalogActive(true);

            Movie saved = movieRepository.save(movie);

            activeByTmdbId.put(snapshot.tmdbId(), saved);

            activeIds.add(snapshot.tmdbId());

        }

        if (snapshots.isEmpty()) {
            log.warn("Skipping catalogue deactivation because TMDB returned no now-playing titles");
            movieRepository.findAll().stream()
                    .filter(movie -> Boolean.TRUE.equals(movie.getCatalogActive()) && movie.getTmdbId() != null)
                    .forEach(movie -> activeByTmdbId.put(movie.getTmdbId(), movie));
            return activeByTmdbId;
        }

        movieRepository.findAll().forEach(movie -> {

            if (movie.getTmdbId() == null && Boolean.TRUE.equals(movie.getCatalogActive())) {

                movie.setCatalogActive(false);

                movieRepository.save(movie);

                log.info("Deactivated legacy seeded movie without tmdbId: {}", movie.getTitle());

            } else if (movie.getTmdbId() != null && !activeIds.contains(movie.getTmdbId())

                    && Boolean.TRUE.equals(movie.getCatalogActive())) {

                movie.setCatalogActive(false);

                movieRepository.save(movie);

                log.info("Deactivated catalogue movie no longer in TMDB now playing: {} (tmdbId={})",

                        movie.getTitle(), movie.getTmdbId());

            }

        });



        log.info("TMDB catalogue sync complete: {} active movies", activeByTmdbId.size());

        return activeByTmdbId;

    }



    public Map<Long, Movie> syncAndGetActiveMovies() {
        return syncAndGetActiveMovies(DEFAULT_SYNC_LIMIT);
    }

    /**
     * Upserts a single movie from TMDB when a user opens a now-playing title that is not yet in CineX.
     */
    @Transactional
    public Movie syncMovieByTmdbId(long tmdbId) {
        return movieRepository.findByTmdbId(tmdbId)
                .map(existing -> {
                    if (!Boolean.TRUE.equals(existing.getCatalogActive())) {
                        existing.setCatalogActive(true);
                        existing.setStatus("NOW_SHOWING");
                        return movieRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    if (isTestCatalogMode()) {
                        return testCatalogFallback().stream()
                                .filter(snapshot -> snapshot.tmdbId() == tmdbId)
                                .findFirst()
                                .map(snapshot -> saveFromSnapshot(snapshot, true))
                                .orElse(null);
                    }
                    return tmdbMovieDetailsService.fetchMovieDetails(tmdbId)
                            .map(node -> saveFromSnapshot(toSnapshot(node), true))
                            .orElseGet(() -> tmdbNowPlayingService.fetchNowPlayingMovies().stream()
                                    .filter(node -> node.has("id") && node.get("id").asLong() == tmdbId)
                                    .findFirst()
                                    .map(node -> saveFromSnapshot(toSnapshot(node), true))
                                    .orElse(null));
                });
    }

    private Movie saveFromSnapshot(TmdbMovieSnapshot snapshot, boolean active) {
        Movie movie = movieRepository.findByTmdbId(snapshot.tmdbId()).orElseGet(Movie::new);
        applySnapshot(movie, snapshot);
        movie.setCatalogActive(active);
        return movieRepository.save(movie);
    }

    private void applySnapshot(Movie movie, TmdbMovieSnapshot snapshot) {

        movie.setTmdbId(snapshot.tmdbId());

        movie.setTitle(snapshot.title());

        movie.setDescription(snapshot.overview());

        movie.setPosterPath(snapshot.posterPath());

        movie.setDuration(snapshot.runtimeMinutes());

        movie.setGenre(snapshot.genre());

        movie.setLanguage(snapshot.originalLanguage() != null ? snapshot.originalLanguage() : "Multilingual");

        if (snapshot.releaseDate() != null) {

            movie.setReleaseDate(snapshot.releaseDate());

        } else if (movie.getReleaseDate() == null) {

            movie.setReleaseDate(LocalDate.now());

        }

        movie.setStatus("NOW_SHOWING");

    }



    private List<TmdbMovieSnapshot> fetchNowPlayingSnapshots(int limit) {

        if (isTestCatalogMode()) {

            log.info("Using embedded TMDB catalogue fallback (test profile / test API key)");

            return testCatalogFallback();

        }



        List<JsonNode> discovered = tmdbNowPlayingService.fetchNowPlayingMovies();

        if (discovered.isEmpty()) {

            log.error("TMDB now_playing returned empty — no catalogue fallback in production");

            return List.of();

        }



        List<TmdbMovieSnapshot> snapshots = new ArrayList<>();

        for (JsonNode node : discovered) {

            if (snapshots.size() >= limit || node == null || !node.has("id")) {

                continue;

            }

            snapshots.add(toSnapshot(node));

        }

        return snapshots;

    }



    private boolean isTestCatalogMode() {

        String apiKey = tmdbProperties.getApiKey();

        return apiKey == null || apiKey.isBlank() || "test-key".equals(apiKey)

                || activeProfiles.contains("test");

    }



    private TmdbMovieSnapshot toSnapshot(JsonNode node) {

        String overview = node.has("overview") ? node.get("overview").asText("") : "";

        String poster = node.has("poster_path") && !node.get("poster_path").isNull()

                ? node.get("poster_path").asText() : null;

        Integer runtime = node.has("runtime") && !node.get("runtime").isNull()

                ? node.get("runtime").asInt() : null;

        LocalDate releaseDate = null;

        if (node.has("release_date") && !node.get("release_date").asText("").isBlank()) {

            try {

                releaseDate = LocalDate.parse(node.get("release_date").asText());

            } catch (Exception ignored) {

                // keep null

            }

        }

        String genre = "Cinema";

        if (node.has("genre_ids") && node.get("genre_ids").isArray() && !node.get("genre_ids").isEmpty()) {

            genre = "Feature";

        }

        String originalLanguage = node.has("original_language") && !node.get("original_language").isNull()

                ? node.get("original_language").asText() : null;

        return new TmdbMovieSnapshot(

                node.get("id").asLong(),

                node.has("title") ? node.get("title").asText("Untitled")

                        : node.get("original_title").asText("Untitled"),

                overview,

                poster,

                runtime,

                genre,

                releaseDate,

                originalLanguage

        );

    }



    /** Embedded catalogue for automated tests only — not used in production. */

    List<TmdbMovieSnapshot> testCatalogFallback() {

        return List.of(

                snap(677179L, "Jawan", "A high-octane action thriller.", "/lqYpWXnOhFCvatIWhmgkeqjfbo.jpg", 169, "Action", "hi"),

                snap(872585L, "Oppenheimer", "The story of the atomic bomb.", "/8Gxv8gSFCU0XGDykEGv7zRWRnxx.jpg", 180, "Drama", "en"),

                snap(653346L, "Dune: Part Two", "Paul Atreides unites with Chani.", "/1bhkWxHGKk36aNDswXPQz3mv941.jpg", 166, "Sci-Fi", "en"),

                snap(786892L, "Kalki 2898 AD", "A futuristic Indian epic.", "/y3YJavMNBHmDBNs0hR7dtyakd6b.jpg", 181, "Sci-Fi", "te"),

                snap(1011985L, "Kung Fu Panda 4", "Po must train a new warrior.", "/kDp1vUBnNemEKSWqrOynr3ou3lF.jpg", 94, "Animation", "en"),

                snap(945961L, "Alien: Romulus", "A new chapter in the Alien saga.", "/5aj8vZgTYlhZ7bcv2JKcJxGWXsP.jpg", 119, "Sci-Fi", "en")

        );

    }



    private TmdbMovieSnapshot snap(Long tmdbId, String title, String overview, String poster,

                                   int runtime, String genre, String originalLanguage) {

        return new TmdbMovieSnapshot(tmdbId, title, overview, poster, runtime, genre,

                LocalDate.now().minusWeeks(2), originalLanguage);

    }



    record TmdbMovieSnapshot(

            Long tmdbId,

            String title,

            String overview,

            String posterPath,

            Integer runtimeMinutes,

            String genre,

            LocalDate releaseDate,

            String originalLanguage

    ) {}

}

