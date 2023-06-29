package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.common.dapr.CrudOperation;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.media_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import graphql.schema.DataFetchingEnvironment;
import io.minio.*;
import io.minio.http.Method;
import jakarta.persistence.EntityNotFoundException;
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
public class MediaService {

    public static final String BUCKET_ID = "bucketId";
    public static final String FILENAME = "filename";
    public static final String MEDIA_RECORDS_NOT_FOUND = "Media record(s) with id(s) %s not found.";
    private final MinioClient minioClient;

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

    public MediaService(MediaRecordRepository mediaRecordRepository, ModelMapper modelMapper, MinioClient minioClient, TopicPublisher topicPublisher) {
        this.repository = mediaRecordRepository;
        this.modelMapper = modelMapper;
        this.minioClient = minioClient;
        this.topicPublisher = topicPublisher;
    }

    /**
     * Returns all media records.
     *
     * @param generateUploadUrls If temporary upload urls should be generated for the media records
     * @param generateDownloadUrls If temporary download urls should be generated for the media records
     * @return Returns a list containing all saved media records.
     */
    public List<MediaRecord> getAllMediaRecords(boolean generateUploadUrls, boolean generateDownloadUrls) {
        List<MediaRecord> records = repository.findAll().stream()
                .map(x -> modelMapper.map(x, MediaRecord.class))
                .toList();

        return fillMediaRecordsUrlsIfRequested(records, generateUploadUrls, generateDownloadUrls);
    }

    /**
     * When passed a list of media record ids, returns a list containing the records matching these ids, or throws
     * an EntityNotFoundException when there is no matching record for one or more of the passed ids.
     *
     * @param ids The ids to retrieve the records for.
     * @param generateUploadUrls If temporary upload urls should be generated for the media records
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

    /**
     * Gets all media records that are associated with the passed content ids.
     *
     * @param contentIds The content ids to get the media records for.
     * @param generateUploadUrls If temporary upload urls should be generated for the media records
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
        for(MediaRecordEntity entity : records) {
            for (int i = 0; i < contentIds.size(); i++) {
                UUID contentId = contentIds.get(i);
                if (entity.getContentIds().contains(contentId)) {
                    result.get(i).add(modelMapper.map(entity, MediaRecord.class));
                }
            }
        }

        result.forEach(x -> fillMediaRecordsUrlsIfRequested(x, generateUploadUrls, generateDownloadUrls));

        return result;
    }

    /**
     * Creates a new media record with the attributes specified in the input argument.
     *
     * @param input Object storing the attributes the newly created media record should have.
     * @param generateUploadUrl If a temporary upload url should be generated for the media record
     * @param generateDownloadUrl If a temporary download url should be generated for the media record
     * @return Returns the media record which was created, with the ID generated for it.
     */
    public MediaRecord createMediaRecord(CreateMediaRecordInput input, boolean generateUploadUrl, boolean generateDownloadUrl) {
        MediaRecordEntity entity = modelMapper.map(input, MediaRecordEntity.class);

        repository.save(entity);

        //publish changes
        topicPublisher.notifyChange(entity, CrudOperation.CREATE);

        return fillMediaRecordUrlsIfRequested(
                modelMapper.map(entity, MediaRecord.class),
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
        Optional<MediaRecordEntity> entity = repository.findById(id);
        Map<String, String> minioVariables = createMinIOVariables(id);
        String bucketId = minioVariables.get(BUCKET_ID);
        String filename = minioVariables.get(FILENAME);

        repository.delete(entity.orElseThrow(() ->
                new EntityNotFoundException(MEDIA_RECORDS_NOT_FOUND.formatted(id.toString()))));

        minioClient.removeObject(
                RemoveObjectArgs
                        .builder()
                        .bucket(bucketId)
                        .object(filename)
                        .build());

        //publish changes
        if (entity.isPresent()){
            topicPublisher.notifyChange(entity.get(), CrudOperation.DELETE);
        }
        return id;
    }

    /**
     * Updates an existing media record matching the id passed as an attribute in the input argument.
     *
     * @param input Object containing the new attributes that should be stored for the existing media record matching
     *              the id field of the input object.
     * @param generateUploadUrl If a temporary upload url should be generated for the media record
     * @param generateDownloadUrl If a temporary download url should be generated for the media record
     * @return Returns the media record with its newly updated data.
     */
    public MediaRecord updateMediaRecord(UpdateMediaRecordInput input, boolean generateUploadUrl, boolean generateDownloadUrl) {
        if (!repository.existsById(input.getId())) {
            throw new EntityNotFoundException(MEDIA_RECORDS_NOT_FOUND.formatted(input.getId().toString()));
        }

        MediaRecordEntity entity = repository.save(modelMapper.map(input, MediaRecordEntity.class));

        //publish changes
        topicPublisher.notifyChange(entity, CrudOperation.UPDATE);

        return fillMediaRecordUrlsIfRequested(
                modelMapper.map(entity, MediaRecord.class),
                generateUploadUrl,
                generateDownloadUrl
        );
    }

    /**
     * Helper method which can be used to fill passed media records with generated upload/download urls if such urls
     * have been requested in the selection set of the graphql query.
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
     * @param mediaRecord The media record to add the urls to.
     * @param generateUploadUrl If an upload url should be generated.
     * @param generateDownloadUrl If a download url should be generated
     * @return Returns the same media record that has been passed to the method.
     */
    private MediaRecord fillMediaRecordUrlsIfRequested(MediaRecord mediaRecord, boolean generateUploadUrl, boolean generateDownloadUrl) {
        if(generateUploadUrl) {
            mediaRecord.setUploadUrl(createUploadUrl(mediaRecord.getId()));
        }

        if(generateDownloadUrl) {
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
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketId).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketId).build());
        }

        return minioClient.getPresignedObjectUrl(
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


        return minioClient.getPresignedObjectUrl(
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

        if (!repository.existsById(input)) {
            throw new EntityNotFoundException(MEDIA_RECORDS_NOT_FOUND.formatted(input.toString()));
        }

        Optional<MediaRecordEntity> entity = repository.findById(input);

        if (entity.isPresent()) {
            String filename = entity.get().getId().toString();
            variables.put(FILENAME, filename);
            String bucketId = entity.get().getType().toString().toLowerCase();
            variables.put(BUCKET_ID, bucketId);
        }

        return variables;
    }
}
