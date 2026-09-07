package com.backend.comment.service;

import com.backend.auth.service.CurrentUserService;
import com.backend.comment.entity.CommentStatus;
import com.backend.comment.dto.CreateComment;
import com.backend.comment.dto.CommentResponse;
import com.backend.comment.dto.UpdateComment;
import com.backend.comment.entity.Comment;
import com.backend.comment.repository.CommentRepository;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CurrentUserService currentUserService;
    private final EventRepository eventRepository;

    @Transactional
    public CommentResponse createComment(Long eventId, CreateComment request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        User user = currentUserService.getRequiredUser();

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
    public CommentResponse updateComment(Long commentId, UpdateComment request){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        User user = currentUserService.getRequiredUser();
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
    public List<CommentResponse> getEventCommentList(Long eventId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        User user = currentUserService.getOptionalUser();
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
    public List<CommentResponse> getMyCommentList(){
        User user = currentUserService.getRequiredUser();

        List<Comment> eventCommentList = commentRepository.findByUser(user);
        List<CommentResponse> commentList = new ArrayList<>();
        for(Comment comment : eventCommentList){
            commentList.add(CommentResponse.from(comment, user.getId()));
        }
        return commentList;
    }

    @Transactional
    public void deleteComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        User user = currentUserService.getRequiredUser();

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
}
