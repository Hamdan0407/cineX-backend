package com.bookmyshow.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MovieDto {
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private String language;
    private String genre;
    private LocalDate releaseDate;
    private String posterPath;
    private String trailerUrl;
    private String cast;
    private String certification;
    private String status;
}
