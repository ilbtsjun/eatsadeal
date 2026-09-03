package com.backend.event.entity;

import com.backend.event.dto.EventCode;
import com.backend.brand.entity.Brand;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column
    private String description;

    @Column(nullable = false, length = 1000, unique = true)
    private String url;

    @Column(nullable = false, length = 1000)
    private String img;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand", nullable = false)
    private Brand brand;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "event_event_codes",
            joinColumns = @JoinColumn(name = "event_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_event_event_code",
                            columnNames = {"event_id", "event_code"}
                    )
            }
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "event_code", nullable = false, length = 50)
    private Set<EventCode> eventCodes = new HashSet<>();

    @Builder
    public Event(String title, String description, String url,
                 String img, LocalDateTime startDate, LocalDateTime endDate,
                 Brand brand,
                 Set<EventCode> eventCodes) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.img = img;
        this.startDate = startDate;
        this.endDate = endDate;
        this.viewCount = 0L;
        this.isActive = true;
        this.brand = brand;
        this.eventCodes = eventCodes == null
                ? new HashSet<>()
                : new HashSet<>(eventCodes);
    }

    public void addEventCode(EventCode eventCode) {
        this.eventCodes.add(eventCode);
    }

    public void removeEventCode(EventCode eventCode) {
        this.eventCodes.remove(eventCode);
    }

    public void update(String title, String description, String url,
                       String img, LocalDateTime startDate, LocalDateTime endDate , Boolean isActive){
        this.title = title;
        this.description = description;
        this.url = url;
        this.img = img;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }

}
