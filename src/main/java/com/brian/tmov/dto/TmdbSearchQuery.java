package com.brian.tmov.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TmdbSearchQuery(
        @NotBlank(message = "請輸入關鍵字")
        String q,

        @Min(value = 1, message = "頁數必須大於 0")
        Integer page,

        String type,

        String language,

        String region,

        Boolean includeAdult,

        Integer year,

        Integer firstAirDateYear
) {
    public String typeOrDefault() {
        return (type == null || type.isBlank()) ? "multi" : type;
    }

    public Integer pageOrDefault() {
        return (page == null || page < 1) ? 1 : page;
    }

    public String languageOrDefault(String defaultLang) {
        return (language == null || language.isBlank()) ? defaultLang : language;
    }

    public String regionOrDefault(String defaultRegion) {
        return (region == null || region.isBlank()) ? defaultRegion : region;
    }
}
