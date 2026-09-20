package org.xFlow.MovieRating.src.main.java.service;


import org.xFlow.MovieRating.src.main.java.models.Rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatingService {
    List<Rating> ratings = new ArrayList<>();

    Map<Integer, List<Rating>> movieRatings = new HashMap<>();
    Map<Integer, List<Rating>> usersToRatingMap = new HashMap<>();

    void createRating(int userId, int movieId, int rating, int weight){
        Rating r =  new Rating(userId, movieId, rating, weight);
        movieRatings.get(movieId).add(r);
        usersToRatingMap.get(userId).add(r);
        ratings.add(r);
    }

    public List<Rating> getAllRatings(){
        return ratings;
    }
}
