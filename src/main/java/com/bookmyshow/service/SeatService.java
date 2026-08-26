package com.bookmyshow.service;

import com.bookmyshow.dto.SeatDto;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Seat;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.SeatRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.dto.SeatLayoutRequest;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SeatService {
    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;

    public SeatService(SeatRepository seatRepository, ScreenRepository screenRepository) {
        this.seatRepository = seatRepository;
        this.screenRepository = screenRepository;
    }

    @Transactional
    public List<SeatDto> buildSeatLayout(Long screenId, SeatLayoutRequest request) {
        Screen screen = screenRepository.findById(screenId)
            .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

        List<Seat> existingSeats = seatRepository.findByScreenId(screenId);
        seatRepository.deleteAll(existingSeats);

        int rows = request.getRows() > 0 ? request.getRows() : 10;
        int cols = request.getColumns() > 0 ? request.getColumns() : 10;

        screen.setTotalRows(rows);
        screen.setTotalColumns(cols);
        screen.setTotalSeats(rows * cols);
        screenRepository.save(screen);

        Map<String, Double> prices = request.getCategoryPrices() != null ? request.getCategoryPrices() : new HashMap<>();
        Map<String, String> rowCats = request.getRowCategories() != null ? request.getRowCategories() : new HashMap<>();

        List<Seat> seats = new ArrayList<>();
        char rowChar = 'A';

        for (int r = 0; r < rows; r++) {
            String rowStr = String.valueOf(rowChar);
            String category = rowCats.get(rowStr);
            if (category == null) {
                if (r == 0) category = "Recliner";
                else if (r == 1) category = "VIP";
                else if (r < rows / 3) category = "Executive";
                else if (r < 2 * rows / 3) category = "Premium";
                else if (r < rows - 1) category = "Gold";
                else category = "Silver";
            }

            Double price = prices.get(category);
            if (price == null) {
                switch (category.toUpperCase()) {
                    case "RECLINER": price = 450.0; break;
                    case "VIP": price = 500.0; break;
                    case "EXECUTIVE": price = 350.0; break;
                    case "PREMIUM": price = 300.0; break;
                    case "GOLD": price = 250.0; break;
                    case "SILVER": default: price = 180.0; break;
                }
            }

            for (int c = 1; c <= cols; c++) {
                Seat seat = new Seat();
                seat.setSeatNumber(rowStr + c);
                seat.setSeatType(category);
                seat.setPrice(price);
                seat.setScreen(screen);
                seats.add(seat);
            }
            rowChar++;
        }

        List<Seat> savedSeats = seatRepository.saveAll(seats);
        return savedSeats.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<SeatDto> bulkCreateSeats(Long screenId) {
        SeatLayoutRequest defaultReq = new SeatLayoutRequest();
        return buildSeatLayout(screenId, defaultReq);
    }

    public SeatDto updateSeat(Long id, SeatDto dto) {
        Seat seat = seatRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
        if (dto.getSeatNumber() != null) seat.setSeatNumber(dto.getSeatNumber());
        if (dto.getSeatType() != null) seat.setSeatType(dto.getSeatType());
        if (dto.getPrice() != null) seat.setPrice(dto.getPrice());
        return mapToDto(seatRepository.save(seat));
    }

    public void deleteSeat(Long id) {
        if (!seatRepository.existsById(id)) {
            throw new ResourceNotFoundException("Seat not found");
        }
        seatRepository.deleteById(id);
    }

    public List<SeatDto> getSeatsByScreen(Long screenId) {
        return seatRepository.findByScreenId(screenId).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    private SeatDto mapToDto(Seat seat) {
        SeatDto dto = new SeatDto();
        dto.setId(seat.getId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setSeatType(seat.getSeatType());
        dto.setPrice(seat.getPrice());
        dto.setScreenId(seat.getScreen().getId());
        return dto;
    }
}
