package com.bookmyshow.repository;

import com.bookmyshow.entity.Show;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByMovieId(Long movieId);
    List<Show> findByScreenId(Long screenId);
    Page<Show> findByMovieId(Long movieId, Pageable pageable);

    boolean existsByMovieIdAndScreenIdAndShowDateAndShowTimeAndScreeningLanguage(
            Long movieId, Long screenId, LocalDate showDate, LocalTime showTime, String screeningLanguage);

    @Query("""
            SELECT s FROM Show s
            JOIN FETCH s.screen sc
            JOIN FETCH sc.theatre t
            JOIN FETCH s.movie m
            WHERE m.id = :movieId
            AND t.city = :city
            AND s.showDate >= :fromDate
            AND s.movie.catalogActive = TRUE
            AND (:language IS NULL OR s.screeningLanguage = :language)
            ORDER BY s.showDate ASC, s.showTime ASC
            """)
    List<Show> findByMovieIdAndCity(
            @Param("movieId") Long movieId,
            @Param("city") String city,
            @Param("fromDate") LocalDate fromDate,
            @Param("language") String language);

    @Query("""
            SELECT s FROM Show s
            JOIN FETCH s.screen sc
            JOIN FETCH sc.theatre t
            JOIN FETCH s.movie m
            WHERE m.tmdbId = :tmdbId
            AND t.city = :city
            AND s.showDate >= :fromDate
            AND s.movie.catalogActive = TRUE
            AND (:language IS NULL OR s.screeningLanguage = :language)
            ORDER BY s.showDate ASC, s.showTime ASC
            """)
    List<Show> findByTmdbIdAndCity(
            @Param("tmdbId") Long tmdbId,
            @Param("city") String city,
            @Param("fromDate") LocalDate fromDate,
            @Param("language") String language);

    @Query("""
            SELECT DISTINCT m.tmdbId FROM Show s
            JOIN s.movie m
            JOIN s.screen sc
            JOIN sc.theatre t
            WHERE m.tmdbId IS NOT NULL
            AND m.catalogActive = TRUE
            AND t.city = :city
            AND s.showDate >= :fromDate
            AND (:language IS NULL OR s.screeningLanguage = :language)
            """)
    List<Long> findDistinctTmdbIdsByCity(
            @Param("city") String city,
            @Param("fromDate") LocalDate fromDate,
            @Param("language") String language);

    @Query("""
            SELECT DISTINCT s.screeningLanguage FROM Show s
            JOIN s.screen sc
            JOIN sc.theatre t
            WHERE t.city = :city
            AND s.showDate >= :fromDate
            AND s.movie.catalogActive = TRUE
            AND s.screeningLanguage IS NOT NULL
            ORDER BY s.screeningLanguage ASC
            """)
    List<String> findDistinctScreeningLanguagesByCity(
            @Param("city") String city,
            @Param("fromDate") LocalDate fromDate);

    @Query("""
            SELECT DISTINCT s.movie FROM Show s
            JOIN s.screen sc
            JOIN sc.theatre t
            WHERE t.city = :city
            AND s.showDate >= :fromDate
            AND s.movie.catalogActive = TRUE
            AND (:language IS NULL OR s.screeningLanguage = :language)
            ORDER BY s.movie.title ASC
            """)
    List<com.bookmyshow.entity.Movie> findDistinctMoviesByCity(
            @Param("city") String city,
            @Param("fromDate") LocalDate fromDate,
            @Param("language") String language);
}
