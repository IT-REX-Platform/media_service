package de.unistuttgart.iste.gits.media_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.gits.media_service.test_config.MockMinIoClientConfiguration;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@Transactional
@TablesToDelete({"media_record_content_ids", "media_record"})
@GraphQlApiTest
class MutationCreateMediaRecordTest {

    @Autowired
    private MediaRecordRepository repository;

    @Autowired
    private MinioClient minioClient;

    @Test
    void testCreateMediaRecord(HttpGraphQlTester tester) throws Exception {
        final UUID userId1 = UUID.randomUUID();

        final String currentUser = """
                {
                    "id": "%s",
                    "userName": "MyUserName",
                    "firstName": "John",
                    "lastName": "Doe",
                    "courseMemberships": []
                }
                """.formatted(userId1.toString());

        // insert user header into tester
        tester = tester.mutate().header("CurrentUser", currentUser).build();

        final String query = """
                mutation {
                    _internal_createMediaRecord(input: {
                        name: "Example Record",
                        type: VIDEO,
                        contentIds: ["e8653f6f-9c14-4d84-8942-613ec651153a"]
                    }) {
                        id,
                        name,
                        creatorId,
                        type,
                        contentIds,
                        uploadUrl,
                        downloadUrl
                    }
                }
                """;

        final UUID id = tester.document(query)
                .execute()
                .path("_internal_createMediaRecord.name").entity(String.class).isEqualTo("Example Record")
                .path("_internal_createMediaRecord.creatorId").entity(UUID.class).isEqualTo(userId1)
                .path("_internal_createMediaRecord.type").entity(String.class).isEqualTo("VIDEO")
                .path("_internal_createMediaRecord.contentIds").entityList(UUID.class)
                    .containsExactly(UUID.fromString("e8653f6f-9c14-4d84-8942-613ec651153a"))
                .path("_internal_createMediaRecord.uploadUrl").entity(String.class).isEqualTo("http://example.com")
                .path("_internal_createMediaRecord.downloadUrl").entity(String.class).isEqualTo("http://example.com")
                .path("_internal_createMediaRecord.id").entity(UUID.class).get();

        assertThat(repository.count(), is(1L));
        final var mediaRecord = repository.findAll().get(0);
        assertThat(mediaRecord.getId(), is(id));
        assertThat(mediaRecord.getName(), is("Example Record"));
        assertThat(mediaRecord.getCreatorId(), is(userId1));
        assertThat(mediaRecord.getType(), is(MediaRecordEntity.MediaType.VIDEO));
        assertThat(mediaRecord.getContentIds(), contains(UUID.fromString("e8653f6f-9c14-4d84-8942-613ec651153a")));

        verify(minioClient).getPresignedObjectUrl(GetPresignedObjectUrlArgs
                .builder()
                .method(Method.PUT)
                .bucket("video")
                .object(id.toString())
                .expiry(15, TimeUnit.MINUTES)
                .build());

        verify(minioClient).getPresignedObjectUrl(GetPresignedObjectUrlArgs
                .builder()
                .method(Method.GET)
                .bucket("video")
                .object(id.toString())
                .expiry(15, TimeUnit.MINUTES)
                .build());
    }

    @Test
    void createMediaRecordWithCourseIds(HttpGraphQlTester tester) {
        final UUID userId1 = UUID.randomUUID();

        final String currentUser = """
                {
                    "id": "%s",
                    "userName": "MyUserName",
                    "firstName": "John",
                    "lastName": "Doe",
                    "courseMemberships": []
                }
                """.formatted(userId1.toString());

        // insert user header into tester
        tester = tester.mutate().header("CurrentUser", currentUser).build();

        final List<UUID> courseIds = List.of(UUID.fromString("d8a92cec-4975-4ce6-9180-44d1aa16f18d"));

        final String query = """
                mutation {
                    _internal_createMediaRecord(
                    input: {
                        name: "Example Record",
                        type: VIDEO,
                        contentIds: ["e8653f6f-9c14-4d84-8942-613ec651153a"]
                    }
                    courseIds: ["d8a92cec-4975-4ce6-9180-44d1aa16f18d"]
                    ) {
                        id,
                        courseIds,
                        name,
                        creatorId,
                        type,
                        contentIds
                    }
                }
                """;

        final UUID id = tester.document(query)
                .execute()
                .path("_internal_createMediaRecord.name").entity(String.class).isEqualTo("Example Record")
                .path("_internal_createMediaRecord.courseIds").entityList(UUID.class)
                .containsExactly(UUID.fromString("d8a92cec-4975-4ce6-9180-44d1aa16f18d"))
                .path("_internal_createMediaRecord.creatorId").entity(UUID.class).isEqualTo(userId1)
                .path("_internal_createMediaRecord.type").entity(String.class).isEqualTo("VIDEO")
                .path("_internal_createMediaRecord.contentIds").entityList(UUID.class)
                .containsExactly(UUID.fromString("e8653f6f-9c14-4d84-8942-613ec651153a"))
                .path("_internal_createMediaRecord.id").entity(UUID.class).get();


        assertThat(repository.count(), is(1L));
        final var mediaRecord = repository.findAll().get(0);
        System.out.println(mediaRecord);
        assertThat(mediaRecord.getId(), is(id));
        assertThat(mediaRecord.getCourseIds(), is(courseIds));
        assertThat(mediaRecord.getName(), is("Example Record"));
        assertThat(mediaRecord.getCreatorId(), is(userId1));
        assertThat(mediaRecord.getType(), is(MediaRecordEntity.MediaType.VIDEO));
        assertThat(mediaRecord.getContentIds(), contains(UUID.fromString("e8653f6f-9c14-4d84-8942-613ec651153a")));
    }

}
