package org.xFlow.MovieRating.src.main.java.controller;


import org.xFlow.MovieRating.src.main.java.models.User;
import org.xFlow.MovieRating.src.main.java.service.UserService;

public class UserController {
    private final UserService userService;

    UserController(UserService userService){
        this.userService = userService;
    }

    public void createUser(int id, String name){
        userService.create(id, name);
        System.out.println("user is created");
    }

    public User getUser(int id){
        return userService.getById(id);
    }

    public void rateMovie(int userId, int movieId, int rating) {
        userService.rate(userId, movieId,  rating);
    }
}
