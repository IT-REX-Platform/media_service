package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.media_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordProgressDataEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordProgressDataRepository;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaUserProgressDataServiceTest {

    private final MediaRecordProgressDataRepository mediaRecordProgressDataRepository = mock(MediaRecordProgressDataRepository.class);

    private final MediaService mediaService = mock(MediaService.class);

    private final TopicPublisher topicPublisher = mock(TopicPublisher.class);

    private final MediaUserProgressDataService mediaUserProgressDataService = new MediaUserProgressDataService(
            mediaRecordProgressDataRepository,
            mediaService,
            new ModelMapper(),
            topicPublisher);

    /**
     * Given that no progress data exists for the given media record and user
     * When getUserProgressData is called
     * Then a new progress data entity is created and returned
     */
    @Test
    void testGetUserProgressDataIsInitialized() {
        doReturn(Optional.empty()).when(mediaRecordProgressDataRepository).findById(any());
        doAnswer(returnsFirstArg()).when(mediaRecordProgressDataRepository).save(any());

        UUID userId = UUID.randomUUID();
        UUID mediaRecordId = UUID.randomUUID();

        MediaRecordProgressData actual = mediaUserProgressDataService.getUserProgressData(mediaRecordId, userId);

        assertThat(actual.getWorkedOn(), is(false));
        assertThat(actual.getDateWorkedOn(), is(nullValue()));

        MediaRecordProgressDataEntity expectedEntity = MediaRecordProgressDataEntity.builder()
                .primaryKey(new MediaRecordProgressDataEntity.PrimaryKey(mediaRecordId, userId))
                .workedOn(false)
                .build();
        verify(mediaRecordProgressDataRepository).save(expectedEntity);
    }

    /**
     * Given a single media record belonging to exactly on content id that is not yet worked on
     * When it is progressed
     * Then an event is published
     */
    @Test
    void testEventIsPublishedWhenSingleMediaOfContentIsProgressed() {
        UUID contentId = UUID.randomUUID();
        MediaRecord mediaRecord = MediaRecord.builder()
                .setId(UUID.randomUUID())
                .setContentIds(List.of(contentId))
                .setName("test")
                .setType(MediaType.AUDIO)
                .build();

        doReturn(mediaRecord).when(mediaService).getMediaRecordById(mediaRecord.getId());

        mockWorkedOnFor(mediaRecord, false);

        UUID userId = UUID.randomUUID();
        mediaUserProgressDataService
                .logMediaRecordWorkedOn(mediaRecord.getId(), userId);

        verify(topicPublisher, times(1)).notifyUserWorkedOnContent(UserProgressLogEvent.builder()
                .correctness(1.0)
                .timeToComplete(null)
                .success(true)
                .userId(userId)
                .contentId(contentId)
                .hintsUsed(0)
                .build());
    }

    /**
     * Given a media record that was already worked on before
     * When it is progressed again
     * Then no event is published
     */
    @Test
    void testNoEventPublishedIfMediaRecordWasAlreadyWorkedOnBefore() {
        UUID contentId = UUID.randomUUID();
        MediaRecord mediaRecord = MediaRecord.builder()
                .setId(UUID.randomUUID())
                .setContentIds(List.of(contentId))
                .setName("test")
                .setType(MediaType.AUDIO)
                .build();

        doReturn(mediaRecord).when(mediaService).getMediaRecordById(mediaRecord.getId());

        doReturn(Optional.of(MediaRecordProgressDataEntity.builder().workedOn(true).build()))
                .when(mediaRecordProgressDataRepository).findById(any());

        mediaUserProgressDataService.logMediaRecordWorkedOn(mediaRecord.getId(), UUID.randomUUID());

        verify(topicPublisher, never()).notifyUserWorkedOnContent(any());
    }

    /**
     * Given two media records that belong to the same content, both not worked on before
     * When one of them is progressed
     * Then no event is published
     */
    @Test
    void testEventIsPublishedOnlyWhenAllMediasOfContentAreWorkedOn() {
        UUID contentId = UUID.randomUUID();
        MediaRecord mediaRecord1 = MediaRecord.builder()
                .setId(UUID.randomUUID())
                .setContentIds(List.of(contentId))
                .setName("test")
                .setType(MediaType.AUDIO)
                .build();
        MediaRecord mediaRecord2 = MediaRecord.builder()
                .setId(UUID.randomUUID())
                .setContentIds(List.of(contentId))
                .setName("test")
                .setType(MediaType.AUDIO)
                .build();

        doReturn(mediaRecord1).when(mediaService).getMediaRecordById(mediaRecord1.getId());
        doReturn(mediaRecord2).when(mediaService).getMediaRecordById(mediaRecord2.getId());

        // this mocking is complicated because we need to mock the repository to return different values for each call
        // the first call is to gain the user progress data for mediaRecord1,
        // which we after that want to set to workedOn = true
        // the second call is when we iterate through the contents to get the user progress data for mediaRecord1,
        mockWorkedOnFor(mediaRecord1, false, true);

        // seconds media record is not worked on yet
        mockWorkedOnFor(mediaRecord2, false);

        doReturn(List.of(dtoToEntity(mediaRecord1), dtoToEntity(mediaRecord2)))
                .when(mediaService)
                .getMediaRecordEntitiesByContentId(contentId);

        mediaUserProgressDataService
                .logMediaRecordWorkedOn(mediaRecord1.getId(), UUID.randomUUID());

        // no event is published because not all medias are worked on
        verify(topicPublisher, never()).notifyUserWorkedOnContent(any());
    }

    private MediaRecordEntity dtoToEntity(MediaRecord mediaRecord) {
        return new ModelMapper().map(mediaRecord, MediaRecordEntity.class);
    }

    /**
     * Mocks the repository to return the given workedOn value for the given media record
     *
     * @param record   the media record to mock the repository for
     * @param workedOn the values to return for the call to the repository
     */
    @SuppressWarnings("SameParameterValue")
    private void mockWorkedOnFor(MediaRecord record, Boolean workedOn) {
        var mockReturnValue = Optional.of(MediaRecordProgressDataEntity.builder().workedOn(workedOn).build());
        doReturn(mockReturnValue)
                .when(mediaRecordProgressDataRepository)
                .findById(argThat(arg -> arg.getMediaRecordId().equals(record.getId())));
    }

    /**
     * Mock work on for the given media record for two calls to the repository
     */
    @SuppressWarnings("SameParameterValue")
    private void mockWorkedOnFor(MediaRecord record, Boolean workedOnFirst, Boolean workedOnSecond) {
        var mockReturnValue1 = Optional.of(MediaRecordProgressDataEntity.builder().workedOn(workedOnFirst).build());
        var mockReturnValue2 = Optional.of(MediaRecordProgressDataEntity.builder().workedOn(workedOnSecond).build());
        doReturn(mockReturnValue1, mockReturnValue2)
                .when(mediaRecordProgressDataRepository)
                .findById(argThat(arg -> arg.getMediaRecordId().equals(record.getId())));
    }
}
