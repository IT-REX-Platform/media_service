package de.unistuttgart.iste.gits.media_service.config;

import de.unistuttgart.iste.gits.common.dapr.CrudOperation;
import de.unistuttgart.iste.gits.media_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
@Slf4j
public class DevTopicPublisherConfiguration {

    @Bean
    public TopicPublisher getTopicPublisher() {
        log.warn("TopicPublisher is mocked. This is intended for development use only.");
        return new MockTopicPublisher();
    }

    @Slf4j
    static class MockTopicPublisher extends TopicPublisher {

        public MockTopicPublisher() {
            super(null);
        }

        @Override
        public void notifyChange(MediaRecordEntity mediaRecordEntity, CrudOperation operation) {
            log.info("notifyChange called with {} and {}", mediaRecordEntity, operation);
        }
    }
}
