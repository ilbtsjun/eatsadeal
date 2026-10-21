package com.backend.event.entity;

import com.backend.event.dto.EventCode;
import com.backend.brand.entity.Brand;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

    @Column(nullable = false, length = 700, unique = true)
    private String url;

    @Column(nullable = false, length = 1000)
    private String img;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column(nullable = false)
    private boolean adminLocked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand", nullable = false)
    private Brand brand;

    @BatchSize(size = 100)
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
                 Brand brand, Boolean isActive, Set<EventCode> eventCodes) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.img = img;
        this.startDate = startDate;
        this.endDate = endDate;
        this.viewCount = 0L;
        this.isActive = isActive;
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

    public List<String> changedFields(String title, String description, String url,
                                      String img, LocalDateTime startDate, LocalDateTime endDate, Boolean isActive) {
        List<String> fields = new ArrayList<>();
        if (!Objects.equals(this.title, title)) fields.add("title");
        if (!Objects.equals(this.description, description)) fields.add("description");
        if (!Objects.equals(this.url, url)) fields.add("url");
        if (!Objects.equals(this.img, img)) fields.add("img");
        if (!Objects.equals(this.startDate, startDate)) fields.add("startDate");
        if (!Objects.equals(this.endDate, endDate)) fields.add("endDate(" + this.endDate + "->" + endDate + ")");
        if (!Objects.equals(this.isActive, isActive)) fields.add("isActive(" + this.isActive + "->" + isActive + ")");
        return fields;
    }

    public boolean update(String title, String description, String url,
                          String img, LocalDateTime startDate, LocalDateTime endDate , Boolean isActive){
        boolean changed = !Objects.equals(this.title, title)
                || !Objects.equals(this.description, description)
                || !Objects.equals(this.url, url)
                || !Objects.equals(this.img, img)
                || !Objects.equals(this.startDate, startDate)
                || !Objects.equals(this.endDate, endDate)
                || !Objects.equals(this.isActive, isActive);
        if (!changed) {
            return false;
        }
        this.title = title;
        this.description = description;
        this.url = url;
        this.img = img;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
        return true;
    }

    public boolean updateContent(String title, String description, String url, String img) {
        boolean changed = !Objects.equals(this.title, title)
                || !Objects.equals(this.description, description)
                || !Objects.equals(this.url, url)
                || !Objects.equals(this.img, img);
        if (!changed) {
            return false;
        }
        this.title = title;
        this.description = description;
        this.url = url;
        this.img = img;
        return true;
    }

    public boolean applyDefaultPeriod(int days, Boolean isActive) {
        if (endDate == null || !endDate.equals(startDate)) {
            return false;
        }
        this.endDate = startDate.plusDays(days);
        this.isActive = isActive;
        return true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void changeHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void changeAdminLocked(boolean adminLocked) {
        this.adminLocked = adminLocked;
    }

}
