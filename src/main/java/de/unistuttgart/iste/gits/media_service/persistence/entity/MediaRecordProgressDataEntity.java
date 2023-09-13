package de.unistuttgart.iste.gits.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity(name = "MediaRecordProgressData")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaRecordProgressDataEntity {

    @EmbeddedId
    private PrimaryKey primaryKey;

    @Column(nullable = false)
    private boolean workedOn;

    @Column(nullable = true)
    private OffsetDateTime workedOnDate;

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrimaryKey implements Serializable {
        private UUID mediaRecordId;
        private UUID userId;
    }
}
