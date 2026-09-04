package com.bookmyshow.dto.mapper;

import com.bookmyshow.dto.ShowRequest;
import com.bookmyshow.dto.ShowResponse;
import com.bookmyshow.entity.Movie;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Show;

public final class ShowMapper {

    private ShowMapper() {
    }

    public static Show toNewEntity(ShowRequest request, Movie movie, Screen screen) {
        Show show = new Show();
        show.setMovie(movie);
        show.setScreen(screen);
        show.setShowTime(request.getShowTime());
        show.setShowDate(request.getShowDate());
        show.setPrice(request.getPrice());
        show.setAvailableSeats(screen.getTotalSeats() != null ? screen.getTotalSeats() : 100);
        return show;
    }

    public static void applyUpdate(Show show, ShowRequest request, Movie movie, Screen screen) {
        if (movie != null) {
            show.setMovie(movie);
        }
        if (screen != null) {
            show.setScreen(screen);
        }
        if (request.getShowTime() != null) {
            show.setShowTime(request.getShowTime());
        }
        if (request.getShowDate() != null) {
            show.setShowDate(request.getShowDate());
        }
        if (request.getPrice() != null) {
            show.setPrice(request.getPrice());
        }
    }

    public static ShowResponse toResponse(Show show, int availableSeats) {
        ShowResponse response = new ShowResponse();
        response.setId(show.getId());
        response.setMovieId(show.getMovie().getId());
        response.setScreenId(show.getScreen().getId());
        response.setTheatreId(show.getScreen().getTheatre().getId());
        response.setTheatreName(show.getScreen().getTheatre().getName());
        response.setCity(show.getScreen().getTheatre().getCity());
        response.setScreenName(show.getScreen().getScreenName());
        response.setScreeningLanguage(show.getScreeningLanguage());
        response.setMovieTitle(show.getMovie().getTitle());
        response.setShowTime(show.getShowTime());
        response.setShowDate(show.getShowDate());
        response.setPrice(show.getPrice());
        response.setAvailableSeats(availableSeats);
        return response;
    }

    public static ShowRequest toRequest(ShowResponse source) {
        if (source == null) {
            return null;
        }
        ShowRequest request = new ShowRequest();
        request.setMovieId(source.getMovieId());
        request.setScreenId(source.getScreenId());
        request.setShowTime(source.getShowTime());
        request.setShowDate(source.getShowDate());
        request.setPrice(source.getPrice());
        return request;
    }
}
