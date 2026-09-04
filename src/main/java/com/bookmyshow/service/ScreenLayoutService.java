package com.bookmyshow.service;

import com.bookmyshow.catalog.ScreenLayoutCatalog;
import com.bookmyshow.catalog.ScreenLayoutCatalog.SeatBlueprint;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Seat;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenLayoutService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;

    @Transactional
    public void applyLayout(Screen screen, String layoutProfile) {
        List<SeatBlueprint> blueprint = ScreenLayoutCatalog.seatsForProfile(layoutProfile);
        List<Seat> existing = new ArrayList<>(seatRepository.findByScreenId(screen.getId()));

        if (isLayoutCurrent(screen, layoutProfile, existing, blueprint.size())) {
            return;
        }

        Map<String, Seat> byNumber = existing.stream()
                .filter(seat -> seat.getSeatNumber() != null)
                .collect(Collectors.toMap(Seat::getSeatNumber, Function.identity(), (a, b) -> a));

        Set<String> wanted = blueprint.stream().map(SeatBlueprint::seatNumber).collect(Collectors.toSet());
        List<Seat> extras = existing.stream()
                .filter(seat -> seat.getSeatNumber() == null || !wanted.contains(seat.getSeatNumber()))
                .toList();
        if (!extras.isEmpty()) {
            seatRepository.deleteAll(extras);
        }

        List<Seat> seats = new ArrayList<>();
        for (SeatBlueprint bp : blueprint) {
            Seat seat = byNumber.get(bp.seatNumber());
            if (seat == null || extras.contains(seat)) {
                seat = new Seat();
                seat.setScreen(screen);
            }
            applyBlueprint(seat, bp, screen);
            seats.add(seat);
        }
        seatRepository.saveAll(seats);

        int maxRow = blueprint.stream().mapToInt(SeatBlueprint::rowIndex).max().orElse(0);
        int maxCol = blueprint.stream().mapToInt(SeatBlueprint::columnIndex).max().orElse(0);

        screen.setLayoutProfile(layoutProfile);
        screen.setTotalSeats(seats.size());
        screen.setTotalRows(maxRow + 1);
        screen.setTotalColumns(maxCol + 1);
        screen.setSeatCategories(describeCategories(blueprint));
        screenRepository.save(screen);

        log.info("Applied layout '{}' to screen '{}': {} seats, {} rows",
                layoutProfile, screen.getScreenName(), seats.size(), maxRow + 1);
    }

    private boolean isLayoutCurrent(Screen screen, String layoutProfile, List<Seat> existing, int expectedCount) {
        if (existing.isEmpty()) {
            return false;
        }
        if (!Objects.equals(layoutProfile, screen.getLayoutProfile())) {
            return false;
        }
        if (existing.size() != expectedCount) {
            return false;
        }
        return existing.stream().allMatch(seat -> seat.getRowIndex() != null && seat.getColumnIndex() != null);
    }

    private void applyBlueprint(Seat seat, SeatBlueprint bp, Screen screen) {
        seat.setScreen(screen);
        seat.setSeatNumber(bp.seatNumber());
        seat.setSeatType(bp.seatType());
        seat.setRowLabel(bp.rowLabel());
        seat.setRowIndex(bp.rowIndex());
        seat.setColumnIndex(bp.columnIndex());
        seat.setWheelchairAccessible(bp.wheelchairAccessible());
        seat.setPrice(bp.priceOffset() > 0 ? bp.priceOffset() : null);
    }

    private String describeCategories(List<SeatBlueprint> blueprint) {
        return blueprint.stream()
                .map(SeatBlueprint::seatType)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .reduce((a, b) -> a + ", " + b)
                .orElse("CLASSIC");
    }
}
