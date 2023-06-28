package de.unistuttgart.iste.gits.media_service.persistence.repository;

import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaRecordRepository extends JpaRepository<MediaRecordEntity, UUID> {

    // Query annotation is necessary to be able to match a list against another
    @Query("SELECT DISTINCT media FROM MediaRecord media JOIN media.contentIds actualIds WHERE actualIds IN :contentIds")
    List<MediaRecordEntity> findMediaRecordEntitiesByContentIds(List<UUID> contentIds);
}
