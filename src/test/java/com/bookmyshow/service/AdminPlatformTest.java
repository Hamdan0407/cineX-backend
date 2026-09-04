package com.bookmyshow.service;

import com.bookmyshow.dto.*;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.User;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminPlatformTest {

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private TheatreService theatreService;

    @Autowired
    private ScreenService screenService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("testadmin@cinex.com")) {
            User admin = new User();
            admin.setName("Test Admin");
            admin.setEmail("testadmin@cinex.com");
            admin.setPassword("secret");
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
        if (!userRepository.existsByEmail("normaluser@cinex.com")) {
            User user = new User();
            user.setName("Normal User");
            user.setEmail("normaluser@cinex.com");
            user.setPassword("secret");
            user.setRole("USER");
            userRepository.save(user);
        }
    }

    @Test
    void testAdminAuthValidation() {
        assertDoesNotThrow(() -> adminAuthService.validateAdmin("ADMIN", "normaluser@cinex.com"));
        assertDoesNotThrow(() -> adminAuthService.validateAdmin("USER", "testadmin@cinex.com"));
        assertThrows(SecurityException.class, () -> adminAuthService.validateAdmin("USER", "normaluser@cinex.com"));
    }

    @Test
    void testMovieCrudOperations() {
        MovieRequest newMovie = new MovieRequest();
        newMovie.setTitle("Inception Admin Test");
        newMovie.setDescription("Dream within a dream");
        newMovie.setDuration(148);
        newMovie.setLanguage("English");
        newMovie.setGenre("Sci-Fi");
        newMovie.setReleaseDate(LocalDate.of(2010, 7, 16));
        newMovie.setPosterPath("/posters/inception.jpg");
        newMovie.setTrailerUrl("https://youtube.com/watch?v=123");
        newMovie.setCast("Leonardo DiCaprio");
        newMovie.setCertification("UA");
        newMovie.setStatus("RELEASED");

        MovieResponse created = movieService.addMovie(newMovie);
        assertNotNull(created.getId());
        assertEquals("Inception Admin Test", created.getTitle());
        assertEquals("UA", created.getCertification());

        MovieRequest updateRequest = new MovieRequest();
        updateRequest.setTitle(created.getTitle());
        updateRequest.setDescription(created.getDescription());
        updateRequest.setDuration(created.getDuration());
        updateRequest.setLanguage(created.getLanguage());
        updateRequest.setGenre(created.getGenre());
        updateRequest.setReleaseDate(created.getReleaseDate());
        updateRequest.setPosterPath(created.getPosterPath());
        updateRequest.setTrailerUrl(created.getTrailerUrl());
        updateRequest.setCast(created.getCast());
        updateRequest.setCertification(created.getCertification());
        updateRequest.setStatus("ARCHIVED");
        MovieResponse updated = movieService.updateMovie(created.getId(), updateRequest);
        assertEquals("ARCHIVED", updated.getStatus());

        List<MovieResponse> searchResults = movieService.searchMoviesByTitle("Inception");
        assertFalse(searchResults.isEmpty());

        movieService.deleteMovie(created.getId());
        assertThrows(RuntimeException.class, () -> movieService.getMovieById(created.getId()));
    }

    @Test
    void testTheatreAndScreenCrudWithSeatLayout() {
        TheatreRequest theatreRequest = new TheatreRequest();
        theatreRequest.setName("PVR Admin Test");
        theatreRequest.setCity("Mumbai");
        theatreRequest.setAddress("Lower Parel");
        theatreRequest.setAmenities("Dolby Atmos, Recliner, Parking");

        TheatreResponse savedTheatre = theatreService.addTheatre(theatreRequest);
        assertNotNull(savedTheatre.getId());
        assertEquals("Dolby Atmos, Recliner, Parking", savedTheatre.getAmenities());

        ScreenRequest screenRequest = new ScreenRequest();
        screenRequest.setScreenName("Audi 1 IMAX");
        screenRequest.setTotalSeats(100);
        screenRequest.setTheatreId(savedTheatre.getId());
        screenRequest.setTotalRows(10);
        screenRequest.setTotalColumns(10);
        screenRequest.setSeatCategories("VIP,Executive,Premium,Recliner,Gold,Silver");

        ScreenResponse savedScreen = screenService.addScreen(screenRequest);
        assertNotNull(savedScreen.getId());

        SeatLayoutRequest layoutReq = new SeatLayoutRequest();
        layoutReq.setRows(5);
        layoutReq.setColumns(10);
        
        Map<String, Double> customPrices = new HashMap<>();
        customPrices.put("Recliner", 600.0);
        customPrices.put("VIP", 500.0);
        customPrices.put("Executive", 350.0);
        layoutReq.setCategoryPrices(customPrices);

        Map<String, String> rowCats = new HashMap<>();
        rowCats.put("A", "Recliner");
        rowCats.put("B", "VIP");
        rowCats.put("C", "Executive");
        layoutReq.setRowCategories(rowCats);

        List<SeatResponse> seats = seatService.buildSeatLayout(savedScreen.getId(), layoutReq);
        assertEquals(50, seats.size());

        SeatResponse rowA1 = seats.stream().filter(s -> "A1".equals(s.getSeatNumber())).findFirst().orElse(null);
        assertNotNull(rowA1);
        assertEquals("Recliner", rowA1.getSeatType());
        assertEquals(600.0, rowA1.getPrice());

        SeatResponse rowB1 = seats.stream().filter(s -> "B1".equals(s.getSeatNumber())).findFirst().orElse(null);
        assertNotNull(rowB1);
        assertEquals("VIP", rowB1.getSeatType());
        assertEquals(500.0, rowB1.getPrice());
    }

    @Test
    void testAdminDashboardStats() {
        AdminDashboardDto stats = adminDashboardService.getDashboardStats();
        assertNotNull(stats);
        assertNotNull(stats.getTodayRevenue());
        assertNotNull(stats.getMonthlyRevenue());
        assertNotNull(stats.getTotalBookings());
        assertNotNull(stats.getOccupancyPercentage());
        assertNotNull(stats.getRevenueGraphData());
        assertNotNull(stats.getBookingGraphData());
    }
}
