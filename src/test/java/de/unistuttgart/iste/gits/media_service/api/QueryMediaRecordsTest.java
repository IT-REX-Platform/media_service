package de.unistuttgart.iste.gits.media_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.generated.dto.MediaRecord;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecords;

@GraphQlApiTest
@Transactional
@TablesToDelete({"media_record_content_ids", "media_record"})
@ActiveProfiles("test")
class QueryMediaRecordsTest {

    @Autowired
    private MediaRecordRepository repository;

    private ModelMapper mapper = new ModelMapper();

    @Test
    void testQueryAllMediaRecordsEmpty(GraphQlTester tester) {
        String query = """
                query {
                    mediaRecords {
                        id,
                        name,
                        type,
                        contentIds
                    }
                }
                """;

        tester.document(query)
                .execute()
                .path("mediaRecords").entityList(MediaRecord.class).hasSize(0);
    }

    @Test
    void testQueryAllMediaRecords(GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecords(repository);

        String query = """
                query {
                    mediaRecords {
                        id,
                        name,
                        creatorId,
                        type,
                        contentIds
                    }
                }
                """;

        tester.document(query)
                .execute()
                .path("mediaRecords").entityList(MediaRecord.class).hasSize(expectedMediaRecords.size())
                .contains(expectedMediaRecords.stream()
                        .map(x -> mapper.map(x, MediaRecord.class))
                        .toArray(MediaRecord[]::new));
    }

    @Test
    void testQueryMediaRecordsByIds(GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecords(repository);

        String query = """
                query {
                    mediaRecordsByIds(ids: ["%s", "%s"]) {
                        id,
                        name,
                        creatorId,
                        type,
                        contentIds
                    }
                }
                """.formatted(expectedMediaRecords.get(0).getId(), expectedMediaRecords.get(1).getId());

        tester.document(query)
                .execute()
                .path("mediaRecordsByIds").entityList(MediaRecord.class).hasSize(expectedMediaRecords.size())
                .contains(expectedMediaRecords.stream()
                        .map(x -> mapper.map(x, MediaRecord.class))
                        .toArray(MediaRecord[]::new));
    }

    @Test
    void testQueryFindMediaRecordsByIds(GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecords(repository);

        UUID nonexistantUUID = UUID.randomUUID();

        String query = """
                query($ids: [UUID!]!) {
                    findMediaRecordsByIds(ids: $ids) {
                        id,
                        name,
                        creatorId,
                        type,
                        contentIds
                    }
                }
                """;

        tester.document(query)
                .variable("ids", List.of(expectedMediaRecords.get(0).getId(), nonexistantUUID))
                .execute()
                .path("findMediaRecordsByIds").entityList(MediaRecord.class).hasSize(2)
                .contains(mapper.map(expectedMediaRecords.get(0), MediaRecord.class), null);
    }

    @Test
    void testQueryMediaRecordsByContentIds(GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecords(repository);

        String query = """
                query {
                    mediaRecordsByContentIds(contentIds: ["%s", "%s"]) {
                        id,
                        name,
                        creatorId,
                        type,
                        contentIds
                    }
                }
                """.formatted(expectedMediaRecords.get(0).getContentIds().get(0),
                expectedMediaRecords.get(1).getContentIds().get(0));

        GraphQlTester.Response response = tester.document(query).execute();

        response.path("mediaRecordsByContentIds").entityList(List.class).hasSize(2);

        response.path("mediaRecordsByContentIds[0]").entityList(MediaRecord.class)
                .hasSize(1)
                .contains(mapper.map(expectedMediaRecords.get(0), MediaRecord.class));

        response.path("mediaRecordsByContentIds[1]").entityList(MediaRecord.class)
                .hasSize(1)
                .contains(mapper.map(expectedMediaRecords.get(1), MediaRecord.class));
    }
}
