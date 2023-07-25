package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.common.event.ContentChangeEvent;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.generated.dto.CreateMediaRecordInput;
import de.unistuttgart.iste.gits.generated.dto.MediaRecord;
import de.unistuttgart.iste.gits.generated.dto.UpdateMediaRecordInput;
import de.unistuttgart.iste.gits.media_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service class which provides the business logic of the media service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MediaService {

    public static final String BUCKET_ID = "bucketId";
    public static final String FILENAME = "filename";
    public static final String MEDIA_RECORDS_NOT_FOUND = "Media record(s) with id(s) %s not found.";
    private final MinioClient minioInternalClient;
    private final MinioClient minioExternalClient;

    /**
     * Database repository storing our media records.
     */
    private final MediaRecordRepository repository;
    /**
     * Mapper used to map media record DTOs to database Entities and vice-versa.
     */
    private final ModelMapper modelMapper;

    /**
     * dapr topic publisher
     */
    private final TopicPublisher topicPublisher;

    /**
     * Returns all media records.
     *
     * @param generateUploadUrls   If temporary upload urls should be generated for the media records
     * @param generateDownloadUrls If temporary download urls should be generated for the media records
     * @return Returns a list containing all saved media records.
     */
    public List<MediaRecord> getAllMediaRecords(boolean generateUploadUrls, boolean generateDownloadUrls) {
        List<MediaRecord> records = repository.findAll().stream()
                .map(this::mapEntityToMediaRecord)
                .toList();

        return fillMediaRecordsUrlsIfRequested(records, generateUploadUrls, generateDownloadUrls);
    }

    /**
     * When passed a list of media record ids, returns a list containing the records matching these ids, or throws
     * an EntityNotFoundException when there is no matching record for one or more of the passed ids.
     *
     * @param ids                  The ids to retrieve the records for.
     * @param generateUploadUrls   If temporary upload urls should be generated for the media records
     * @param generateDownloadUrls If temporary download urls should be generated for the media records
     * @return List of the records with matching ids.
     * @throws EntityNotFoundException Thrown when one or more passed ids do not have corresponding media records in
     *                                 the database.
     */
    public List<MediaRecord> getMediaRecordsById(List<UUID> ids, boolean generateUploadUrls, boolean generateDownloadUrls) {
        List<MediaRecordEntity> records = repository.findAllById(ids).stream().toList();

        // if there are fewer returned records than passed ids, that means that some ids could not be found in the
        // db. In that case, calculate the difference of the two lists and throw an exception listing which ids
        // could not be found
        if (records.size() != ids.size()) {
            List<UUID> missingIds = new ArrayList<>(ids);
            missingIds.removeAll(records.stream().map(MediaRecordEntity::getId).toList());

            throw new EntityNotFoundException(MEDIA_RECORDS_NOT_FOUND
                    .formatted(missingIds.stream().map(UUID::toString).collect(Collectors.joining(", "))));
        }

        return fillMediaRecordsUrlsIfRequested(
                records.stream().map(x -> modelMapper.map(x, MediaRecord.class)).toList(),
                generateUploadUrls,
                generateDownloadUrls
        );
    }

    public MediaRecord getMediaRecordById(UUID id) {
        requireMediaRecordExisting(id);
        return mapEntityToMediaRecord(repository.getReferenceById(id));
    }

    /**
     * Gets all media records for which the specified user is the creator.
     * @param userId The id of the user to get the media records for.
     * @return Returns a list of the user's media records.
     */
    public List<MediaRecord> getMediaRecordsForUser(UUID userId, boolean generateUploadUrls, boolean generateDownloadUrls) {
        List<MediaRecordEntity> records = repository.findMediaRecordEntitiesByCreatorId(userId);

        return fillMediaRecordsUrlsIfRequested(
                records.stream().map(x -> modelMapper.map(x, MediaRecord.class)).toList(),
                generateUploadUrls,
                generateDownloadUrls
        );
    }

    /**
     * Gets all media records that are associated with the passed content ids.
     *
     * @param contentIds           The content ids to get the media records for.
     * @param generateUploadUrls   If temporary upload urls should be generated for the media records
     * @param generateDownloadUrls If temporary download urls should be generated for the media records
     * @return Returns a list of lists, where each sublist stores the media records that are associated with the content
     * id at the same index in the passed list.
     */
    public List<List<MediaRecord>> getMediaRecordsByContentIds(List<UUID> contentIds, boolean generateUploadUrls, boolean generateDownloadUrls) {
        List<MediaRecordEntity> records = repository.findMediaRecordEntitiesByContentIds(contentIds);

        // create our resulting list
        List<List<MediaRecord>> result = new ArrayList<>(contentIds.size());

        // fill it with empty lists for each content id so that we can later fill them with
        // the media records associated with that content id
        for (int i = 0; i < contentIds.size(); i++) {
            result.add(new ArrayList<>());
        }

        // loop over all the entities we got and put them in their respective lists
        for (MediaRecordEntity entity : records) {
            for (int i = 0; i < contentIds.size(); i++) {
                UUID contentId = contentIds.get(i);
                if (entity.getContentIds().contains(contentId)) {
                    result.get(i).add(mapEntityToMediaRecord(entity));
                }
            }
        }

        result.forEach(x -> fillMediaRecordsUrlsIfRequested(x, generateUploadUrls, generateDownloadUrls));

        return result;
    }

    public List<MediaRecordEntity> getMediaRecordEntitiesByContentId(UUID contentId) {
        return repository.findMediaRecordEntitiesByContentIds(List.of(contentId));
    }

    public void requireMediaRecordExisting(UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Media record with id " + id + " not found.");
        }
    }

    /**
     * Links the media records with the passed ids to the content with the passed id.
     * @param contentId The content id to link the media records to.
     * @param mediaRecordIds The ids of the media records to link to the content.
     * @return Returns a list of the media records that were linked to the content.
     */
    public List<MediaRecord> linkMediaRecordsWithContent(UUID contentId, List<UUID> mediaRecordIds) {
        List<MediaRecordEntity> entities = repository.findAllById(mediaRecordIds);

        // if there are fewer returned records than passed ids, that means that some ids could not be found in the
        // db. In that case, calculate the difference of the two lists and throw an exception listing which ids
        // could not be found
        if (entities.size() != mediaRecordIds.size()) {
            List<UUID> missingIds = new ArrayList<>(mediaRecordIds);
            missingIds.removeAll(entities.stream().map(MediaRecordEntity::getId).toList());

            throw new EntityNotFoundException(MEDIA_RECORDS_NOT_FOUND
                    .formatted(missingIds.stream().map(UUID::toString).collect(Collectors.joining(", "))));
        }

        for(MediaRecordEntity entity : entities) {
            entity.getContentIds().add(contentId);
            repository.save(entity);
        }

        return entities.stream().map(x -> modelMapper.map(x, MediaRecord.class)).toList();
    }

    /**
     * Creates a new media record with the attributes specified in the input argument.
     *
     * @param input               Object storing the attributes the newly created media record should have.
     * @param creatorId           The id of the user that creates the media record
     * @param generateUploadUrl   If a temporary upload url should be generated for the media record
     * @param generateDownloadUrl If a temporary download url should be generated for the media record
     * @return Returns the media record which was created, with the ID generated for it.
     */
    public MediaRecord createMediaRecord(CreateMediaRecordInput input,
                                         UUID creatorId,
                                         boolean generateUploadUrl,
                                         boolean generateDownloadUrl) {
        MediaRecordEntity entity = modelMapper.map(input, MediaRecordEntity.class);

        entity.setCreatorId(creatorId);

        repository.save(entity);

        //publish changes
        topicPublisher.notifyResourceChange(entity, CrudOperation.CREATE);

        return fillMediaRecordUrlsIfRequested(
                mapEntityToMediaRecord(entity),
                generateUploadUrl,
                generateDownloadUrl
        );
    }

    /**
     * Deletes a media record matching the specified id or throws EntityNotFoundException if a record with the
     * specified id could not be found.
     *
     * @param id The id of the media record which should be deleted.
     * @return Returns the id of the record which was deleted.
     * @throws EntityNotFoundException Thrown when no record matching the passed id could be found.
     */
    @SneakyThrows
    public UUID deleteMediaRecord(UUID id) {
        requireMediaRecordExisting(id);
        MediaRecordEntity entity = repository.getReferenceById(id);
        Map<String, String> minioVariables = createMinIOVariables(id);
        String bucketId = minioVariables.get(BUCKET_ID);
        String filename = minioVariables.get(FILENAME);

        repository.delete(entity);


        boolean bucketExists = minioInternalClient.bucketExists(BucketExistsArgs.builder().bucket(bucketId).build());
        if (bucketExists) {
            if (isObjectExist(filename, bucketId)) {
                minioInternalClient.removeObject(
                        RemoveObjectArgs
                                .builder()
                                .bucket(bucketId)
                                .object(filename)
                                .build());
            }
        }

        //publish changes
        topicPublisher.notifyResourceChange(entity, CrudOperation.DELETE);
        return id;
    }

    /**
     * Updates an existing media record matching the id passed as an attribute in the input argument.
     *
     * @param input               Object containing the new attributes that should be stored for the existing media record matching
     *                            the id field of the input object.
     * @param generateUploadUrl   If a temporary upload url should be generated for the media record
     * @param generateDownloadUrl If a temporary download url should be generated for the media record
     * @return Returns the media record with its newly updated data.
     */
    public MediaRecord updateMediaRecord(UpdateMediaRecordInput input, boolean generateUploadUrl, boolean generateDownloadUrl) {
        requireMediaRecordExisting(input.getId());

        MediaRecordEntity oldEntity = repository.getReferenceById(input.getId());

        // generate new entity based on updated data
        MediaRecordEntity newEntity = modelMapper.map(input, MediaRecordEntity.class);

        // keep creator id from old entity
        newEntity.setCreatorId(oldEntity.getCreatorId());

        // save updated entity
        MediaRecordEntity entity = repository.save(newEntity);

        //publish changes
        topicPublisher.notifyResourceChange(entity, CrudOperation.UPDATE);

        return fillMediaRecordUrlsIfRequested(
                mapEntityToMediaRecord(entity),
                generateUploadUrl,
                generateDownloadUrl
        );
    }

    public MediaRecord mapEntityToMediaRecord(MediaRecordEntity entity) {
        return modelMapper.map(entity, MediaRecord.class);
    }

    /**
     * Helper method which can be used to fill passed media records with generated upload/download urls if such urls
     * have been requested in the selection set of the graphql query.
     *
     * @param mediaRecords The list of media records to fill the urls for.
     * @return Returns the same list (which has been modified in-place) with the media records with the now added urls.
     */
    private List<MediaRecord> fillMediaRecordsUrlsIfRequested(List<MediaRecord> mediaRecords, boolean generateUploadUrls, boolean generateDownloadUrls) {
        for (MediaRecord mediaRecord : mediaRecords) {
            fillMediaRecordUrlsIfRequested(mediaRecord, generateUploadUrls, generateDownloadUrls);
        }

        return mediaRecords;
    }

    /**
     * Helper method which adds a generated upload and/or download url to the passed media record and returns that same
     * media record.
     *
     * @param mediaRecord         The media record to add the urls to.
     * @param generateUploadUrl   If an upload url should be generated.
     * @param generateDownloadUrl If a download url should be generated
     * @return Returns the same media record that has been passed to the method.
     */
    private MediaRecord fillMediaRecordUrlsIfRequested(MediaRecord mediaRecord, boolean generateUploadUrl, boolean generateDownloadUrl) {
        if (generateUploadUrl) {
            mediaRecord.setUploadUrl(createUploadUrl(mediaRecord.getId()));
        }

        if (generateDownloadUrl) {
            mediaRecord.setDownloadUrl(createDownloadUrl(mediaRecord.getId()));
        }

        return mediaRecord;
    }

    /**
     * Creates a URL for uploading a file to the minIO Server.
     *
     * @param mediaRecordId UUID of the media record to generate the upload url for.
     * @return Returns the created uploadURL.
     */
    @SneakyThrows
    private String createUploadUrl(UUID mediaRecordId) {
        Map<String, String> variables = createMinIOVariables(mediaRecordId);
        String bucketId = variables.get(BUCKET_ID);
        String filename = variables.get(FILENAME);

        // Ensures that the Bucket exists or creates a new one otherwise. Weirdly this only works if at least one bucket already exists.
        boolean found = minioInternalClient.bucketExists(BucketExistsArgs.builder().bucket(bucketId).build());
        if (!found) {
            minioInternalClient.makeBucket(MakeBucketArgs.builder().bucket(bucketId).build());
        }

        return minioExternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs
                        .builder()
                        .method(Method.PUT)
                        .bucket(bucketId)
                        .object(filename)
                        .expiry(15, TimeUnit.MINUTES)
                        .build());
    }

    /**
     * Creates a URL for downloading a file from the MinIO Server.
     *
     * @param mediaRecordId UUID of the media record to generate the download url for.
     * @return Returns the created downloadURL.
     */
    @SneakyThrows
    private String createDownloadUrl(UUID mediaRecordId) {
        Map<String, String> variables = createMinIOVariables(mediaRecordId);
        String bucketId = variables.get(BUCKET_ID);
        String filename = variables.get(FILENAME);


        return minioExternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketId)
                        .object(filename)
                        .expiry(15, TimeUnit.MINUTES)
                        .build());
    }

    /**
     * Creates the bucketId and filename for MinIO from the media record.
     *
     * @param input UUID of the media record
     * @return a map with the bucketID and filename which should be used by MinIO
     */
    private Map<String, String> createMinIOVariables(UUID input) {
        Map<String, String> variables = new HashMap<>();

        requireMediaRecordExisting(input);

        MediaRecordEntity entity = repository.getReferenceById(input);

        String filename = entity.getId().toString();
        variables.put(FILENAME, filename);
        String bucketId = entity.getType().toString().toLowerCase();
        variables.put(BUCKET_ID, bucketId);

        return variables;
    }

    public boolean isObjectExist(String name, String bucketname) {
        try {

            minioInternalClient.statObject(StatObjectArgs.builder()

                    .bucket(bucketname)
                    .object(name).build());
            return true;
        } catch (ErrorResponseException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * function that updates all media records that contain at least one of the received content IDs.
     * All received content Ids are removed from the media records.
     * If changes are performed to an entity, a message is published to a dapr topic.
     * @param dto Event object containing a list of content IDs and a CRUD operation
     */
    public void removeContentIds(ContentChangeEvent dto){

        // check if DTO is complete
        if (dto.getContentIds() == null  || dto.getOperation() == null){
            throw new NullPointerException("incomplete message received: all fields of a message must be non-null");
        }

        //This method should only process Content Deletion Events
        if (!dto.getOperation().equals(CrudOperation.DELETE) || dto.getContentIds().isEmpty()){
            return;
        }


        List<MediaRecordEntity> entities = repository.findMediaRecordEntitiesByContentIds(dto.getContentIds());

        // apply changes to all found media records
        for (MediaRecordEntity entity: entities) {

            //is true if changes are applied
            boolean listChanged = entity.getContentIds().removeAll(dto.getContentIds());

            if (listChanged){
                repository.save(entity);
                //publish changes to dapr topic
                topicPublisher.notifyResourceChange(entity, CrudOperation.UPDATE);
            }

        }

    }
}
