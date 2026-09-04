package com.bookmyshow.service;

import com.bookmyshow.entity.Movie;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Show;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Ensures CineX show inventory exists for catalog movies across operational cities.
 * This is CineX booking data — not TMDB theatre/showtime data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShowInventoryService {

    static final List<LocalTime> SHOW_TIMES = List.of(
            LocalTime.of(10, 0),
            LocalTime.of(13, 30),
            LocalTime.of(17, 0),
            LocalTime.of(21, 0)
    );

    static final List<String> SCREENING_LANGUAGES = List.of("English", "Hindi", "Tamil");

    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;

    @Transactional
    public void ensureShowsForCatalog(Map<Long, Movie> activeMovies, Map<String, List<Screen>> cityScreens) {
        if (activeMovies == null || activeMovies.isEmpty()) {
            return;
        }
        List<Movie> movies = activeMovies.values().stream()
                .filter(movie -> Boolean.TRUE.equals(movie.getCatalogActive()))
                .filter(movie -> movie.getTmdbId() != null)
                .toList();

        for (Map.Entry<String, List<Screen>> entry : cityScreens.entrySet()) {
            String city = entry.getKey();
            List<Screen> screens = entry.getValue();
            if (screens.isEmpty()) {
                continue;
            }
            for (int movieIndex = 0; movieIndex < movies.size(); movieIndex++) {
                Movie movie = movies.get(movieIndex);
                Screen screen = screens.get(movieIndex % screens.size());
                ensureShowsForMovieOnScreen(movie, screen, city);
            }
        }
    }

    @Transactional
    public void ensureShowsForTmdbMovieInCity(Movie movie, String city) {
        if (movie == null || movie.getTmdbId() == null || city == null || city.isBlank()) {
            return;
        }
        List<Screen> screens = screenRepository.findAll().stream()
                .filter(screen -> screen.getTheatre() != null
                        && city.equalsIgnoreCase(screen.getTheatre().getCity()))
                .toList();
        if (screens.isEmpty()) {
            log.debug("No screens found for city {} when ensuring shows for tmdbId={}", city, movie.getTmdbId());
            return;
        }

        int startIndex = Math.floorMod(movie.getTmdbId().hashCode(), screens.size());
        int theatresToCover = Math.min(3, screens.size());
        for (int offset = 0; offset < theatresToCover; offset++) {
            Screen screen = screens.get((startIndex + offset) % screens.size());
            ensureShowsForMovieOnScreen(movie, screen, city);
        }
    }

    private void ensureShowsForMovieOnScreen(Movie movie, Screen screen, String city) {
        Screen resolvedScreen = screenRepository.findById(screen.getId()).orElse(screen);
        Theatre theatre = resolvedScreen.getTheatre();
        if (theatre == null || theatre.getCity() == null
                || !theatre.getCity().equalsIgnoreCase(city)) {
            return;
        }

        LocalDate today = LocalDate.now();
        int seatCount = resolvedScreen.getTotalSeats() != null && resolvedScreen.getTotalSeats() > 0
                ? resolvedScreen.getTotalSeats() : 96;

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate showDate = today.plusDays(dayOffset);
            for (int timeIndex = 0; timeIndex < SHOW_TIMES.size(); timeIndex++) {
                LocalTime showTime = SHOW_TIMES.get(timeIndex);
                String language = SCREENING_LANGUAGES.get(
                        (Math.floorMod(movie.getTmdbId().intValue(), SCREENING_LANGUAGES.size())
                                + timeIndex + dayOffset) % SCREENING_LANGUAGES.size());

                if (showRepository.existsByMovieIdAndScreenIdAndShowDateAndShowTimeAndScreeningLanguage(
                        movie.getId(), resolvedScreen.getId(), showDate, showTime, language)) {
                    continue;
                }

                Show show = new Show();
                show.setMovie(movie);
                show.setScreen(resolvedScreen);
                show.setShowDate(showDate);
                show.setShowTime(showTime);
                show.setPrice(250.0 + (timeIndex * 50.0));
                show.setScreeningLanguage(language);
                show.setAvailableSeats(seatCount);
                showRepository.save(show);
            }
        }
    }
}
