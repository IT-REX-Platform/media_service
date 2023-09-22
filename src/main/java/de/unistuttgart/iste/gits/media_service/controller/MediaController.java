package de.unistuttgart.iste.gits.media_service.controller;

import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import de.unistuttgart.iste.gits.media_service.service.MediaUserProgressDataService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
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
    public List<MediaRecord> mediaRecordsByIds(@Argument List<UUID> ids, DataFetchingEnvironment env) {
        return mediaService.getMediaRecordsByIds(
                ids,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<MediaRecord> findMediaRecordsByIds(@Argument List<UUID> ids, DataFetchingEnvironment env) {
        return mediaService.findMediaRecordsByIds(
                ids,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<MediaRecord> userMediaRecords(@ContextValue LoggedInUser currentUser, DataFetchingEnvironment env) {
        return mediaService.getMediaRecordsForUser(
                currentUser.getId(),
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<List<MediaRecord>> mediaRecordsByContentIds(@Argument List<UUID> contentIds, DataFetchingEnvironment env) {
        return mediaService.getMediaRecordsByContentIds(
                contentIds,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @SchemaMapping(typeName = "MediaRecord", field = "userProgressData")
    public MediaRecordProgressData userProgressData(MediaRecord mediaRecord, @ContextValue LoggedInUser currentUser) {
        return mediaUserProgressDataService.getUserProgressData(mediaRecord.getId(), currentUser.getId());
    }

    @MutationMapping
    public MediaRecord _internal_createMediaRecord(@Argument UUID[] courseIds,
            @Argument CreateMediaRecordInput input,
                                         @ContextValue LoggedInUser currentUser,
                                         DataFetchingEnvironment env) {
        return mediaService.createMediaRecord(
                courseIds,
                input,
                currentUser.getId(),
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @MutationMapping
    public UUID deleteMediaRecord(@Argument UUID id) {
        return mediaService.deleteMediaRecord(id);
    }

    @MutationMapping
    public MediaRecord updateMediaRecord(@Argument UUID[] courseIds, @Argument UpdateMediaRecordInput input, DataFetchingEnvironment env) {
        return mediaService.updateMediaRecord(
                courseIds,
                input,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @MutationMapping
    public MediaRecord logMediaRecordWorkedOn(@Argument UUID mediaRecordId, @ContextValue LoggedInUser currentUser) {
        return mediaUserProgressDataService.logMediaRecordWorkedOn(mediaRecordId, currentUser.getId());
    }

    @MutationMapping
    public List<MediaRecord> linkMediaRecordsWithContent(@Argument UUID contentId, @Argument List<UUID> mediaRecordIds) {
        return mediaService.linkMediaRecordsWithContent(contentId, mediaRecordIds);
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
