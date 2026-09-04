package com.bookmyshow.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenLayoutCatalogTest {

    @Test
    void sathyamHasSixDistinctScreens() {
        assertEquals(6, ScreenLayoutCatalog.sathyamScreens().size());
        assertTrue(ScreenLayoutCatalog.sathyamScreens().containsKey("Screen 1 (Dolby Atmos)"));
        assertTrue(ScreenLayoutCatalog.sathyamScreens().containsKey("Elite"));
    }

    @Test
    void eachSathyamLayoutIsUniqueAndNotAGenericGrid() {
        var screens = ScreenLayoutCatalog.sathyamScreens();
        Set<Integer> counts = screens.values().stream()
                .map(ScreenLayoutCatalog::expectedSeatCount)
                .collect(Collectors.toSet());
        assertEquals(6, counts.size(), "each Sathyam screen must have a different seat count");
        counts.forEach(count -> assertTrue(count > 20, "must not be the legacy 2x10 grid"));
    }

    @Test
    void screen1HasAislesCategoriesAndWheelchairSpaces() {
        List<ScreenLayoutCatalog.SeatBlueprint> seats =
                ScreenLayoutCatalog.seatsForProfile(ScreenLayoutCatalog.SATHYAM_SCREEN_1);
        assertEquals(162, seats.size());
        Set<String> types = seats.stream().map(ScreenLayoutCatalog.SeatBlueprint::seatType).collect(Collectors.toSet());
        assertTrue(types.containsAll(Set.of("ROYALE", "CLUB", "CLASSIC", "WHEELCHAIR")));
        List<Integer> cols = seats.stream()
                .filter(s -> "A".equals(s.rowLabel()) && !s.wheelchairAccessible())
                .map(ScreenLayoutCatalog.SeatBlueprint::columnIndex)
                .sorted()
                .toList();
        boolean hasAisle = false;
        for (int i = 1; i < cols.size(); i++) {
            if (cols.get(i) - cols.get(i - 1) > 1) {
                hasAisle = true;
            }
        }
        assertTrue(hasAisle);
        assertEquals(16, cols.size());
    }

    @Test
    void eliteIsAllRoyaleReclinersPlusAccessible() {
        List<ScreenLayoutCatalog.SeatBlueprint> seats =
                ScreenLayoutCatalog.seatsForProfile(ScreenLayoutCatalog.SATHYAM_ELITE);
        assertEquals(42, seats.size());
        assertEquals(5, seats.stream().map(ScreenLayoutCatalog.SeatBlueprint::rowIndex).distinct().count());
        assertTrue(seats.stream().anyMatch(ScreenLayoutCatalog.SeatBlueprint::wheelchairAccessible));
        assertTrue(seats.stream().filter(s -> !s.wheelchairAccessible())
                .allMatch(s -> "ROYALE".equals(s.seatType())));
    }
}
