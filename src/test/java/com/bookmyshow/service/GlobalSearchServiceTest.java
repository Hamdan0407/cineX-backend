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
public class GlobalSearchServiceTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private TheatreService theatreService;

    @Autowired
    private ShowService showService;

    @Autowired
    private BookingService bookingService;

    @Test
    @DisplayName("Verify Global Search and Sorting on MovieService")
    void testMovieGlobalSearchAndSorting() {
        MovieDto m1 = new MovieDto();
        m1.setTitle("Avengers Sci-Fi Epic");
        m1.setDescription("Action superhero movie");
        m1.setDuration(180);
        m1.setLanguage("English");
        m1.setGenre("Sci-Fi");
        m1.setReleaseDate(LocalDate.now());
        movieService.addMovie(m1);

        MovieDto m2 = new MovieDto();
        m2.setTitle("Bahubali Drama");
        m2.setDescription("Epic war movie");
        m2.setDuration(160);
        m2.setLanguage("Telugu");
        m2.setGenre("Action");
        m2.setReleaseDate(LocalDate.now());
        movieService.addMovie(m2);

        Page<MovieDto> searchByTitle = movieService.searchMoviesPaginated("Avengers", 0, 10, "id", "asc");
        assertNotNull(searchByTitle);
        assertTrue(searchByTitle.getTotalElements() >= 1, "Should find Avengers by title");

        Page<MovieDto> searchByGenre = movieService.searchMoviesPaginated("Sci-Fi", 0, 10, "id", "asc");
        assertNotNull(searchByGenre);
        assertTrue(searchByGenre.getTotalElements() >= 1, "Should find Avengers by genre");

        Page<MovieDto> searchByLanguage = movieService.searchMoviesPaginated("Telugu", 0, 10, "id", "asc");
        assertNotNull(searchByLanguage);
        assertTrue(searchByLanguage.getTotalElements() >= 1, "Should find Bahubali by language");
    }

    @Test
    @DisplayName("Verify Global Search on TheatreService")
    void testTheatreGlobalSearch() {
        TheatreDto t1 = new TheatreDto();
        t1.setName("PVR Cyberhub IMAX");
        t1.setCity("Gurugram");
        t1.setAddress("DLF Cyber City");
        theatreService.addTheatre(t1);

        Page<TheatreDto> searchByName = theatreService.searchTheatresPaginated("Cyberhub", 0, 10, "name", "asc");
        assertNotNull(searchByName);
        assertTrue(searchByName.getTotalElements() >= 1, "Should find PVR by name");

        Page<TheatreDto> searchByCity = theatreService.searchTheatresPaginated("Gurugram", 0, 10, "name", "asc");
        assertNotNull(searchByCity);
        assertTrue(searchByCity.getTotalElements() >= 1, "Should find PVR by city");
    }

    @Test
    @DisplayName("Verify Global Search on ShowService")
    void testShowGlobalSearch() {
        Page<ShowDto> page = showService.searchShowsPaginated(null, 0, 10, "id", "asc");
        assertNotNull(page);
    }

    @Test
    @DisplayName("Verify Global Search on BookingService")
    void testBookingGlobalSearch() {
        Page<BookingResponse> page = bookingService.searchBookingsPaginated("CNX", 0, 10, "id", "desc");
        assertNotNull(page);
    }
}
