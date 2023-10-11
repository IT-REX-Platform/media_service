package de.unistuttgart.iste.gits.media_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
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

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMemberships;
import static de.unistuttgart.iste.gits.media_service.test_util.CourseMembershipUtil.dummyCourseMembershipBuilder;
import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecords;
import static org.assertj.core.api.Assertions.assertThat;

@TablesToDelete({"media_record_course_ids", "media_record_content_ids", "media_record"})
@GraphQlApiTest
@ActiveProfiles("test")
public class MutationAddCourseMediaRecordTest {

    @Autowired
    private MediaRecordRepository repository;

    private final UUID courseId1 = UUID.randomUUID();
    private final UUID courseId2 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = dummyCourseMembershipBuilder(courseId1);
    private final LoggedInUser.CourseMembership courseMembership2 = dummyCourseMembershipBuilder(courseId2);


    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMemberships(courseMembership1, courseMembership2);

    @Test
    @Transactional
    @Commit
    void testAddCourseToMediaRecords(final GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecords(repository);
        expectedMediaRecords.get(0).setCourseIds(new ArrayList<>(List.of(courseId1, courseId2)));
        expectedMediaRecords.get(1).setCourseIds(new ArrayList<>(List.of(courseId1)));

        expectedMediaRecords = repository.saveAll(expectedMediaRecords);

        final String query = """
                mutation($courseId: UUID!, $mediaRecordIds: [UUID!]!) {
                    mediaRecords: setMediaRecordsForCourse(courseId: $courseId, mediaRecordIds: $mediaRecordIds) {
                        courseIds
                    }
                }
                """;

        tester.document(query)
                .variable("courseId", courseId2)
                .variable("mediaRecordIds", List.of(expectedMediaRecords.get(1).getId()))
                .execute()
                .path("mediaRecords").entityList(MediaRecord.class).hasSize(1)
                .path("mediaRecords[0].courseIds").entityList(UUID.class).hasSize(2).contains(courseId1, courseId2);

        final List<MediaRecordEntity> actualMediaRecords = repository.findAll();
        assertThat(actualMediaRecords).hasSize(2);
        assertThat(actualMediaRecords.get(0).getCourseIds()).contains(courseId1);
        assertThat(actualMediaRecords.get(1).getCourseIds()).contains(courseId1, courseId2);
    }

}
