package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.MediaRecord;
import de.unistuttgart.iste.gits.generated.dto.MediaRecordProgressData;
import de.unistuttgart.iste.gits.media_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordProgressDataEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordProgressDataRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaUserProgressDataService {

    private final MediaRecordProgressDataRepository mediaRecordProgressDataRepository;
    private final MediaService mediaService;
    private final ModelMapper modelMapper;
    private final TopicPublisher topicPublisher;

    public MediaRecordProgressData getUserProgressData(UUID mediaRecordId, UUID userId) {
        var entity = getUserProgressDataEntity(mediaRecordId, userId);
        return mapToDto(entity);
    }

    public MediaRecordProgressDataEntity getUserProgressDataEntity(UUID mediaRecordId, UUID userId) {
        var primaryKey = new MediaRecordProgressDataEntity.PrimaryKey(mediaRecordId, userId);
        return mediaRecordProgressDataRepository.findById(primaryKey)
                .orElseGet(() -> initializeProgressData(mediaRecordId, userId));
    }

    /**
     * Initializes the progress data for the given media record and user.
     *
     * @param mediaRecordId The media record id
     * @param userId        The user id
     * @return The media record progress data entity, initialized with the given ids and the worked on flag set to false
     */
    public MediaRecordProgressDataEntity initializeProgressData(UUID mediaRecordId, UUID userId) {
        var primaryKey = new MediaRecordProgressDataEntity.PrimaryKey(mediaRecordId, userId);
        var progressData = MediaRecordProgressDataEntity.builder()
                .primaryKey(primaryKey)
                .workedOn(false)
                .build();
        return mediaRecordProgressDataRepository.save(progressData);
    }

    /**
     * Logs that the user has worked on the given media record.
     *
     * @param mediaRecordId The media record id
     * @param userId        The user id
     * @return The media record
     */
    public MediaRecord logMediaRecordWorkedOn(UUID mediaRecordId, UUID userId) {
        var mediaRecord = mediaService.getMediaRecordById(mediaRecordId);
        var progressData = getUserProgressDataEntity(mediaRecordId, userId);

        boolean wasAlreadyWorkedOnBefore = progressData.isWorkedOn();
        updateProgressDataEntity(progressData);

        // prevent multiple triggers of the content learned event
        if (!wasAlreadyWorkedOnBefore) {
            publishProgressEventsIfContentsAreCompletelyWorkedOn(userId, mediaRecord);
        }

        return mediaRecord;
    }

    private void updateProgressDataEntity(MediaRecordProgressDataEntity progressData) {
        progressData.setWorkedOn(true);
        progressData.setWorkedOnDate(OffsetDateTime.now());
        mediaRecordProgressDataRepository.save(progressData);
    }

    /**
     * Iterates through all the associated content ids of the given media record and checks if
     * each are completely worked on, i.e, if all media records associated with the content id
     * are worked on.
     * If so, a user progress event is published for each of them.
     *
     * @param userId      The user id
     * @param mediaRecord The media record
     */
    private void publishProgressEventsIfContentsAreCompletelyWorkedOn(UUID userId, MediaRecord mediaRecord) {
        List<UUID> associatedContentIds = mediaRecord.getContentIds();

        for (var contentId : associatedContentIds) {
            List<MediaRecordEntity> associatedMediaRecords = mediaService.getMediaRecordEntitiesByContentId(contentId);

            boolean allAssociatedMediaRecordsWorkedOn = associatedMediaRecords.stream()
                    .map(mediaRecordEntity -> getUserProgressDataEntity(mediaRecordEntity.getId(), userId))
                    .allMatch(MediaRecordProgressDataEntity::isWorkedOn);

            if (allAssociatedMediaRecordsWorkedOn) {
                publishUserProgressEvent(userId, contentId);
            }
        }
    }

    private void publishUserProgressEvent(UUID userId, UUID contentId) {
        topicPublisher.notifyUserWorkedOnContent(
                UserProgressLogEvent.builder()
                        .userId(userId)
                        .contentId(contentId)
                        .hintsUsed(0)
                        .success(true)
                        .timeToComplete(null)
                        .correctness(1.0)
                        .build()
        );
    }

    private MediaRecordProgressData mapToDto(MediaRecordProgressDataEntity entity) {
        return modelMapper.map(entity, MediaRecordProgressData.class);
    }
}
