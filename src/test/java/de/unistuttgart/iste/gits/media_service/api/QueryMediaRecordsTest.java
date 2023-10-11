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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;
import static de.unistuttgart.iste.gits.media_service.test_util.CourseMembershipUtil.dummyCourseMembershipBuilder;
import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecords;
import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecordsAndCourseIds;

@GraphQlApiTest
@Transactional
@TablesToDelete({"media_record_content_ids", "media_record_course_ids", "media_record"})
@ActiveProfiles("test")
class QueryMediaRecordsTest {

    @Autowired
    private MediaRecordRepository repository;

    private final ModelMapper mapper = new ModelMapper();

    private final UUID courseId1 = UUID.randomUUID();
    private final UUID courseId2 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = dummyCourseMembershipBuilder(courseId1);

    private final LoggedInUser.CourseMembership courseMembership2 = dummyCourseMembershipBuilder(courseId2);

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMembershipsAndRealmRoles(Set.of(LoggedInUser.RealmRole.SUPER_USER), courseMembership1,  courseMembership2);

    @Test
    void testQueryAllMediaRecordsEmpty(final GraphQlTester tester) {
        final String query = """
                query {
                    mediaRecords {
                        id,
                        courseIds,
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
    void testQueryAllMediaRecords(final GraphQlTester tester) {
        final List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecordsAndCourseIds(repository, courseId1, courseId2);

        final String query = """
                query {
                    mediaRecords {
                        id,
                        courseIds,
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
    void testQueryMediaRecordsByIds(final GraphQlTester tester) {
        final List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecords(repository);

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

    @Test
    void testQueryFindMediaRecordsByIds(final GraphQlTester tester) {
        final List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecordsAndCourseIds(repository, courseId1, courseId2);

        final UUID nonexistantUUID = UUID.randomUUID();

        final String query = """
                query($ids: [UUID!]!) {
                    findMediaRecordsByIds(ids: $ids) {
                        id,
                        courseIds,
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
    void testQueryMediaRecordsByContentIds(final GraphQlTester tester) {
        final List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecordsAndCourseIds(repository, courseId1, courseId2);

        final String query = """
                query {
                    mediaRecordsByContentIds(contentIds: ["%s", "%s"]) {
                        id,
                        courseIds,
                        name,
                        creatorId,
                        type,
                        contentIds
                    }
                }
                """.formatted(expectedMediaRecords.get(0).getContentIds().get(0),
                expectedMediaRecords.get(1).getContentIds().get(0));

        final GraphQlTester.Response response = tester.document(query).execute();

        response.path("mediaRecordsByContentIds").entityList(List.class).hasSize(2);

        response.path("mediaRecordsByContentIds[0]").entityList(MediaRecord.class)
                .hasSize(1)
                .contains(mapper.map(expectedMediaRecords.get(0), MediaRecord.class));

        response.path("mediaRecordsByContentIds[1]").entityList(MediaRecord.class)
                .hasSize(1)
                .contains(mapper.map(expectedMediaRecords.get(1), MediaRecord.class));
    }
}
