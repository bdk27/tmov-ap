package com.brian.tmov.controller;

import com.brian.tmov.dto.request.CommentRequest;
import com.brian.tmov.dto.response.CommentResponse;
import com.brian.tmov.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Tag(name = "留言功能", description = "針對電影/劇集的討論區")
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Operation(summary = "新增留言", description = "發布一則新留言")
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @Valid @RequestBody CommentRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(commentService.addComment(principal.getName(), request));
    }

    @Operation(summary = "刪除留言", description = "刪除自己的留言")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        commentService.deleteComment(principal.getName(), commentId);
        return ResponseEntity.ok(Map.of("message", "留言已刪除"));
    }

    @Operation(summary = "取得留言列表", description = "取得該項目的所有留言")
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @RequestParam Long tmdbId,
            @RequestParam String mediaType,
            Principal principal // 可為 null (若未登入)
    ) {
        String email = (principal != null) ? principal.getName() : null;
        return ResponseEntity.ok(commentService.getComments(tmdbId, mediaType, email));
    }
}
