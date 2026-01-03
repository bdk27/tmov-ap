package com.brian.tmov.service;

import com.brian.tmov.dto.request.CommentRequest;
import com.brian.tmov.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    // 新增留言
    CommentResponse addComment(String email, CommentRequest request);

    // 刪除留言 (需檢查是否為本人)
    void deleteComment(String email, Long commentId);

    // 取得留言列表 (currentUserEmail 用來判斷 isMyComment)
    List<CommentResponse> getComments(Long tmdbId, String mediaTypeStr, String currentUserEmail);
}
