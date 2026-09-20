package org.xFlow.MovieRating.src.main.java.service;

import org.xFlow.MovieRating.src.main.java.models.SearchCriteria;
import org.xFlow.MovieRating.src.main.java.models.SingleSearchResult;

import java.util.List;

public interface SearchInterface {
    List<SingleSearchResult> search(SearchCriteria searchCriteria);
}
