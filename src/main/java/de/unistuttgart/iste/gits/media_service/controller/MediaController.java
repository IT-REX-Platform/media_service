package de.unistuttgart.iste.gits.media_service.controller;

import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import de.unistuttgart.iste.gits.media_service.service.MediaUserProgressDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

/**
 * Implements the graphql endpoints of the service.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final MediaUserProgressDataService mediaUserProgressDataService;

    @QueryMapping
    public List<MediaRecord> mediaRecords() {
        return mediaService.getAllMediaRecords();
    }

    @QueryMapping
    public List<MediaRecord> mediaRecordsById(@Argument List<UUID> ids) {
        return mediaService.getMediaRecordsById(ids);
    }

    @QueryMapping
    List<List<MediaRecord>> mediaRecordsByContentIds(@Argument List<UUID> contentIds) {
        return mediaService.getMediaRecordsByContentIds(contentIds);
    }

    @SchemaMapping(typeName = "MediaRecord", field = "userProgressData")
    public MediaRecordProgressData userProgressData(MediaRecord mediaRecord, @Argument UUID userId) {
        return mediaUserProgressDataService.getUserProgressData(mediaRecord.getId(), userId);
    }

    @MutationMapping
    public MediaRecord createMediaRecord(@Argument CreateMediaRecordInput input) {
        return mediaService.createMediaRecord(input);
    }

    @MutationMapping
    public UUID deleteMediaRecord(@Argument UUID id) {
        return mediaService.deleteMediaRecord(id);
    }

    @MutationMapping
    public MediaRecord updateMediaRecord(@Argument UpdateMediaRecordInput input) {
        return mediaService.updateMediaRecord(input);
    }

    @MutationMapping
    public UploadUrl createStorageUploadUrl(@Argument CreateUrlInput input) {
        return mediaService.createUploadUrl(input);
    }

    @MutationMapping
    public DownloadUrl createStorageDownloadUrl(@Argument CreateUrlInput input) {
        return mediaService.createDownloadUrl(input);
    }

    @MutationMapping
    public MediaRecord logMediaRecordWorkedOn(@Argument UUID mediaRecordId, @Argument UUID userId) {
        return mediaUserProgressDataService.logMediaRecordWorkedOn(mediaRecordId, userId);
    }
}
