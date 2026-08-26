package com.bookmyshow.service;

import com.bookmyshow.dto.BookingResponse;
import com.bookmyshow.dto.MovieDto;
import com.bookmyshow.dto.ShowDto;
import com.bookmyshow.dto.TheatreDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PaginationServiceTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private TheatreService theatreService;

    @Autowired
    private ShowService showService;

    @Autowired
    private BookingService bookingService;

    @Test
    @DisplayName("Verify Spring Pageable and Sorting on MovieService")
    void testMoviePaginationAndSorting() {
        // Add 3 movies to ensure pageable data exists
        for (int i = 1; i <= 3; i++) {
            MovieDto m = new MovieDto();
            m.setTitle("Paginated Movie " + i);
            m.setDescription("Desc " + i);
            m.setDuration(120);
            m.setLanguage("English");
            m.setGenre("Action");
            m.setReleaseDate(LocalDate.now());
            movieService.addMovie(m);
        }

        Page<MovieDto> page0 = movieService.getMoviesPaginated(0, 2, "id", "desc");
        assertNotNull(page0, "Page should not be null");
        assertEquals(2, page0.getSize(), "Page size should be 2");
        assertTrue(page0.getTotalElements() >= 3, "Total elements should be at least 3");
        assertEquals(0, page0.getNumber(), "Page number should be 0");
        assertTrue(page0.getContent().size() <= 2, "Content size should be <= page size");

        Page<MovieDto> page1 = movieService.getMoviesPaginated(1, 2, "id", "desc");
        assertNotNull(page1);
        assertEquals(1, page1.getNumber(), "Page number should be 1");
    }

    @Test
    @DisplayName("Verify Spring Pageable on TheatreService")
    void testTheatrePagination() {
        for (int i = 1; i <= 3; i++) {
            TheatreDto t = new TheatreDto();
            t.setName("Paginated Theatre " + i);
            t.setCity("Mumbai");
            t.setAddress("Addr " + i);
            theatreService.addTheatre(t);
        }

        Page<TheatreDto> page = theatreService.getTheatresPaginated(0, 5, "name", "asc");
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 3);
        assertEquals(0, page.getNumber());
    }

    @Test
    @DisplayName("Verify Spring Pageable on ShowService")
    void testShowPagination() {
        Page<ShowDto> page = showService.getShowsPaginated(0, 10, "id", "asc");
        assertNotNull(page);
        assertEquals(10, page.getSize());
        assertEquals(0, page.getNumber());
    }

    @Test
    @DisplayName("Verify Spring Pageable on BookingService")
    void testBookingPagination() {
        Page<BookingResponse> page = bookingService.getBookingsPaginated(0, 10, "id", "desc");
        assertNotNull(page);
        assertEquals(10, page.getSize());
        assertEquals(0, page.getNumber());
    }
}
