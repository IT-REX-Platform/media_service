package de.unistuttgart.iste.gits.media_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecords;

@Transactional
@TablesToDelete({"media_record_content_ids", "media_record"})
@GraphQlApiTest
@ActiveProfiles("test")
public class MutationLinkMediaRecordsWithContentTest {

    @Autowired
    private MediaRecordRepository repository;

    @Test
    void testLinkMediaRecordsWithContent(GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecords(repository);

        expectedMediaRecords = repository.saveAll(expectedMediaRecords);

        UUID contentId = UUID.randomUUID();

        String query = """
                mutation($contentId: UUID!, $mediaRecordIds: [UUID!]!) {
                    linkMediaRecordsWithContent(contentId: $contentId, mediaRecordIds: $mediaRecordIds) {
                        contentIds
                    }
                }
                """;

        tester.document(query)
                .variable("contentId", contentId)
                .variable("mediaRecordIds", expectedMediaRecords.stream().map(MediaRecordEntity::getId).toArray())
                .execute()
                .path("linkMediaRecordsWithContent").entityList(MediaRecordEntity.class).hasSize(expectedMediaRecords.size())
                .path("linkMediaRecordsWithContent[0].contentIds").entityList(UUID.class).contains(contentId)
                .path("linkMediaRecordsWithContent[1].contentIds").entityList(UUID.class).contains(contentId);

    }
}
