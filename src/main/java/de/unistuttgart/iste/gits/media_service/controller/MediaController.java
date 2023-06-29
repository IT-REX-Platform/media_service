package de.unistuttgart.iste.gits.media_service.controller;

import de.unistuttgart.iste.gits.generated.dto.CreateMediaRecordInput;
import de.unistuttgart.iste.gits.generated.dto.MediaRecord;
import de.unistuttgart.iste.gits.generated.dto.MediaRecordProgressData;
import de.unistuttgart.iste.gits.generated.dto.UpdateMediaRecordInput;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import de.unistuttgart.iste.gits.media_service.service.MediaUserProgressDataService;
import graphql.schema.DataFetchingEnvironment;
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
    public List<MediaRecord> mediaRecords(DataFetchingEnvironment env) {
        return mediaService.getAllMediaRecords(
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<MediaRecord> mediaRecordsById(@Argument List<UUID> ids, DataFetchingEnvironment env) {
        return mediaService.getMediaRecordsById(
                ids,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    List<List<MediaRecord>> mediaRecordsByContentIds(@Argument List<UUID> contentIds, DataFetchingEnvironment env) {
        return mediaService.getMediaRecordsByContentIds(
                contentIds,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @SchemaMapping(typeName = "MediaRecord", field = "userProgressData")
    public MediaRecordProgressData userProgressData(MediaRecord mediaRecord, @Argument UUID userId) {
        return mediaUserProgressDataService.getUserProgressData(mediaRecord.getId(), userId);
    }

    @MutationMapping
    public MediaRecord createMediaRecord(@Argument CreateMediaRecordInput input, DataFetchingEnvironment env) {
        return mediaService.createMediaRecord(
                input,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @MutationMapping
    public UUID deleteMediaRecord(@Argument UUID id) {
        return mediaService.deleteMediaRecord(id);
    }

    @MutationMapping
    public MediaRecord updateMediaRecord(@Argument UpdateMediaRecordInput input, DataFetchingEnvironment env) {
        return mediaService.updateMediaRecord(
                input,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @MutationMapping
    public MediaRecord logMediaRecordWorkedOn(@Argument UUID mediaRecordId, @Argument UUID userId) {
        return mediaUserProgressDataService.logMediaRecordWorkedOn(mediaRecordId, userId);
    }

    /**
     * Checks if the downloadUrl field is in the selection set of a graphql query.
     *
     * @param env The DataFetchingEnvironment of the graphql query
     * @return Returns true if the downloadUrl field is in the selection set of the passed DataFetchingEnvironment.
     */
    private boolean downloadUrlInSelectionSet(DataFetchingEnvironment env) {
        return env.getSelectionSet().contains("downloadUrl");
    }

    /**
     * Checks if the uploadUrl field is in the selection set of a graphql query.
     * @param env The DataFetchingEnvironment of the graphql query
     * @return Returns true if the uploadUrl field is in the selection set of the passed DataFetchingEnvironment.
     */
    private boolean uploadUrlInSelectionSet(DataFetchingEnvironment env) {
        return env.getSelectionSet().contains("uploadUrl");
    }
}
