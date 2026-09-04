package com.bookmyshow.dto.mapper;

import com.bookmyshow.dto.ScreenRequest;
import com.bookmyshow.dto.ScreenResponse;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Theatre;

public final class ScreenMapper {

    private ScreenMapper() {
    }

    public static Screen toNewEntity(ScreenRequest request, Theatre theatre) {
        Screen screen = new Screen();
        applyRequest(screen, request, theatre);
        return screen;
    }

    public static void applyRequest(Screen screen, ScreenRequest request, Theatre theatre) {
        screen.setScreenName(request.getScreenName());
        screen.setTotalSeats(request.getTotalSeats());
        screen.setTotalRows(request.getTotalRows() != null ? request.getTotalRows() : 10);
        screen.setTotalColumns(request.getTotalColumns() != null ? request.getTotalColumns() : 10);
        screen.setSeatCategories(request.getSeatCategories() != null
                ? request.getSeatCategories()
                : "VIP,Executive,Premium,Recliner,Gold,Silver");
        screen.setTheatre(theatre);
    }

    public static void applyUpdate(Screen screen, ScreenRequest request, Theatre theatre) {
        screen.setScreenName(request.getScreenName());
        screen.setTotalSeats(request.getTotalSeats());
        if (request.getTotalRows() != null) {
            screen.setTotalRows(request.getTotalRows());
        }
        if (request.getTotalColumns() != null) {
            screen.setTotalColumns(request.getTotalColumns());
        }
        if (request.getSeatCategories() != null) {
            screen.setSeatCategories(request.getSeatCategories());
        }
        if (theatre != null) {
            screen.setTheatre(theatre);
        }
    }

    public static ScreenResponse toResponse(Screen screen) {
        ScreenResponse response = new ScreenResponse();
        response.setId(screen.getId());
        response.setScreenName(screen.getScreenName());
        response.setTotalSeats(screen.getTotalSeats());
        response.setTotalRows(screen.getTotalRows());
        response.setTotalColumns(screen.getTotalColumns());
        response.setSeatCategories(screen.getSeatCategories());
        response.setTheatreId(screen.getTheatre().getId());
        return response;
    }

    public static ScreenRequest toRequest(ScreenResponse source) {
        if (source == null) {
            return null;
        }
        ScreenRequest request = new ScreenRequest();
        request.setScreenName(source.getScreenName());
        request.setTotalSeats(source.getTotalSeats());
        request.setTotalRows(source.getTotalRows());
        request.setTotalColumns(source.getTotalColumns());
        request.setSeatCategories(source.getSeatCategories());
        request.setTheatreId(source.getTheatreId());
        return request;
    }
}
