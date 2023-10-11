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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMemberships;
import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecordsAndCourseIds;
import static org.assertj.core.api.Assertions.assertThat;

@TablesToDelete({"media_record_content_ids", "media_record_course_ids", "media_record"})
@GraphQlApiTest
@ActiveProfiles("test")
class MutationLinkMediaRecordsWithContentTest {

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
    @Transactional
    @Commit
    void testLinkMediaRecordsWithContent(final GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecordsAndCourseIds(repository, courseId1, courseId2);
        final UUID content1Id = UUID.randomUUID();
        final UUID content2Id = UUID.randomUUID();
        expectedMediaRecords.get(0).setContentIds(new ArrayList<>(List.of(content1Id, content2Id)));
        expectedMediaRecords.get(1).setContentIds(new ArrayList<>(List.of(content1Id)));

        expectedMediaRecords = repository.saveAll(expectedMediaRecords);

        final String query = """
                mutation($contentId: UUID!, $mediaRecordIds: [UUID!]!) {
                    mediaRecords: setLinkedMediaRecordsForContent(contentId: $contentId, mediaRecordIds: $mediaRecordIds) {
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

        final List<MediaRecordEntity> actualMediaRecords = repository.findAll();
        assertThat(actualMediaRecords).hasSize(2);
        assertThat(actualMediaRecords.get(0).getContentIds()).contains(content1Id);
        assertThat(actualMediaRecords.get(1).getContentIds()).contains(content1Id, content2Id);
    }
}
