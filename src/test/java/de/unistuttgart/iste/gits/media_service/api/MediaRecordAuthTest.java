package de.unistuttgart.iste.gits.media_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.generated.dto.MediaRecord;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMemberships;
import static de.unistuttgart.iste.gits.media_service.test_util.CourseMembershipUtil.dummyCourseMembershipBuilder;
import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecordsAndCourseIds;

@TablesToDelete({"media_record_course_ids", "media_record_content_ids", "media_record"})
@GraphQlApiTest
@ActiveProfiles("test")
public class MediaRecordAuthTest {

    @Autowired
    private MediaRecordRepository repository;

    private final ModelMapper mapper = new ModelMapper();

    private final UUID courseId1 = UUID.randomUUID();
    private final UUID courseId2 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = dummyCourseMembershipBuilder(courseId1);
    private final LoggedInUser.CourseMembership courseMembership2 = dummyCourseMembershipBuilder(courseId2);

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMemberships(courseMembership1, courseMembership2);

    @Test
    void testQueryMediaRecordsByIds(final GraphQlTester tester) {
        final List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecordsAndCourseIds(repository, courseId1, courseId2);

        final String query = """
                query {
                    mediaRecordsByIds(ids: ["%s", "%s"]) {
                        id,
                        courseIds,
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

}
