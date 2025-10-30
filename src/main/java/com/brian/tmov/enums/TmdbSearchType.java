package com.brian.tmov.enums;

import java.util.Arrays;

public enum TmdbSearchType {

    MULTI("multi"),
    MOVIE("movie"),
    TV("tv"),
    PERSON("person");

    private final String value;
    TmdbSearchType(String value) { this.value = value; }
    public String value() { return value; }

    public static TmdbSearchType from(String s){
        if (s == null || s.isBlank()) return MULTI;
        String lower = s.toLowerCase();
        return Arrays.stream(values())
                .filter(t -> t.value.equals(lower))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("type 參數不正確，必須是：multi、movie、tv、person"));
    }
}
