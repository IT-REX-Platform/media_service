package de.unistuttgart.iste.gits.media_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.gits.media_service.test_config.MockMinIoClientConfiguration;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecords;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@TablesToDelete({"media_record_content_ids", "media_record"})
@Transactional
@GraphQlApiTest
class MutationDeleteMediaRecordTest {

    @Autowired
    private MediaRecordRepository repository;

    @Test
    void testDeleteMediaRecord(GraphQlTester tester) {
        List<MediaRecordEntity> createdMediaRecords = fillRepositoryWithMediaRecords(repository);

        createdMediaRecords = repository.saveAll(createdMediaRecords);

        String query = """
                mutation {
                    deleteMediaRecord(id: "%s")
                }
                """.formatted(createdMediaRecords.get(0).getId());

        tester.document(query)
                .execute()
                .path("deleteMediaRecord").entity(UUID.class).isEqualTo(createdMediaRecords.get(0).getId());

        // ensure that the media record left in the db is the other one (the one we didn't delete)
        assertThat(repository.count(), is((long) createdMediaRecords.size() - 1));
        MediaRecordEntity remainingMediaRecord = repository.findAll().get(0);
        assertThat(remainingMediaRecord, equalTo(createdMediaRecords.get(1)));
    }
}
