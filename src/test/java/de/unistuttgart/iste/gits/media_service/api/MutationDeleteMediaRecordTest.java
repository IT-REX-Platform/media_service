package de.unistuttgart.iste.gits.media_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.gits.media_service.test_config.MockMinIoClientConfiguration;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMemberships;
import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecordsAndCourseIds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@TablesToDelete({"media_record_content_ids","media_record_course_ids", "media_record"})
@Transactional
@GraphQlApiTest
class MutationDeleteMediaRecordTest {

    @Autowired
    private MediaRecordRepository repository;

    private final UUID courseId1 = UUID.randomUUID();
    private final UUID courseId2 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 =
            LoggedInUser.CourseMembership.builder()
                    .courseId(courseId1)
                    .role(LoggedInUser.UserRoleInCourse.ADMINISTRATOR)
                    .startDate(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                    .endDate(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                    .build();
    private final LoggedInUser.CourseMembership courseMembership2 =
            LoggedInUser.CourseMembership.builder()
                    .courseId(courseId2)
                    .role(LoggedInUser.UserRoleInCourse.ADMINISTRATOR)
                    .startDate(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                    .endDate(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                    .build();
    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMemberships(courseMembership1, courseMembership2);

    @Test
    void testDeleteMediaRecord(final GraphQlTester tester) {
        List<MediaRecordEntity> createdMediaRecords = fillRepositoryWithMediaRecordsAndCourseIds(repository, courseId1, courseId2);

        createdMediaRecords = repository.saveAll(createdMediaRecords);


        final String query = """
                mutation {
                    deleteMediaRecord(id: "%s")
                }
                """.formatted(createdMediaRecords.get(0).getId());

        tester.document(query)
                .execute()
                .path("deleteMediaRecord").entity(UUID.class).isEqualTo(createdMediaRecords.get(0).getId());

        // ensure that the media record left in the db is the other one (the one we didn't delete)
        assertThat(repository.count(), is((long) createdMediaRecords.size() - 1));
        final MediaRecordEntity remainingMediaRecord = repository.findAll().get(0);
        assertThat(remainingMediaRecord, equalTo(createdMediaRecords.get(1)));
    }
}
