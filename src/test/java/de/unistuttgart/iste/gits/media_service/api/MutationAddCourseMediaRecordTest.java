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

import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecords;
import static org.assertj.core.api.Assertions.assertThat;

@TablesToDelete({"media_record_course_ids", "media_record_content_ids", "media_record"})
@GraphQlApiTest
@ActiveProfiles("test")
public class MutationAddCourseMediaRecordTest {

    @Autowired
    private MediaRecordRepository repository;

    @Test
    @Transactional
    @Commit
    void testAddCourseToMediaRecords(final GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecords(repository);
        final UUID course1Id = UUID.randomUUID();
        final UUID course2Id = UUID.randomUUID();
        expectedMediaRecords.get(0).setCourseIds(new ArrayList<>(List.of(course1Id, course2Id)));
        expectedMediaRecords.get(1).setCourseIds(new ArrayList<>(List.of(course1Id)));

        expectedMediaRecords = repository.saveAll(expectedMediaRecords);

        String query = """
                mutation($courseId: UUID!, $mediaRecordIds: [UUID!]!) {
                    mediaRecords: setMediaRecordsForCourse(courseId: $courseId, mediaRecordIds: $mediaRecordIds) {
                        courseIds
                    }
                }
                """;

        tester.document(query)
                .variable("courseId", course2Id)
                .variable("mediaRecordIds", List.of(expectedMediaRecords.get(1).getId()))
                .execute()
                .path("mediaRecords").entityList(MediaRecord.class).hasSize(1)
                .path("mediaRecords[0].courseIds").entityList(UUID.class).hasSize(2).contains(course1Id, course2Id);

        final List<MediaRecordEntity> actualMediaRecords = repository.findAll();
        assertThat(actualMediaRecords).hasSize(2);
        assertThat(actualMediaRecords.get(0).getCourseIds()).contains(course1Id);
        assertThat(actualMediaRecords.get(1).getCourseIds()).contains(course1Id, course2Id);
    }

}
