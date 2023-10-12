package de.unistuttgart.iste.gits.media_service.controller;

import de.unistuttgart.iste.gits.common.exception.NoAccessToCourseException;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.gits.generated.dto.CreateMediaRecordInput;
import de.unistuttgart.iste.gits.generated.dto.MediaRecord;
import de.unistuttgart.iste.gits.generated.dto.MediaRecordProgressData;
import de.unistuttgart.iste.gits.generated.dto.UpdateMediaRecordInput;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import de.unistuttgart.iste.gits.media_service.service.MediaUserProgressDataService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.user_handling.GlobalPermissionAccessValidator.validateUserHasGlobalPermission;
import static de.unistuttgart.iste.gits.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;
import static de.unistuttgart.iste.gits.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourses;

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
    public List<MediaRecord> mediaRecords(final DataFetchingEnvironment env, @ContextValue final LoggedInUser currentUser) {
        validateUserHasGlobalPermission(currentUser, Set.of(LoggedInUser.RealmRole.SUPER_USER));
        return mediaService.getAllMediaRecords(
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @QueryMapping
    public List<MediaRecord> mediaRecordsByIds(@Argument final List<UUID> ids, final DataFetchingEnvironment env, @ContextValue final LoggedInUser currentUser) {
        final List<MediaRecord> mediaRecords = mediaService.getMediaRecordsByIds(ids, uploadUrlInSelectionSet(env), downloadUrlInSelectionSet(env));

        return checkAccessForMediaRecords(currentUser, mediaRecords, UserRoleInCourse.STUDENT);
    }

    @QueryMapping
    public List<MediaRecord> findMediaRecordsByIds(@Argument final List<UUID> ids, final DataFetchingEnvironment env, @ContextValue final LoggedInUser currentUser) {
        final List<MediaRecord> mediaRecords = mediaService.findMediaRecordsByIds(ids, uploadUrlInSelectionSet(env), downloadUrlInSelectionSet(env));

        return checkAccessForMediaRecords(currentUser, mediaRecords, UserRoleInCourse.STUDENT);
    }


    @QueryMapping
    public List<MediaRecord> userMediaRecords(@ContextValue final LoggedInUser currentUser,
                                              final DataFetchingEnvironment env) {
        final List<MediaRecord> mediaRecords = mediaService.getMediaRecordsForUser(currentUser.getId(), uploadUrlInSelectionSet(env), downloadUrlInSelectionSet(env));

        return checkAccessForMediaRecords(currentUser, mediaRecords, UserRoleInCourse.STUDENT);
    }

    @QueryMapping
    public List<MediaRecord> mediaRecordsForUsers(final List<UUID> userIds, final DataFetchingEnvironment env) {
        return  mediaService.getMediaRecordsForUsers(userIds, uploadUrlInSelectionSet(env), downloadUrlInSelectionSet(env));
    }

    @QueryMapping
    public List<List<MediaRecord>> mediaRecordsByContentIds(@Argument final List<UUID> contentIds,
                                                            final DataFetchingEnvironment env, @ContextValue final LoggedInUser currentUser) {
        final List<List<MediaRecord>> mediaRecordsByContentIds = mediaService.getMediaRecordsByContentIds(contentIds, uploadUrlInSelectionSet(env), downloadUrlInSelectionSet(env));
        return checkAccessForSubLists(currentUser, mediaRecordsByContentIds, UserRoleInCourse.STUDENT);
    }

    @QueryMapping
    public List<List<MediaRecord>> mediaRecordsForCourses(@Argument final List<UUID> courseIds, final DataFetchingEnvironment env, @ContextValue final LoggedInUser currentUser) {
        final List<List<MediaRecord>> mediaRecordsByContentIds = mediaService.getMediaRecordsForCourses(courseIds, uploadUrlInSelectionSet(env), downloadUrlInSelectionSet(env));
        return checkAccessForSubLists(currentUser, mediaRecordsByContentIds, UserRoleInCourse.STUDENT);
    }


    @SchemaMapping(typeName = "MediaRecord", field = "userProgressData")
    public MediaRecordProgressData userProgressData(final MediaRecord mediaRecord,
                                                    @ContextValue final LoggedInUser currentUser) {
        checkAccessForMediaRecords(currentUser, List.of(mediaRecord), UserRoleInCourse.STUDENT);
        return mediaUserProgressDataService.getUserProgressData(mediaRecord.getId(), currentUser.getId());
    }

    @MutationMapping
    public MediaRecord createMediaRecord(@Argument final List<UUID> courseIds,
                                         @Argument final CreateMediaRecordInput input,
                                         @ContextValue final LoggedInUser currentUser,
                                         final DataFetchingEnvironment env) {

        if (courseIds != null && !courseIds.isEmpty()) {
            validateUserHasAccessToCourses(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, courseIds);
        }

        return mediaService.createMediaRecord(
                courseIds,
                input,
                currentUser.getId(),
                uploadUrlInSelectionSet(env),
                downloadUrlInSelectionSet(env)
        );
    }

    @MutationMapping
    public UUID deleteMediaRecord(@Argument final UUID id, @ContextValue final LoggedInUser currentUser) {
        checkAccessForMediaRecord(currentUser, mediaService.getMediaRecordById(id), UserRoleInCourse.ADMINISTRATOR);

        return mediaService.deleteMediaRecord(id);
    }

    @MutationMapping
    public MediaRecord updateMediaRecord(@Argument final List<UUID> courseIds,
                                         @Argument final UpdateMediaRecordInput input,
                                         @ContextValue final LoggedInUser currentUser,
                                         final DataFetchingEnvironment env) {
        checkAccessForMediaRecord(currentUser, mediaService.getMediaRecordById(input.getId()), UserRoleInCourse.ADMINISTRATOR);

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
        final MediaRecord mediaRecord = mediaService.getMediaRecordById(mediaRecordId);
        checkAccessForMediaRecord(currentUser, mediaRecord, UserRoleInCourse.STUDENT);
        return mediaUserProgressDataService.logMediaRecordWorkedOn(mediaRecordId, currentUser.getId());
    }

    @MutationMapping
    public List<MediaRecord> setLinkedMediaRecordsForContent(@Argument final UUID contentId,
                                                             @Argument final List<UUID> mediaRecordIds, @ContextValue final LoggedInUser currentUser) {
        final List<MediaRecord> mediaRecords = mediaService.getMediaRecordsByIds(mediaRecordIds, false, false);
        checkAccessForMediaRecords(currentUser, mediaRecords, UserRoleInCourse.ADMINISTRATOR);
        return mediaService.setLinkedMediaRecordsForContent(contentId, mediaRecordIds);
    }

    /**
     * Add the Course to the selected mediaRecords
     *
     * @param courseId       of the course
     * @param mediaRecordIds of the mediaRecords to be changed
     * @return the updated mediaRecords
     */
    @MutationMapping
    public List<MediaRecord> setMediaRecordsForCourse(@Argument final UUID courseId, @Argument final List<UUID> mediaRecordIds, @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, courseId);
        return mediaService.setMediaRecordsForCourse(courseId, mediaRecordIds);
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
     *
     * @param env The DataFetchingEnvironment of the graphql query
     * @return Returns true if the uploadUrl field is in the selection set of the passed DataFetchingEnvironment.
     */
    private boolean uploadUrlInSelectionSet(final DataFetchingEnvironment env) {
        return env.getSelectionSet().contains("uploadUrl");
    }

    /**
     * Checks if the user has access to a MediaRecord in a List of MediaRecords.
     * If the user doesn't has access the mediaRecord will be set to null.
     *
     * @param currentUser  the currently LoggedIn user
     * @param mediaRecords A list of mediaRecords for which permissions should be checked
     * @param role         the minimum required role the user needs for access
     * @return A List of MediaRecords.
     */
    @NotNull
    private static List<MediaRecord> checkAccessForMediaRecords(final LoggedInUser currentUser, final List<MediaRecord> mediaRecords, final UserRoleInCourse role) {
        final List<MediaRecord> filteredMediaRecords = new ArrayList<>();
        for (final MediaRecord mediaRecord : mediaRecords) {
            if (mediaRecord == null) {
                filteredMediaRecords.add(null);
            } else {
                final MediaRecord mediaRecordToAdd;
                final List<UUID> courseIds = mediaRecord.getCourseIds();
                if (courseIds.isEmpty()) {
                    mediaRecordToAdd = mediaRecord;
                } else {
                    mediaRecordToAdd = getMediaRecordToAdd(currentUser, role, mediaRecord, courseIds);
                }
                filteredMediaRecords.add(mediaRecordToAdd);
            }
        }
        return filteredMediaRecords;
    }

    private static MediaRecord getMediaRecordToAdd(final LoggedInUser currentUser, final UserRoleInCourse role, final MediaRecord mediaRecord, final List<UUID> courseIds) {
        MediaRecord mediaRecordToAdd = null;
        for (final UUID id : courseIds) {
            try {
                validateUserHasAccessToCourse(currentUser, role, id);
                mediaRecordToAdd = mediaRecord;
                break;
            } catch (final NoAccessToCourseException ignored) {

            }
        }
        return mediaRecordToAdd;
    }

    /**
     * Checks if the user has access to a single mediaRecord.
     * Throws an exception if the user doesn't have the required permission.
     *
     * @param currentUser currently logged-in User
     * @param mediaRecord the mediaRecord that should be checked
     * @param role        the minimum required role the user needs to perform this action
     */
    private static void checkAccessForMediaRecord(final LoggedInUser currentUser, final MediaRecord mediaRecord, final UserRoleInCourse role) {
        if (mediaRecord.getCourseIds().isEmpty()) {
            return;
        }
        for (final UUID courseId : mediaRecord.getCourseIds()) {
            try {
                validateUserHasAccessToCourse(currentUser, role, courseId);
                break;
            } catch (final NoAccessToCourseException exception) {
               throw exception;
            }
        }
    }

    /**
     * Checks if the User has access to the mediaRecords of the SubLists
     *
     * @param currentUser            currently logged-in user
     * @param ListOfMediaRecordLists the lists that should be checked
     * @param role                   the minimum required role for access
     * @return a List of Lists of Mediarecords
     */
    @NotNull
    private List<List<MediaRecord>> checkAccessForSubLists(final LoggedInUser currentUser, final List<List<MediaRecord>> ListOfMediaRecordLists, final UserRoleInCourse role) {
        final List<List<MediaRecord>> result = new ArrayList<>();

        for (final List<MediaRecord> mediaRecords : ListOfMediaRecordLists) {
            final List<MediaRecord> newMediaRecords = checkAccessForMediaRecords(currentUser, mediaRecords, role);
            result.add(newMediaRecords);
        }

        return result;
    }

}
