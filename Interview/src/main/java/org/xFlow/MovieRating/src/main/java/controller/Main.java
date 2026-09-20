package org.xFlow.MovieRating.src.main.java.controller;



import org.xFlow.MovieRating.src.main.java.models.Genere;
import org.xFlow.MovieRating.src.main.java.service.MovieService;
import org.xFlow.MovieRating.src.main.java.service.RatingService;
import org.xFlow.MovieRating.src.main.java.service.UserService;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        RatingService ratingService = new RatingService();
        UserService userService = new UserService(ratingService);
        UserController userController = new UserController(userService);

        userController.createUser(1, "a");
        userController.createUser(2, "b");
        userController.createUser(3, "c");

        String name = userController.getUser(1).getName();
        System.out.println("fetched user succesfully " + name);

        MovieService movieService = new MovieService();
        MovieController movieController = new MovieController(movieService);

        movieController.create(1, "m1", "2018", List.of(Genere.ACTION));
        movieController.create(2, "m2", "2018", List.of(Genere.COMEDY));

        String movieName = movieService.getById(1).getName();
        System.out.println("fetched movie succesfully " + movieName);


        // Platform by default on-boards a user as a ‘viewer’.
        // Later a ‘viewer’ can be upgraded to a ‘critic’ after he/she published more than
        // 3 ratings for various movies.








    }
}
