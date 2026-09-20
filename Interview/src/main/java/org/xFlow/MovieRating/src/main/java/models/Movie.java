package org.xFlow.MovieRating.src.main.java.models;

import java.util.List;

public class Movie {
    private final int id;
    private final String name;
    private final String releaseYear;

    private final List<Genere> genereList;

    public Movie(int id, String name, String releaseYear, List<Genere> genereList) {
        this.id = id;
        this.name = name;
        this.releaseYear = releaseYear;
        this.genereList = genereList;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Genere> getGenereList() {
        return genereList;
    }
}
