package com.bookmyshow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "movies")
@Data
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(length = 2000)
    private String description;
    private Integer duration;
    private String language;
    private String genre;
    private LocalDate releaseDate;
    private String posterPath;
    private String trailerUrl;
    @Column(name = "movie_cast", length = 1000)
    private String cast;
    private String certification;
    private String status;
    @Column(unique = true)
    private Long tmdbId;
    /** When true, movie is part of the current TMDB now-playing catalogue and may appear to customers. */
    @Column(nullable = true)
    private Boolean catalogActive = false;
}
