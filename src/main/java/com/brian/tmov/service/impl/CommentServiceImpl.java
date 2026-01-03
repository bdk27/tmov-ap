package com.brian.tmov.service.impl;

import com.brian.tmov.dao.entity.CommentEntity;
import com.brian.tmov.dao.entity.MemberEntity;
import com.brian.tmov.dao.repository.CommentRepository;
import com.brian.tmov.dao.repository.MemberRepository;
import com.brian.tmov.dto.request.CommentRequest;
import com.brian.tmov.dto.response.CommentResponse;
import com.brian.tmov.enums.MediaType;
import com.brian.tmov.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Override
    @Transactional
    public CommentResponse addComment(String email, CommentRequest request) {
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("會員不存在"));

        CommentEntity comment = new CommentEntity(
                member,
                request.tmdbId(),
                request.mediaType(),
                request.content()
        );

        CommentEntity savedComment = commentRepository.save(comment);

        return new CommentResponse(
                savedComment.getId(),
                savedComment.getContent(),
                savedComment.getCreatedAt(),
                member.getDisplayName(),
                member.getPictureUrl(),
                true
        );
    }

    @Override
    @Transactional
    public void deleteComment(String email, Long commentId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("留言不存在"));

        // 安全檢查：確認刪除者是留言擁有者
        if (!comment.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("您無權刪除此留言");
        }

        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long tmdbId, String mediaTypeStr, String currentUserEmail) {
        try {
            MediaType type = MediaType.valueOf(mediaTypeStr.toLowerCase());
            List<CommentEntity> comments = commentRepository.find(tmdbId, type);

            return comments.stream().map(c -> new CommentResponse(
                    c.getId(),
                    c.getContent(),
                    c.getCreatedAt(),
                    c.getMember().getDisplayName(),
                    c.getMember().getPictureUrl(),
                    // 判斷這則留言是否為「當前登入者」發的
                    currentUserEmail != null && currentUserEmail.equals(c.getMember().getEmail())
            )).collect(Collectors.toList());

        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }
}
