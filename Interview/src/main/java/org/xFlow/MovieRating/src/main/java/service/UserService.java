package org.xFlow.MovieRating.src.main.java.service;


import org.xFlow.MovieRating.src.main.java.exceptions.AlreadyExistsException;
import org.xFlow.MovieRating.src.main.java.exceptions.DoesNotExistsException;
import org.xFlow.MovieRating.src.main.java.models.User;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    Map<Integer, User> users = new HashMap<>();
    private final RatingService ratingService;

    public UserService(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public void create(int id, String name){
        if(users.containsKey(id)){
            throw new AlreadyExistsException("user with id: "+ id + " already present");
        }
        User user = new User(id, name);
        users.put(id, user);
    }

    public User getById(int id){
       if(!users.containsKey(id)){
           throw new DoesNotExistsException("user doesn't exists");
       }
       return users.get(id);
    }

    public void rate(int userId, int movieId, int rating){
        if(!users.containsKey(userId)){
            throw new DoesNotExistsException("user doesn't exists");
        }
        // TODO: movie exists or not.
        int weight = users.get(userId).rateMovie();
        ratingService.createRating(userId, movieId, rating, weight);
    }
}
