package com.backend.comment.repository;

import com.backend.comment.entity.Comment;
import com.backend.comment.entity.CommentStatus;
import com.backend.event.entity.Event;
import com.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByEvent(Event event);
    List<Comment> findByUser(User user);

    long countByCommentStatusNot(CommentStatus commentStatus);

    long countByCommentStatusNotAndCreatedAtGreaterThanEqual(CommentStatus commentStatus, LocalDateTime since);
}
