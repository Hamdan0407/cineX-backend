package com.bookmyshow.initializer;

import com.bookmyshow.entity.Movie;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Seat;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.entity.Show;
import com.bookmyshow.repository.MovieRepository;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.TheatreRepository;
import com.bookmyshow.repository.ShowRepository;
import com.bookmyshow.entity.User;
import com.bookmyshow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void initData() {
        if (!userRepository.existsByEmail("admin@cinex.com")) {
            User admin = new User();
            admin.setName("CineX Admin");
            admin.setEmail("admin@cinex.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("9999999999");
            admin.setRole("ADMIN");
            userRepository.save(admin);
            log.info("Default Admin User created: admin@cinex.com / admin123");
        }

        if (!userRepository.existsByEmail("user@cinex.com")) {
            User user = new User();
            user.setName("CineX User");
            user.setEmail("user@cinex.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setPhone("8888888888");
            user.setRole("USER");
            userRepository.save(user);
            log.info("Default Test User created: user@cinex.com / user123");
        }

        if (movieRepository.count() > 0) {
            log.info("Data already initialized.");
            return;
        }
        
        log.info("Initializing sample data for BookMyShow...");

        // 1. Create Sample Movies
        List<Movie> movies = new ArrayList<>();
        
        Movie movie1 = new Movie();
        movie1.setTitle("Inception");
        movie1.setDescription("A mind-bending thriller.");
        movie1.setDuration(148);
        movie1.setLanguage("English");
        movie1.setGenre("Sci-Fi");
        movie1.setReleaseDate(LocalDate.of(2010, 7, 16));
        movies.add(movie1);

        Movie movie2 = new Movie();
        movie2.setTitle("The Dark Knight");
        movie2.setDescription("Batman fights Joker.");
        movie2.setDuration(152);
        movie2.setLanguage("English");
        movie2.setGenre("Action");
        movie2.setReleaseDate(LocalDate.of(2008, 7, 18));
        movies.add(movie2);

        Movie movie3 = new Movie();
        movie3.setTitle("Interstellar");
        movie3.setDescription("Space exploration.");
        movie3.setDuration(169);
        movie3.setLanguage("English");
        movie3.setGenre("Sci-Fi");
        movie3.setReleaseDate(LocalDate.of(2014, 11, 7));
        movies.add(movie3);

        movieRepository.saveAll(movies);

        // 2. Create Sample Theatres and Screens
        Theatre t1 = new Theatre();
        t1.setName("PVR Cinemas");
        t1.setCity("Mumbai");
        t1.setAddress("Andheri West");
        t1 = theatreRepository.save(t1);

        Theatre t2 = new Theatre();
        t2.setName("INOX");
        t2.setCity("Delhi");
        t2.setAddress("Nariman Point");
        t2 = theatreRepository.save(t2);

        // Create Screens and Seats for Theatre 1
        Screen s1 = new Screen();
        s1.setScreenName("Screen 1");
        s1.setTheatre(t1);
        s1 = screenRepository.save(s1);
        
        createSeatsForScreen(s1, 20); // 20 seats

        Screen s2 = new Screen();
        s2.setScreenName("Screen 2");
        s2.setTheatre(t1);
        s2 = screenRepository.save(s2);
        
        createSeatsForScreen(s2, 20);

        // Create Screens and Seats for Theatre 2
        Screen s3 = new Screen();
        s3.setScreenName("Screen A");
        s3.setTheatre(t2);
        s3 = screenRepository.save(s3);
        
        createSeatsForScreen(s3, 30); // 30 seats

        // 3. Create Sample Shows
        Show show1 = new Show();
        show1.setMovie(movie1);
        show1.setScreen(s1);
        show1.setShowDate(LocalDate.now());
        show1.setShowTime(LocalTime.of(10, 0));
        show1.setPrice(250.0);
        showRepository.save(show1);

        Show show2 = new Show();
        show2.setMovie(movie2);
        show2.setScreen(s2);
        show2.setShowDate(LocalDate.now());
        show2.setShowTime(LocalTime.of(14, 30));
        show2.setPrice(300.0);
        showRepository.save(show2);

        Show show3 = new Show();
        show3.setMovie(movie3);
        show3.setScreen(s3);
        show3.setShowDate(LocalDate.now());
        show3.setShowTime(LocalTime.of(18, 0));
        show3.setPrice(350.0);
        showRepository.save(show3);

        log.info("Initialization completed successfully!");
    }

    private void createSeatsForScreen(Screen screen, int totalSeats) {
        List<Seat> seats = new ArrayList<>();
        int rows = totalSeats / 10;
        int seatsPerRow = 10;
        
        for (int i = 0; i < rows; i++) {
            char rowChar = (char) ('A' + i);
            for (int j = 1; j <= seatsPerRow; j++) {
                Seat seat = new Seat();
                seat.setSeatNumber(rowChar + str(j));
                seat.setSeatType("PREMIUM");
                seat.setScreen(screen);
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
    }
    
    private String str(int val) {
        return String.valueOf(val);
    }
}
