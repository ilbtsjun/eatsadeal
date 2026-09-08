package com.backend.comment.service;

import com.backend.auth.service.CurrentUserService;
import com.backend.comment.entity.CommentStatus;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.event.dto.CreateComment;
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
    public CommentResponse updateComment(Long commentId, UpdateComment request){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        User user = currentUserService.getRequiredUser();
        if(!user.getId().equals(comment.getUser().getId())){
            throw new BusinessException(ErrorCode.NOT_OWNED);
        }

        if(comment.getCommentStatus() == CommentStatus.HIDDEN || comment.getCommentStatus() == CommentStatus.DELETED){
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        String content = request.content().trim();

        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        comment.updateContent(content);
        return CommentResponse.from(comment, comment.getUser().getId());
    }

    @Transactional
    public void deleteComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        User user = currentUserService.getRequiredUser();

        if(!user.getId().equals(comment.getUser().getId())){
            throw new BusinessException(ErrorCode.NOT_OWNED);
        }

        if (comment.getCommentStatus() == CommentStatus.HIDDEN || comment.getCommentStatus() == CommentStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        comment.delete();
    }

    @Transactional
    public void hideComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (comment.getCommentStatus() == CommentStatus.HIDDEN || comment.getCommentStatus() == CommentStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        comment.hide();
    }

    @Transactional
    public void unhideComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (comment.getCommentStatus() == CommentStatus.ACTIVE || comment.getCommentStatus() == CommentStatus.MODIFIED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        comment.unhide();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getEventCommentList(Long eventId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

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

    @Transactional
    public CommentResponse createComment(Long eventId, CreateComment request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        User user = currentUserService.getRequiredUser();

        String content = request.content().trim();

        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Comment comment = Comment.builder()
                .content(content)
                .user(user)
                .event(event)
                .build();
        commentRepository.save(comment);

        return CommentResponse.from(comment, comment.getUser().getId());
    }
}
