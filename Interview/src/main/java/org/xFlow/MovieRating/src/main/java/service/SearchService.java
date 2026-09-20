package org.xFlow.MovieRating.src.main.java.service;


import org.xFlow.MovieRating.src.main.java.models.*;
import org.xFlow.MovieRating.src.main.java.models.Rating;
import org.xFlow.MovieRating.src.main.java.models.SearchCriteria;
import org.xFlow.MovieRating.src.main.java.models.SingleSearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchService implements SearchInterface{
    private final RatingService ratingService;
    private final MovieService movieService;
    private final UserService userService;

    public SearchService(RatingService ratingService, MovieService movieService, UserService userService) {
        this.ratingService = ratingService;
        this.movieService = movieService;
        this.userService = userService;
    }

    @Override
    public List<SingleSearchResult> search(SearchCriteria searchCriteria) {

        List<Rating> ratings = ratingService.getAllRatings();

        if(searchCriteria.getYearOfRelease() != null){
            ratings = ratings.stream().filter(r ->
                    haveYearOfRelease(r.getMovieId(),
                            searchCriteria.getYearOfRelease())).
                    collect(Collectors.toList());
        }

        if(searchCriteria.getGenre() != null){
            ratings = ratings.stream().filter(r ->
                            haveGenere(r.getMovieId(),
                                    searchCriteria.getGenre())).
                    collect(Collectors.toList());
        }

        if(searchCriteria.getUserLevel() != null){
            ratings = ratings.stream().filter(r ->
                            haveLevel(r.getUserId(),
                                    searchCriteria.getUserLevel())).
                    collect(Collectors.toList());
        }

        return aggregatedSearchResult(ratings);
    }
    public boolean haveYearOfRelease(int movieId, int expectedYoe){
        return movieService.getById(movieId).getReleaseYear().equals(expectedYoe);
    }

    public boolean haveGenere(int movieId, Genere expectedGen){
        return movieService.getById(movieId).getGenereList().contains(expectedGen);
    }

    public boolean haveLevel(int userId, UserLevels expectedLevel){
        return userService.getById(userId).getLevel().equals(expectedLevel);
    }

    public List<SingleSearchResult> aggregatedSearchResult(List<Rating> ratings){
        Map<Integer, List<Rating>> movieToRatingsMap = new HashMap<>();
        // create buckets of rating group of movie.
        for(Rating rating: ratings){
            movieToRatingsMap.computeIfAbsent(rating.getMovieId(), k-> new ArrayList<>())
                    .add(rating);
        }

        Map<Integer, SingleSearchResult> resultMap = new HashMap<>();
        // compute avg and total
        for(Map.Entry<Integer, List<Rating>> entry: movieToRatingsMap.entrySet()){
            double total = 0;
            int countUsers = 0;
            for(Rating rating: entry.getValue()){
                total += rating.getRating() * rating.getWeight();
                countUsers+=rating.getWeight();
            }
            if(countUsers == 0) continue;
            SingleSearchResult singleSearchResult = new SingleSearchResult(movieService.getById(entry.getKey()).getName()
                    , total / countUsers,countUsers);
            resultMap.put(entry.getKey(), singleSearchResult);
        }
        return resultMap.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
    }
}
