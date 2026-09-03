package com.backend.comment.service;

import com.backend.comment.entity.CommentStatus;
import com.backend.comment.dto.CreateComment;
import com.backend.comment.dto.CommentResponse;
import com.backend.comment.dto.UpdateComment;
import com.backend.comment.entity.Comment;
import com.backend.comment.repository.CommentRepository;
import com.backend.config.JwtTokenProvider;
import com.backend.config.TokenBlacklist;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.user.dto.UserStatus;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;

    @Transactional
    public CommentResponse createComment(String token, Long eventId, CreateComment request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        User user = findUserByToken(token);

        String content = request.content().trim();

        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("댓글 내용은 비어 있을 수 없습니다.");
        }

        Comment comment = Comment.builder()
                .content(content)
                .user(user)
                .event(event)
                .build();
        commentRepository.save(comment);

        return CommentResponse.from(comment, comment.getUser().getId());
    }

    @Transactional
    public CommentResponse updateComment(String token, Long commentId, UpdateComment request){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        User user = findUserByToken(token);
        if(!user.getId().equals(comment.getUser().getId())){
            throw new IllegalArgumentException("자신의 댓글만 수정할 수 있습니다.");
        }

        if(comment.getCommentStatus() == CommentStatus.HIDDEN || comment.getCommentStatus() == CommentStatus.DELETED){
            throw new IllegalArgumentException("삭제되었거나 숨김 처리된 댓글은 수정할 수 없습니다.");
        }

        String content = request.content().trim();

        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("댓글 내용은 비어 있을 수 없습니다.");
        }

        comment.updateContent(content);
        return CommentResponse.from(comment, comment.getUser().getId());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getEventCommentList(String token, Long eventId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        User user = null;
        if (StringUtils.hasText(token)) {
            user = findUserByToken(token);
        }
        Long userId = user == null
                ? null
                : user.getId();

        List<Comment> eventCommentList = commentRepository.findByEvent(event);
        List<CommentResponse> commentList = new ArrayList<>();
        for(Comment comment : eventCommentList){
            commentList.add(CommentResponse.from(comment, userId));
        }
        return commentList;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getMyCommentList(String token){
        User user = findUserByToken(token);

        List<Comment> eventCommentList = commentRepository.findByUser(user);
        List<CommentResponse> commentList = new ArrayList<>();
        for(Comment comment : eventCommentList){
            commentList.add(CommentResponse.from(comment, user.getId()));
        }
        return commentList;
    }

    @Transactional
    public void deleteComment(String token, Long commentId){
        Comment comment = commentRepository.findById(commentId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        User user = findUserByToken(token);

        if(!user.getId().equals(comment.getUser().getId())){
            throw new IllegalArgumentException("자신의 댓글만 삭제할 수 있습니다.");
        }

        if (comment.getCommentStatus() == CommentStatus.HIDDEN || comment.getCommentStatus() == CommentStatus.DELETED) {
            throw new IllegalArgumentException("이미 숨김되거나 삭제된 댓글입니다.");
        }

        comment.delete();
    }

    @Transactional
    public void hideComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        if (comment.getCommentStatus() == CommentStatus.HIDDEN || comment.getCommentStatus() == CommentStatus.DELETED) {
            throw new IllegalArgumentException("이미 숨김되거나 삭제된 댓글입니다.");
        }

        comment.hide();
    }

    @Transactional
    public void unhideComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        if (comment.getCommentStatus() == CommentStatus.ACTIVE || comment.getCommentStatus() == CommentStatus.MODIFIED) {
            throw new IllegalArgumentException("이미 보여지는 상태입니다.");
        }

        comment.unhide();
    }

    private User findUserByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰은 필수입니다.");
        }
        if (tokenBlacklist.contains(token)) {
            throw new IllegalArgumentException("이미 로그아웃된 토큰입니다.");
        }
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
            }
            Long userId = Long.valueOf(jwtTokenProvider.getSubject(token));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            user.releaseIfExpired(LocalDateTime.now());
            if(user.getUserStatus().equals(UserStatus.ACTIVE)){
                return user;
            }
            throw new IllegalArgumentException("정지되거나 탈퇴한 사용자입니다.");
        } catch (JwtException | NumberFormatException ex) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }
}
