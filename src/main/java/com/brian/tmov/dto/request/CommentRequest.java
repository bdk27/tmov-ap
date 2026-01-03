package com.brian.tmov.dto.request;

import com.brian.tmov.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentRequest(

        @NotNull(message = "TMDB ID 不能為空")
        Long tmdbId,

        @NotNull(message = "媒體類型不能為空")
        MediaType mediaType,

        @NotBlank(message = "留言內容不能為空")
        @Size(max = 1000, message = "留言內容過長 (最多1000字)")
        String content
) {
}
