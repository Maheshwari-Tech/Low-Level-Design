package org.xFlow.MovieRating.src.main.java.models;

import java.util.List;
import java.util.Map;

import static org.xFlow.MovieRating.src.main.java.models.UserLevels.Critic;
import static org.xFlow.MovieRating.src.main.java.models.UserLevels.Viewer;

public class User {
    private final int id;
    private final String name;
    private UserLevels level;
    private int ratedMovies;
    private static List<Integer> LIMITS = List.of(4, 10);
    private static Map<UserLevels, Integer> WEIGHT = Map.of(Viewer,1, Critic, 2);

    public User(int id, String name) {
        this.id = id;
        this.name = name;
        this.level = Viewer;
        this.ratedMovies = 0;
    }

    public UserLevels getLevel() {
        return level;
    }

    public void setLevel(UserLevels level) {
        this.level = level;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public int rateMovie(){
        ratedMovies++;
        if(LIMITS.contains(ratedMovies)){
            upgradeLevel();
        }
        return WEIGHT.get(getLevel());
    }

    public void upgradeLevel(){
        if(level == Viewer){
            level = Critic;
        }
    }
}
