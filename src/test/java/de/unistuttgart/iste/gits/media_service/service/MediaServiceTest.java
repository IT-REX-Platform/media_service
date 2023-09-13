package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.gits.media_service.test_config.MockMinIoClientConfiguration;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecords;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@TablesToDelete({"media_record_content_ids", "media_record"})
@Transactional
@SpringBootTest
@Testcontainers
class MediaServiceTest {

    @Autowired
    private MediaService service;

    @Autowired
    private MediaRecordRepository repository;

    ModelMapper mapper = new ModelMapper();

    @Test
    void testRequireMediaRecordExisting() {
        List<MediaRecordEntity> entities = fillRepositoryWithMediaRecords(repository);

        assertDoesNotThrow(() -> service.requireMediaRecordExisting(entities.get(0).getId()));
        UUID notExistingId = UUID.randomUUID();
        assertThrows(EntityNotFoundException.class, () -> service.requireMediaRecordExisting(notExistingId));
    }

    @Test
    void testGetMediaRecordById() {
        List<MediaRecordEntity> entities = fillRepositoryWithMediaRecords(repository);

        MediaRecordEntity expected = entities.get(0);
        MediaRecordEntity actual = mapper.map(service.getMediaRecordById(expected.getId()), MediaRecordEntity.class);

        assertThat(actual, is(expected));
    }
}
