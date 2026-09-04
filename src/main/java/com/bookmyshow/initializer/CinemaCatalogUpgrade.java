package com.bookmyshow.initializer;

import com.bookmyshow.catalog.ScreenLayoutCatalog;
import com.bookmyshow.service.TmdbCatalogSyncService;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.TheatreRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Idempotent upgrade that seeds multi-city theatres, TMDB-linked movies,
 * and real showtimes with screening languages.
 */
@Component("cinemaCatalogUpgrade")
@Order(1)
@DependsOn("databaseSchemaPatch")
@RequiredArgsConstructor
@Slf4j
public class CinemaCatalogUpgrade {

    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final TmdbCatalogSyncService tmdbCatalogSyncService;

    @PostConstruct
    @Transactional
    public void upgradeCatalog() {
        log.info("Running cinema catalog upgrade...");
        normalizeLegacyCities();
        tmdbCatalogSyncService.syncAndGetActiveMovies();
        upsertTheatresAndScreens();
        // Show inventory is provisioned on-demand when a user opens a TMDB title in a city
        // (see ShowService.getShowsByTmdbId) to avoid blocking startup with bulk inserts.
        log.info("Cinema catalog upgrade completed (theatres synced; show inventory is on-demand).");
    }

    private void normalizeLegacyCities() {
        theatreRepository.findAll().stream()
                .filter(theatre -> "Delhi".equalsIgnoreCase(theatre.getCity()))
                .forEach(theatre -> {
                    theatre.setCity("Delhi NCR");
                    theatreRepository.save(theatre);
                });
    }

    private Map<String, List<Screen>> upsertTheatresAndScreens() {
        List<TheatreSeed> seeds = List.of(
                new TheatreSeed("Chennai", "PVR Sathyam Cinemas: Royapettah", "Royapettah, Chennai",
                        "4K Dolby Atmos • SPI Gourmet", true),
                new TheatreSeed("Chennai", "PVR: Phoenix Marketcity, Velachery", "Velachery, Chennai",
                        "IMAX with Laser • 4K Dolby Atmos", false),
                new TheatreSeed("Chennai", "AGS Cinemas: T. Nagar", "T. Nagar, Chennai",
                        "Dolby Atmos • Recliner Lounge", false),
                new TheatreSeed("Mumbai", "PVR ICON: Palladium, Lower Parel", "Lower Parel, Mumbai",
                        "IMAX • 4K Dolby Atmos • Recliner Seats", false),
                new TheatreSeed("Mumbai", "INOX: Laserplex, Nariman Point", "Nariman Point, Mumbai",
                        "Laser Projection • Dolby 7.1", false),
                new TheatreSeed("Mumbai", "Cinepolis: Fun Republic, Andheri", "Andheri West, Mumbai",
                        "4DX • Dolby Atmos • RealD 3D", false),
                new TheatreSeed("Bengaluru", "PVR: Forum Mall, Koramangala", "Koramangala, Bengaluru",
                        "IMAX • 4K Dolby Atmos • Gold Class", false),
                new TheatreSeed("Bengaluru", "INOX: Mantri Square, Malleshwaram", "Malleshwaram, Bengaluru",
                        "INSIGNIA • Dolby 7.1 • 2K Laser", false),
                new TheatreSeed("Hyderabad", "AMB Cinemas: Gachibowli", "Gachibowli, Hyderabad",
                        "Laser projection • VIP Lounge • Dolby Atmos", false),
                new TheatreSeed("Hyderabad", "PVR: Nexus Mall, Kukatpally", "Kukatpally, Hyderabad",
                        "IMAX • Dolby Atmos • Recliners", false),
                new TheatreSeed("Delhi NCR", "PVR: Select CityWalk, Saket", "Saket, Delhi NCR",
                        "IMAX • Gold Class • Dolby Atmos", false),
                new TheatreSeed("Delhi NCR", "INOX: Odeon, Connaught Place", "Connaught Place, Delhi NCR",
                        "Heritage • 4K Projection • Dolby 7.1", false)
        );

        Map<String, List<Screen>> screensByCity = new java.util.HashMap<>();
        for (TheatreSeed seed : seeds) {
            Theatre theatre = theatreRepository.findByNameIgnoreCaseAndCityIgnoreCase(seed.name(), seed.city())
                    .or(() -> theatreRepository.findAll().stream()
                            .filter(t -> seed.sathyam() && t.getName().toLowerCase().contains("sathyam")
                                    && "Chennai".equalsIgnoreCase(t.getCity()))
                            .findFirst())
                    .orElseGet(Theatre::new);
            theatre.setName(seed.name());
            theatre.setCity(seed.city());
            theatre.setAddress(seed.address());
            theatre.setAmenities(seed.amenities());
            theatre = theatreRepository.save(theatre);

            List<Screen> screens = new ArrayList<>();
            if (seed.sathyam()) {
                screens.addAll(ensureSathyamScreens(theatre));
            } else {
                screens.add(ensureScreenShell(theatre, "Screen 1"));
                screens.add(ensureScreenShell(theatre, "Screen 2"));
            }
            screensByCity.computeIfAbsent(seed.city(), key -> new ArrayList<>()).addAll(screens);
        }
        return screensByCity;
    }

    private List<Screen> ensureSathyamScreens(Theatre theatre) {
        String[] names = ScreenLayoutCatalog.sathyamScreens().keySet().toArray(String[]::new);
        List<Screen> existing = screenRepository.findByTheatreId(theatre.getId()).stream()
                .sorted(Comparator.comparing(Screen::getId))
                .toList();
        List<Screen> configured = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Screen screen = i < existing.size() ? existing.get(i) : new Screen();
            screen.setTheatre(theatre);
            screen.setScreenName(names[i]);
            configured.add(screenRepository.save(screen));
        }
        return configured;
    }

    private Screen ensureScreenShell(Theatre theatre, String screenName) {
        Optional<Screen> existing = screenRepository.findAll().stream()
                .filter(screen -> screen.getTheatre().getId().equals(theatre.getId())
                        && screenName.equalsIgnoreCase(screen.getScreenName()))
                .findFirst();
        Screen screen = existing.orElseGet(Screen::new);
        screen.setScreenName(screenName);
        screen.setTheatre(theatre);
        return screenRepository.save(screen);
    }

    private record TheatreSeed(String city, String name, String address, String amenities, boolean sathyam) {}
}
