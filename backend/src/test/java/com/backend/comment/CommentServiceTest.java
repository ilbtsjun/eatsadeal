package com.backend.comment;

import com.backend.comment.entity.Comment;
import com.backend.comment.entity.CommentStatus;
import com.backend.event.entity.Event;
import com.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CommentServiceTests {

    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        event = mock(Event.class);
    }

    private Comment newComment() {
        return Comment.builder()
                .content("원본 댓글")
                .user(user)
                .event(event)
                .build();
    }

    private Comment editedComment() {
        Comment comment = newComment();
        comment.updateContent("수정된 댓글");
        return comment;
    }

    private void assertBetween(LocalDateTime before, LocalDateTime actual, LocalDateTime after) {
        assertNotNull(actual);
        assertFalse(actual.isBefore(before), "기록 시각이 호출 이전이면 안 된다: " + actual + " < " + before);
        assertFalse(actual.isAfter(after), "기록 시각이 호출 이후이면 안 된다: " + actual + " > " + after);
    }

    @Nested
    @DisplayName("생성 (builder)")
    class CreateTests {

        @Test
        @DisplayName("생성 시 요청값이 담기고, 상태는 ACTIVE, 생성 시각만 기록되며 수정/삭제 시각은 null이다")
        void initialState() {
            LocalDateTime before = LocalDateTime.now();

            Comment comment = newComment();

            LocalDateTime after = LocalDateTime.now();
            assertEquals("원본 댓글", comment.getContent());
            assertSame(user, comment.getUser());
            assertSame(event, comment.getEvent());
            assertEquals(CommentStatus.ACTIVE, comment.getCommentStatus());
            assertBetween(before, comment.getCreatedAt(), after);
            assertNull(comment.getUpdatedAt());
            assertNull(comment.getDeletedAt());
            assertNull(comment.getCommentId(), "ID는 저장 시점에 DB가 부여한다");
        }
    }

    @Nested
    @DisplayName("updateContent")
    class UpdateContentTests {

        @Test
        @DisplayName("내용이 바뀌고 상태는 MODIFIED, 수정 시각이 기록된다. 생성 시각/삭제 시각은 그대로")
        void updatesContentAndStatus() {
            Comment comment = newComment();
            LocalDateTime createdAt = comment.getCreatedAt();
            LocalDateTime before = LocalDateTime.now();

            comment.updateContent("수정된 댓글");

            LocalDateTime after = LocalDateTime.now();
            assertEquals("수정된 댓글", comment.getContent());
            assertEquals(CommentStatus.MODIFIED, comment.getCommentStatus());
            assertBetween(before, comment.getUpdatedAt(), after);
            assertEquals(createdAt, comment.getCreatedAt());
            assertNull(comment.getDeletedAt());
        }

        @Test
        @DisplayName("여러 번 수정해도 상태는 MODIFIED로 유지되고 마지막 내용이 남는다")
        void updatesRepeatedly() {
            Comment comment = newComment();

            comment.updateContent("첫 번째 수정");
            comment.updateContent("두 번째 수정");

            assertEquals("두 번째 수정", comment.getContent());
            assertEquals(CommentStatus.MODIFIED, comment.getCommentStatus());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("상태는 DELETED, 삭제 시각이 기록된다. 내용은 그대로 남는다")
        void softDeletes() {
            Comment comment = newComment();
            LocalDateTime before = LocalDateTime.now();

            comment.delete();

            LocalDateTime after = LocalDateTime.now();
            assertEquals(CommentStatus.DELETED, comment.getCommentStatus());
            assertBetween(before, comment.getDeletedAt(), after);
            assertEquals("원본 댓글", comment.getContent());
        }

        @Test
        @DisplayName("수정 시각은 삭제로 바뀌지 않는다")
        void doesNotTouchUpdatedAt() {
            Comment comment = editedComment();
            LocalDateTime updatedAt = comment.getUpdatedAt();

            comment.delete();

            assertEquals(updatedAt, comment.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("hide")
    class HideTests {

        @Test
        @DisplayName("상태는 HIDDEN이 되고 내용/삭제 시각은 그대로다")
        void hides() {
            Comment comment = newComment();

            comment.hide();

            assertEquals(CommentStatus.HIDDEN, comment.getCommentStatus());
            assertEquals("원본 댓글", comment.getContent());
            assertNull(comment.getDeletedAt());
        }

        @Test
        @DisplayName("숨김은 '내용 수정'이 아니므로 수정 시각을 바꾸지 않는다")
        void doesNotTouchUpdatedAt() {
            Comment untouched = newComment();
            Comment edited = editedComment();
            LocalDateTime editedAt = edited.getUpdatedAt();

            untouched.hide();
            edited.hide();

            assertNull(untouched.getUpdatedAt());
            assertEquals(editedAt, edited.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("unhide")
    class UnhideTests {

        @Test
        @DisplayName("한 번도 수정하지 않은 댓글은 숨김 해제 후 ACTIVE로 돌아간다")
        void restoresActiveWhenNeverEdited() {
            Comment comment = newComment();
            comment.hide();

            comment.unhide();

            assertEquals(CommentStatus.ACTIVE, comment.getCommentStatus());
        }

        @Test
        @DisplayName("수정한 적 있는 댓글은 숨김 해제 후 MODIFIED로 돌아간다")
        void restoresModifiedWhenEdited() {
            Comment comment = editedComment();
            comment.hide();

            comment.unhide();

            assertEquals(CommentStatus.MODIFIED, comment.getCommentStatus());
        }

        @Test
        @DisplayName("숨김 해제는 수정 시각을 바꾸지 않는다")
        void doesNotTouchUpdatedAt() {
            Comment untouched = newComment();
            Comment edited = editedComment();
            LocalDateTime editedAt = edited.getUpdatedAt();
            untouched.hide();
            edited.hide();

            untouched.unhide();
            edited.unhide();

            assertNull(untouched.getUpdatedAt());
            assertEquals(editedAt, edited.getUpdatedAt());
        }

        @Test
        @DisplayName("숨김 → 해제를 반복해도 ACTIVE/MODIFIED 판정이 흔들리지 않는다")
        void repeatedHideUnhideIsStable() {
            Comment comment = newComment();

            comment.hide();
            comment.unhide();
            comment.hide();
            comment.unhide();

            assertEquals(CommentStatus.ACTIVE, comment.getCommentStatus());
        }
    }
}