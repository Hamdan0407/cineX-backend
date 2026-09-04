package com.bookmyshow.service;

import com.bookmyshow.entity.Movie;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowInventoryServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private ScreenRepository screenRepository;

    @InjectMocks
    private ShowInventoryService showInventoryService;

    private Movie movie;
    private Screen screen;

    @BeforeEach
    void setUp() {
        Theatre theatre = new Theatre();
        theatre.setId(1L);
        theatre.setCity("Chennai");
        theatre.setName("PVR Sathyam");

        screen = new Screen();
        screen.setId(10L);
        screen.setTheatre(theatre);
        screen.setScreenName("Dolby Atmos Main");
        screen.setTotalSeats(96);

        movie = new Movie();
        movie.setId(5L);
        movie.setTmdbId(969681L);
        movie.setTitle("Spider-Man: Brand New Day");
        movie.setCatalogActive(true);
    }

    @Test
    @DisplayName("ensureShowsForTmdbMovieInCity creates idempotent show rows for a city screen")
    void ensureShowsForTmdbMovieInCityCreatesShows() {
        when(screenRepository.findAll()).thenReturn(List.of(screen));
        when(screenRepository.findById(10L)).thenReturn(Optional.of(screen));
        when(showRepository.existsByMovieIdAndScreenIdAndShowDateAndShowTimeAndScreeningLanguage(
                anyLong(), anyLong(), any(LocalDate.class), any(LocalTime.class), anyString()))
                .thenReturn(false);

        showInventoryService.ensureShowsForTmdbMovieInCity(movie, "Chennai");

        verify(showRepository, atLeast(ShowInventoryService.SHOW_TIMES.size())).save(any());
    }

    @Test
    @DisplayName("ensureShowsForTmdbMovieInCity skips when city has no screens")
    void ensureShowsForTmdbMovieInCitySkipsWithoutScreens() {
        when(screenRepository.findAll()).thenReturn(List.of());

        showInventoryService.ensureShowsForTmdbMovieInCity(movie, "Chennai");

        verify(showRepository, never()).save(any());
    }

    @Test
    @DisplayName("ensureShowsForTmdbMovieInCity covers multiple screens in the same city")
    void ensureShowsForTmdbMovieInCityUsesMultipleScreens() {
        Theatre theatre2 = new Theatre();
        theatre2.setId(2L);
        theatre2.setCity("Chennai");
        theatre2.setName("PVR Phoenix");

        Screen screen2 = new Screen();
        screen2.setId(11L);
        screen2.setTheatre(theatre2);
        screen2.setScreenName("IMAX");
        screen2.setTotalSeats(120);

        Theatre theatre3 = new Theatre();
        theatre3.setId(3L);
        theatre3.setCity("Chennai");
        theatre3.setName("AGS T Nagar");

        Screen screen3 = new Screen();
        screen3.setId(12L);
        screen3.setTheatre(theatre3);
        screen3.setScreenName("Screen 1");
        screen3.setTotalSeats(100);

        when(screenRepository.findAll()).thenReturn(List.of(screen, screen2, screen3));
        when(screenRepository.findById(anyLong())).thenAnswer(invocation -> {
            long id = invocation.getArgument(0);
            if (id == 10L) return Optional.of(screen);
            if (id == 11L) return Optional.of(screen2);
            return Optional.of(screen3);
        });
        when(showRepository.existsByMovieIdAndScreenIdAndShowDateAndShowTimeAndScreeningLanguage(
                anyLong(), anyLong(), any(LocalDate.class), any(LocalTime.class), anyString()))
                .thenReturn(false);

        showInventoryService.ensureShowsForTmdbMovieInCity(movie, "Chennai");

        ArgumentCaptor<Long> screenIds = ArgumentCaptor.forClass(Long.class);
        verify(showRepository, atLeast(ShowInventoryService.SHOW_TIMES.size() * 3)).save(any());
        verify(showRepository, atLeast(1)).existsByMovieIdAndScreenIdAndShowDateAndShowTimeAndScreeningLanguage(
                eq(5L), screenIds.capture(), any(LocalDate.class), any(LocalTime.class), anyString());
        assertTrue(screenIds.getAllValues().stream().distinct().count() >= 2);
    }
}
