package com.brian.tmov.controller;

import com.brian.tmov.dto.request.CommentRequest;
import com.brian.tmov.dto.response.CommentResponse;
import com.brian.tmov.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @Valid @RequestBody CommentRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(commentService.addComment(principal.getName(), request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        commentService.deleteComment(principal.getName(), commentId);
        return ResponseEntity.ok(Map.of("message", "留言已刪除"));
    }

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
