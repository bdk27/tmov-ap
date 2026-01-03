package com.brian.tmov.dto.request;

import com.brian.tmov.enums.MediaType;
import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(

        @NotNull(message = "TMDB ID 不能為空")
        Long tmdbId,

        @NotNull(message = "媒體類型不能為空 (movie 或 tv)")
        MediaType mediaType
) {
}
