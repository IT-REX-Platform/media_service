package de.unistuttgart.iste.gits.media_service.persistence.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
