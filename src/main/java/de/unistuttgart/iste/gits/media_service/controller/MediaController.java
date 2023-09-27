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
    public List<MediaRecord> mediaRecords(final DataFetchingEnvironment env) {
        return mediaService.getAllMediaRecords(
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<MediaRecord> mediaRecordsByIds(@Argument final List<UUID> ids, final DataFetchingEnvironment env) {
        return mediaService.getMediaRecordsByIds(
                ids,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<MediaRecord> findMediaRecordsByIds(@Argument final List<UUID> ids, final DataFetchingEnvironment env) {
        return mediaService.findMediaRecordsByIds(
                ids,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<MediaRecord> userMediaRecords(@ContextValue final LoggedInUser currentUser,
                                              final DataFetchingEnvironment env) {
        return mediaService.getMediaRecordsForUser(
                currentUser.getId(),
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<List<MediaRecord>> mediaRecordsByContentIds(@Argument final List<UUID> contentIds,
                                                            final DataFetchingEnvironment env) {
        return mediaService.getMediaRecordsByContentIds(
                contentIds,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<List<MediaRecord>> mediaRecordsForCourses(@Argument final List<UUID> courseIds, final DataFetchingEnvironment env) {
        return mediaService.getMediaRecordsForCourses(
                courseIds,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @SchemaMapping(typeName = "MediaRecord", field = "userProgressData")
    public MediaRecordProgressData userProgressData(final MediaRecord mediaRecord,
                                                    @ContextValue final LoggedInUser currentUser) {
        return mediaUserProgressDataService.getUserProgressData(mediaRecord.getId(), currentUser.getId());
    }

    @MutationMapping
    public MediaRecord createMediaRecord(@Argument final List<UUID> courseIds,
                                         @Argument final CreateMediaRecordInput input,
                                         @ContextValue final LoggedInUser currentUser,
                                         final DataFetchingEnvironment env) {
        return mediaService.createMediaRecord(
                courseIds,
                input,
                currentUser.getId(),
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @MutationMapping
    public UUID deleteMediaRecord(@Argument final UUID id) {
        return mediaService.deleteMediaRecord(id);
    }

    @MutationMapping
    public MediaRecord updateMediaRecord(@Argument final List<UUID> courseIds,
                                         @Argument final UpdateMediaRecordInput input,
                                         final DataFetchingEnvironment env) {
        return mediaService.updateMediaRecord(
                courseIds,
                input,
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @MutationMapping
    public MediaRecord logMediaRecordWorkedOn(@Argument final UUID mediaRecordId,
                                              @ContextValue final LoggedInUser currentUser) {
        return mediaUserProgressDataService.logMediaRecordWorkedOn(mediaRecordId, currentUser.getId());
    }

    @MutationMapping
    public List<MediaRecord> setLinkedMediaRecordsForContent(@Argument final UUID contentId,
                                                         @Argument final List<UUID> mediaRecordIds) {
        return mediaService.setLinkedMediaRecordsForContent(contentId, mediaRecordIds);
    }

    /**
     * Checks if the downloadUrl field is in the selection set of a graphql query.
     *
     * @param env The DataFetchingEnvironment of the graphql query
     * @return Returns true if the downloadUrl field is in the selection set of the passed DataFetchingEnvironment.
     */
    private boolean downloadUrlInSelectionSet(final DataFetchingEnvironment env) {
        return env.getSelectionSet().contains("downloadUrl");
    }

    /**
     * Checks if the uploadUrl field is in the selection set of a graphql query.
     * @param env The DataFetchingEnvironment of the graphql query
     * @return Returns true if the uploadUrl field is in the selection set of the passed DataFetchingEnvironment.
     */
    private boolean uploadUrlInSelectionSet(final DataFetchingEnvironment env) {
        return env.getSelectionSet().contains("uploadUrl");
    }

    /**
     * Add the Course to the selected mediaRecords
     *
     * @param courseId of the course
     * @param mediaRecordIds of the mediaRecords to be changed
     * @return the updated mediaRecords
     */
    @MutationMapping
    public List<MediaRecord> setMediaRecordsForCourse(@Argument final UUID courseId, @Argument final List<UUID> mediaRecordIds) {
        return mediaService.setMediaRecordsForCourse(courseId, mediaRecordIds);
    }
}
