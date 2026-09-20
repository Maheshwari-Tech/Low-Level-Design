package org.xFlow.MovieRating.src.main.java.models;

public class Rating {
    private final int movieId;
    private final  int userId;
    private final  int rating;
    private final int weight;

    public Rating(int movieId, int userId, int rating, int weight) {
        this.movieId = movieId;
        this.userId = userId;
        this.rating = rating;
        this.weight = weight;
    }

    public int getUserId() {
        return userId;
    }

    public int getRating() {
        return rating;
    }

    public int getMovieId() {
        return movieId;
    }

    public int getWeight() {
        return weight;
    }
}
