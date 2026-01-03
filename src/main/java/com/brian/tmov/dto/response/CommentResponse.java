package com.brian.tmov.dto.response;

import com.brian.tmov.dao.entity.CommentEntity;

import java.time.LocalDateTime;

public record CommentResponse(

        Long commentId,

        String content,

        LocalDateTime createdAt,

        String memberName,

        String memberPicture,

        boolean isMyComment
) {
}
