package de.unistuttgart.iste.gits.media_service.test_util;

import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;

import java.util.*;

public class MediaRecordRepositoryUtil {

    public static final UUID creator1Id = UUID.randomUUID();
    public static final UUID creator2Id = UUID.randomUUID();

    /**
     * Helper method to fill the repository with example media records.
     * @return The list of media records that were added to the repository.
     */
    public static List<MediaRecordEntity> fillRepositoryWithMediaRecords(final MediaRecordRepository repository) {
        final List<MediaRecordEntity> expectedMediaRecords = List.of(
                MediaRecordEntity.builder()
                        .name("Example Record1")
                        .courseIds(Collections.emptyList())
                        .creatorId(creator1Id)
                        .type(MediaRecordEntity.MediaType.DOCUMENT)
                        .contentIds(new ArrayList<>(List.of(UUID.randomUUID())))
                        .build(),
                MediaRecordEntity.builder()
                        .name("Example Record2")
                        .courseIds(Collections.emptyList())
                        .creatorId(creator2Id)
                        .type(MediaRecordEntity.MediaType.PRESENTATION)
                        .contentIds(new ArrayList<>(List.of(UUID.randomUUID(), UUID.randomUUID())))
                        .build()
        );

        return repository.saveAll(expectedMediaRecords);
    }
}
