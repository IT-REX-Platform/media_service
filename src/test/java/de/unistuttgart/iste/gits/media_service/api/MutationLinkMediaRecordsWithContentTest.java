package de.unistuttgart.iste.gits.media_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.generated.dto.MediaRecord;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecords;

@TablesToDelete({"media_record_content_ids", "media_record"})
@GraphQlApiTest
@ActiveProfiles("test")
class MutationLinkMediaRecordsWithContentTest {

    @Autowired
    private MediaRecordRepository repository;

    @Test
    @Transactional
    @Commit
    void testLinkMediaRecordsWithContent(GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecords(repository);
        UUID content1Id = UUID.randomUUID();
        UUID content2Id = UUID.randomUUID();
        expectedMediaRecords.get(0).setContentIds(new ArrayList<>(List.of(content1Id, content2Id)));
        expectedMediaRecords.get(1).setContentIds(new ArrayList<>(List.of(content1Id)));

        expectedMediaRecords = repository.saveAll(expectedMediaRecords);

        String query = """
                mutation($contentId: UUID!, $mediaRecordIds: [UUID!]!) {
                    mediaRecords: _internal_setLinkedMediaRecordsForContent(contentId: $contentId, mediaRecordIds: $mediaRecordIds) {
                        contentIds
                    }
                }
                """;

        tester.document(query)
                .variable("contentId", content2Id)
                .variable("mediaRecordIds", List.of(expectedMediaRecords.get(1).getId()))
                .execute()
                .path("mediaRecords").entityList(MediaRecord.class).hasSize(1)
                .path("mediaRecords[0].contentIds").entityList(UUID.class).hasSize(2).contains(content1Id, content2Id);

        List<MediaRecordEntity> actualMediaRecords = repository.findAll();
        assertThat(actualMediaRecords).hasSize(2);
        assertThat(actualMediaRecords.get(0).getContentIds()).contains(content1Id);
        assertThat(actualMediaRecords.get(1).getContentIds()).contains(content1Id, content2Id);
    }
}
