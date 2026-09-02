package com.backend.favorite.entity;

import com.backend.event.entity.Event;
import com.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(
        name = "uk_brand_category",
        columnNames = {"user_id", "event_id"}
        )
    }
)
@NoArgsConstructor
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Builder
    public Favorite(User user, Event event){
        this.user = user;
        this.event = event;
    }
}
