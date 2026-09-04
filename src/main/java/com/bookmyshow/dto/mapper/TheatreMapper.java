package com.bookmyshow.dto.mapper;

import com.bookmyshow.dto.TheatreRequest;
import com.bookmyshow.dto.TheatreResponse;
import com.bookmyshow.entity.Theatre;

public final class TheatreMapper {

    private TheatreMapper() {
    }

    public static Theatre toNewEntity(TheatreRequest request) {
        Theatre theatre = new Theatre();
        applyRequest(theatre, request);
        return theatre;
    }

    public static void applyRequest(Theatre theatre, TheatreRequest request) {
        theatre.setName(request.getName());
        theatre.setCity(request.getCity());
        theatre.setAddress(request.getAddress());
        theatre.setAmenities(request.getAmenities());
    }

    public static TheatreResponse toResponse(Theatre theatre) {
        TheatreResponse response = new TheatreResponse();
        response.setId(theatre.getId());
        response.setName(theatre.getName());
        response.setCity(theatre.getCity());
        response.setAddress(theatre.getAddress());
        response.setAmenities(theatre.getAmenities());
        return response;
    }

    public static TheatreRequest toRequest(TheatreResponse source) {
        if (source == null) {
            return null;
        }
        TheatreRequest request = new TheatreRequest();
        request.setName(source.getName());
        request.setCity(source.getCity());
        request.setAddress(source.getAddress());
        request.setAmenities(source.getAmenities());
        return request;
    }
}
