package de.unistuttgart.iste.gits.media_service.test_util;

import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CourseMembershipUtil {

    public static LoggedInUser.CourseMembership dummyCourseMembershipBuilder(final UUID courseId) {
        return LoggedInUser.CourseMembership.builder()
                        .courseId(courseId)
                        .role(LoggedInUser.UserRoleInCourse.ADMINISTRATOR)
                        .startDate(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                        .endDate(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                        .build();
    }

    public static LoggedInUser.CourseMembership dummyCourseMembershipBuilderWithRole(final UUID courseId, final LoggedInUser.UserRoleInCourse role) {
        return LoggedInUser.CourseMembership.builder()
                .courseId(courseId)
                .role(role)
                .startDate(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                .endDate(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                .build();
    }
}
