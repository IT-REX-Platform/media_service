package de.unistuttgart.iste.gits.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity(name = "MediaRecord")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID creatorId;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private MediaType type;

    @ElementCollection
    private List<UUID> contentIds;

    @Column(length = 500)
    private String uploadUrl;

    @Column(length = 500)
    private String downloadUrl;

    @OneToMany(mappedBy = "primaryKey.mediaRecordId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    private List<MediaRecordProgressDataEntity> progressData = new ArrayList<>();

    public enum MediaType {
        AUDIO,
        VIDEO,
        IMAGE,
        PRESENTATION,
        DOCUMENT,
        URL
    }
}
