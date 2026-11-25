package model;

/** Anything that can be matched against a free-text search query. */
public interface Searchable {
    boolean matchesKeyword(String keyword);
}
