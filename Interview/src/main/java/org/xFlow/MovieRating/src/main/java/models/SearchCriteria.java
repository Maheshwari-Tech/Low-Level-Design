package org.xFlow.MovieRating.src.main.java.models;


public class SearchCriteria {
    private final Integer N;
    private final Integer yearOfRelease;
    private final Genere genre;
    private final UserLevels userLevel;

    public SearchCriteria(SearchCriteriaBuilder builder){
        this.N = builder.getN();
        this.yearOfRelease = builder.getYearOfRelease();
        this.genre = builder.getGenre();
        this.userLevel = builder.getUserLevel();
    }

    public Integer getYearOfRelease() {
        return yearOfRelease;
    }

    public UserLevels getUserLevel() {
        return userLevel;
    }

    public int getN() {
        return N;
    }

    public Genere getGenre() {
        return genre;
    }

    public class SearchCriteriaBuilder{
        private Integer N = null;
        private Integer yearOfRelease = null;
        Genere genre = null;
        UserLevels userLevel =  null;

        public int getN() {
            return N;
        }

        public Genere getGenre() {
            return genre;
        }

        public Integer getYearOfRelease() {
            return yearOfRelease;
        }

        public UserLevels getUserLevel() {
            return userLevel;
        }

        public SearchCriteriaBuilder n(int n) {
            this.N = n;
            return this;
        }

        public SearchCriteriaBuilder yearOfRelease(int yearOfRelease) {
           this.yearOfRelease = yearOfRelease;
           return this;
        }

        public SearchCriteriaBuilder userLevel(UserLevels userLevel) {
            this.userLevel = userLevel;
            return this;
        }
        public SearchCriteriaBuilder genre(Genere genre) {
            this.genre = genre;
            return this;
        }

        public SearchCriteria build(){
            return new SearchCriteria(this);
        }
    }

}
