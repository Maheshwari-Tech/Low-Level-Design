package org.xFlow.MovieRating.src.main.java.models;

public class SingleSearchResult {
    private  final String movieName;
    private  final   double avgRating;
    private  final  int totalUsersRated;

    public SingleSearchResult(String movieName, double avgRating, int totalUsersRated) {
        this.movieName = movieName;
        this.avgRating = avgRating;
        this.totalUsersRated = totalUsersRated;
    }
}
