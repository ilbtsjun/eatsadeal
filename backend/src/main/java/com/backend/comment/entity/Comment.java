package com.backend.comment.entity;

import com.backend.event.entity.Event;
import com.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table
@NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt = null;

    @Column
    private LocalDateTime deletedAt = null;

    @Column
    @Enumerated(EnumType.STRING)
    private CommentStatus commentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Builder
    public Comment(String content, User user, Event event){
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.commentStatus = CommentStatus.ACTIVE;
        this.user = user;
        this.event = event;
    }

    public void updateContent(String content){
        this.content = content;
        this.updatedAt = LocalDateTime.now();
        this.commentStatus = CommentStatus.MODIFIED;
    }

    public void delete(){
        this.deletedAt = LocalDateTime.now();
        this.commentStatus = CommentStatus.DELETED;
    }

    public void hide(){
        this.updatedAt = LocalDateTime.now();
        this.commentStatus = CommentStatus.HIDDEN;
    }

    public void unhide() {
        this.commentStatus = updatedAt == null
                ? CommentStatus.ACTIVE
                : CommentStatus.MODIFIED;
        this.updatedAt = LocalDateTime.now();
    }
}
