package de.unistuttgart.iste.gits.media_service.persistence.dao;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "MediaRecord")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    @NotNull(message = "Name must not be null")
    @Length(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    private String name;

    @NotNull(message = "Creator ID must not be null")
    private UUID creatorId;

    @Enumerated(EnumType.ORDINAL)
    private MediaType type;

    @ElementCollection
    private List<UUID> contentIds;

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
