package org.xFlow.MovieRating.src.main.java.controller;

import org.xFlow.MovieRating.src.main.java.models.Genere;
import org.xFlow.MovieRating.src.main.java.service.MovieService;

import java.util.List;

public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    public void create(int id, String name, String releaseYear, List<Genere> genereList){
         movieService.create(id, name, releaseYear, genereList);
    }
}
