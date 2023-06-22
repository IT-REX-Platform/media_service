package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.common.dapr.CrudOperation;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.media_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
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
     * @return Returns a list containing all saved media records.
     */
    public List<MediaRecord> getAllMediaRecords() {
        return repository.findAll().stream().map(x -> modelMapper.map(x, MediaRecord.class)).toList();
    }

    /**
     * When passed a list of media record ids, returns a list containing the records matching these ids, or throws
     * an EntityNotFoundException when there is no matching record for one or more of the passed ids.
     *
     * @param ids The ids to retrieve the records for.
     * @return List of the records with matching ids.
     * @throws EntityNotFoundException Thrown when one or more passed ids do not have corresponding media records in
     *                                 the database.
     */
    public List<MediaRecord> getMediaRecordsById(List<UUID> ids) {
        List<MediaRecordEntity> records = repository.findAllById(ids).stream().toList();

        // if there are fewer returned records than passed ids, that means that some ids could not be found in the
        // db. In that case, calculate the difference of the two lists and throw an exception listing which ids
        // could not be found
        if (records.size() != ids.size()) {
            List<UUID> missingIds = new ArrayList<>(ids);
            missingIds.removeAll(records.stream().map(MediaRecordEntity::getId).toList());

            throw new EntityNotFoundException("Media record(s) with id(s) " + missingIds.stream().map(UUID::toString).collect(Collectors.joining(", ")) + " not found.");
        }

        return records.stream().map(x -> modelMapper.map(x, MediaRecord.class)).toList();
    }

    /**
     * Gets all media records that are associated with the passed content ids.
     *
     * @param contentIds The content ids to get the media records for.
     * @return Returns a list of lists, where each sublist stores the media records that are associated with the content
     * id at the same index in the passed list.
     */
    public List<List<MediaRecord>> getMediaRecordsByContentIds(List<UUID> contentIds) {
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

        return result;
    }

    /**
     * Creates a new media record with the attributes specified in the input argument.
     *
     * @param input Object storing the attributes the newly created media record should have.
     * @return Returns the media record which was created, with the ID generated for it.
     */
    public MediaRecord createMediaRecord(CreateMediaRecordInput input) {
        MediaRecordEntity entity = modelMapper.map(input, MediaRecordEntity.class);

        repository.save(entity);

        //publish changes
        topicPublisher.notifyChange(entity, CrudOperation.CREATE);

        return modelMapper.map(entity, MediaRecord.class);
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
        String bucketId = minioVariables.get("bucketId");
        String filename = minioVariables.get("filename");

        repository.delete(entity.orElseThrow(() -> new EntityNotFoundException("Media record with id " + id + " not found.")));

        minioClient.removeObject(
                RemoveObjectArgs
                        .builder()
                        .bucket(bucketId)
                        .object(filename)
                        .build());

        //publish changes
        topicPublisher.notifyChange(entity.get(), CrudOperation.DELETE);
        return id;
    }

    /**
     * Updates an existing media record matching the id passed as an attribute in the input argument.
     *
     * @param input Object containing the new attributes that should be stored for the existing media record matching
     *              the id field of the input object.
     * @return Returns the media record with its newly updated data.
     */
    public MediaRecord updateMediaRecord(UpdateMediaRecordInput input) {
        if (!repository.existsById(input.getId())) {
            throw new EntityNotFoundException("Media record with id " + input.getId() + " not found.");
        }

        MediaRecordEntity entity = repository.save(modelMapper.map(input, MediaRecordEntity.class));

        //publish changes
        topicPublisher.notifyChange(entity, CrudOperation.UPDATE);

        return modelMapper.map(entity, MediaRecord.class);
    }

    /**
     * Creates a URL for uploading a file to the minIO Server.
     *
     * @param input DTO which contains the bucket id to which to upload as well as the name of the file which should be uploaded.
     * @return Returns the created uploadURL.
     */
    @SneakyThrows
    public UploadUrl createUploadUrl(CreateUrlInput input) {
        Map<String, String> variables = createMinIOVariables(input.getId());
        String bucketId = variables.get("bucketId");
        String filename = variables.get("filename");

        // Ensures that the Bucket exists or creates a new one otherwise. Weirdly this only works if at least one bucket already exists.
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketId).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketId).build());
        }

        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs
                        .builder()
                        .method(Method.PUT)
                        .bucket(bucketId)
                        .object(filename)
                        .expiry(15, TimeUnit.MINUTES)
                        .build());

        return new UploadUrl(url);
    }

    /**
     * Creates a URL for downloading a file from the MinIO Server.
     *
     * @param input DTO which contains the bucket id from which to download as well as the name of the file which should be downloaded.
     * @return Returns the created downloadURL.
     */
    @SneakyThrows
    public DownloadUrl createDownloadUrl(CreateUrlInput input) {
        Map<String, String> variables = createMinIOVariables(input.getId());
        String bucketId = variables.get("bucketId");
        String filename = variables.get("filename");


        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketId)
                        .object(filename)
                        .expiry(15, TimeUnit.MINUTES)
                        .build());
        return new DownloadUrl(url);
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
            throw new EntityNotFoundException("Media record with id " + input + " not found.");
        }

        Optional<MediaRecordEntity> entity = repository.findById(input);

        if (entity.isPresent()) {
            String filename = entity.get().getId().toString();
            variables.put("filename", filename);
            String bucketId = entity.get().getType().toString().toLowerCase();
            variables.put("bucketId", bucketId);
        }

        return variables;
    }
}
