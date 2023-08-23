package de.unistuttgart.iste.gits.media_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
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
        UUID userId1 = UUID.randomUUID();

        String currentUser = """
                {
                    "id": "%s",
                    "userName": "MyUserName",
                    "firstName": "John",
                    "lastName": "Doe"
                }
                """.formatted(userId1.toString());

        // insert user header into tester
        tester = tester.mutate().header("CurrentUser", currentUser).build();

        String query = """
                mutation {
                    createMediaRecord(input: {
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

        UUID id = tester.document(query)
                .execute()
                .path("createMediaRecord.name").entity(String.class).isEqualTo("Example Record")
                .path("createMediaRecord.creatorId").entity(UUID.class).isEqualTo(userId1)
                .path("createMediaRecord.type").entity(String.class).isEqualTo("VIDEO")
                .path("createMediaRecord.contentIds").entityList(UUID.class)
                    .containsExactly(UUID.fromString("e8653f6f-9c14-4d84-8942-613ec651153a"))
                .path("createMediaRecord.uploadUrl").entity(String.class).isEqualTo("http://example.com")
                .path("createMediaRecord.downloadUrl").entity(String.class).isEqualTo("http://example.com")
                .path("createMediaRecord.id").entity(UUID.class).get();

        assertThat(repository.count(), is(1L));
        var mediaRecord = repository.findAll().get(0);
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
}
