package org.xFlow.MovieRating.src.main.java.service;

import org.xFlow.MovieRating.src.main.java.exceptions.AlreadyExistsException;
import org.xFlow.MovieRating.src.main.java.exceptions.DoesNotExistsException;
import org.xFlow.MovieRating.src.main.java.models.Genere;
import org.xFlow.MovieRating.src.main.java.models.Movie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieService {
    Map<Integer, Movie> movies = new HashMap<>();

    public void create(int id, String name, String releaseYear, List<Genere> genereList){
        if(movies.containsKey(id)){
            throw new AlreadyExistsException("user with id: "+ id + " already present");
        }
        Movie movie = new Movie(id, name, releaseYear, genereList);
        movies.put(id, movie);
    }

    public Movie getById(int id){
        if(!movies.containsKey(id)){
            throw new DoesNotExistsException("user doesn't exists");
        }
        return movies.get(id);
    }
}
