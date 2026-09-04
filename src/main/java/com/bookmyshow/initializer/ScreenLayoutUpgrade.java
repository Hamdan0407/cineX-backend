package com.bookmyshow.initializer;

import com.bookmyshow.catalog.ScreenLayoutCatalog;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.entity.Show;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.ShowRepository;
import com.bookmyshow.repository.TheatreRepository;
import com.bookmyshow.service.ScreenLayoutService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Migrates legacy theatre name and applies screen-specific seat layouts.
 */
@Component
@Order(2)
@DependsOn("cinemaCatalogUpgrade")
@RequiredArgsConstructor
@Slf4j
public class ScreenLayoutUpgrade {

    private static final String SATHYAM_PVR = "PVR Sathyam Cinemas: Royapettah";

    private static final String[] SATHYAM_SCREEN_NAMES = ScreenLayoutCatalog.sathyamScreens().keySet()
            .toArray(String[]::new);
    private static final String[] SATHYAM_LAYOUTS = ScreenLayoutCatalog.sathyamScreens().values()
            .toArray(String[]::new);

    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ScreenLayoutService screenLayoutService;
    private final ShowRepository showRepository;
    private final CacheManager cacheManager;

    @PostConstruct
    @Transactional
    public void upgradeLayouts() {
        migrateSathyamTheatreName();
        applySathyamLayouts();
        applyStandardLayoutsForOtherTheatres();
        refreshShowSeatCapacity();
        evictCatalogCaches();
    }

    private void evictCatalogCaches() {
        for (String cacheName : List.of("theatres", "cities", "shows")) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("Evicted theatre/show caches after layout upgrade");
    }

    private void refreshShowSeatCapacity() {
        List<Show> updates = showRepository.findAll().stream()
                .filter(show -> show.getScreen() != null
                        && show.getScreen().getTotalSeats() != null
                        && show.getScreen().getTotalSeats() > 0
                        && !show.getScreen().getTotalSeats().equals(show.getAvailableSeats()))
                .peek(show -> show.setAvailableSeats(show.getScreen().getTotalSeats()))
                .toList();
        if (!updates.isEmpty()) {
            showRepository.saveAll(updates);
        }
        log.info("Synchronized availableSeats for {} shows", updates.size());
    }

    private void migrateSathyamTheatreName() {
        theatreRepository.findAll().stream()
                .filter(t -> "Chennai".equalsIgnoreCase(t.getCity())
                        && t.getName() != null
                        && t.getName().toLowerCase().contains("sathyam")
                        && !SATHYAM_PVR.equalsIgnoreCase(t.getName()))
                .forEach(theatre -> {
                    theatre.setName(SATHYAM_PVR);
                    theatreRepository.save(theatre);
                    log.info("Renamed theatre '{}' to {}", theatre.getName(), SATHYAM_PVR);
                });
    }

    private void applySathyamLayouts() {
        theatreRepository.findByNameIgnoreCaseAndCityIgnoreCase(SATHYAM_PVR, "Chennai")
                .or(() -> theatreRepository.findAll().stream()
                        .filter(t -> "Chennai".equalsIgnoreCase(t.getCity())
                                && t.getName() != null
                                && t.getName().toLowerCase().contains("sathyam"))
                        .findFirst())
                .ifPresent(theatre -> {
                    theatre.setName(SATHYAM_PVR);
                    theatreRepository.save(theatre);
                    configureSathyamScreens(theatre);
                });
    }

    private void configureSathyamScreens(Theatre theatre) {
        List<Screen> configured = new ArrayList<>();
        for (int i = 0; i < SATHYAM_SCREEN_NAMES.length; i++) {
            Screen screen = resolveScreen(theatre, SATHYAM_SCREEN_NAMES[i]);
            screenLayoutService.applyLayout(screen, SATHYAM_LAYOUTS[i]);
            configured.add(screen);
        }

        log.info("Configured {} Sathyam screens: {}", configured.size(),
                configured.stream().map(Screen::getScreenName).toList());
        purgeExtraSathyamScreens(theatre, configured);
    }

    private void purgeExtraSathyamScreens(Theatre theatre, List<Screen> canonicalScreens) {
        var canonicalIds = canonicalScreens.stream().map(Screen::getId).collect(java.util.stream.Collectors.toSet());
        screenRepository.findByTheatreId(theatre.getId()).stream()
                .filter(screen -> !canonicalIds.contains(screen.getId()))
                .forEach(extra -> {
                    Screen target = canonicalScreens.stream()
                            .filter(canonical -> canonical.getScreenName() != null
                                    && canonical.getScreenName().equalsIgnoreCase(extra.getScreenName()))
                            .findFirst()
                            .orElse(canonicalScreens.get(0));
                    mergeDuplicateScreen(extra, target);
                });
    }

    private Screen resolveScreen(Theatre theatre, String screenName) {
        List<Screen> matches = screenRepository.findByTheatreId(theatre.getId()).stream()
                .filter(screen -> screenName.equalsIgnoreCase(screen.getScreenName()))
                .sorted(Comparator.comparing(Screen::getId))
                .toList();

        Screen canonical;
        if (matches.isEmpty()) {
            canonical = new Screen();
            canonical.setScreenName(screenName);
            canonical.setTheatre(theatre);
            canonical = screenRepository.save(canonical);
        } else {
            canonical = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                mergeDuplicateScreen(matches.get(i), canonical);
            }
        }

        canonical.setScreenName(screenName);
        canonical.setTheatre(theatre);
        return screenRepository.save(canonical);
    }

    private void mergeDuplicateScreen(Screen duplicate, Screen canonical) {
        showRepository.findByScreenId(duplicate.getId()).forEach(show -> {
            show.setScreen(canonical);
            if (canonical.getTotalSeats() != null && canonical.getTotalSeats() > 0) {
                show.setAvailableSeats(canonical.getTotalSeats());
            }
            showRepository.save(show);
        });
        seatRepository.deleteAll(seatRepository.findByScreenId(duplicate.getId()));
        screenRepository.delete(duplicate);
        log.info("Merged duplicate screen {} into {}", duplicate.getId(), canonical.getId());
    }

    private void applyStandardLayoutsForOtherTheatres() {
        theatreRepository.findAll().stream()
                .filter(t -> t.getName() == null || !t.getName().toLowerCase().contains("sathyam"))
                .forEach(theatre -> screenRepository.findByTheatreId(theatre.getId())
                        .forEach(screen -> screenLayoutService.applyLayout(
                                screen, ScreenLayoutCatalog.STANDARD_AUDITORIUM)));
    }
}
