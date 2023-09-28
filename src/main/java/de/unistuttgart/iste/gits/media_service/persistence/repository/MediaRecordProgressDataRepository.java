package de.unistuttgart.iste.gits.media_service.persistence.repository;

import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordProgressDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRecordProgressDataRepository
        extends JpaRepository<MediaRecordProgressDataEntity, MediaRecordProgressDataEntity.PrimaryKey> {

}
