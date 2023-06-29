package de.unistuttgart.iste.gits.media_service.controller;

import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

/**
 * Implements the graphql endpoints of the service.
 */
@Slf4j
@Controller
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

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

    /**
     * Checks if the downloadUrl field is in the selection set of a graphql query.
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
