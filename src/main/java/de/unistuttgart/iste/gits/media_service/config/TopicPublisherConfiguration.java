package de.unistuttgart.iste.gits.media_service.config;

import de.unistuttgart.iste.gits.media_service.dapr.TopicPublisher;
import io.dapr.client.DaprClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


/**
 * This is a prod-config for the TopicPublisher. It uses the dapr client to send messages to the dapr topic.
 */
@Configuration
@Profile("prod")
public class TopicPublisherConfiguration {

    @Bean
    public TopicPublisher getTopicPublisher() {
        return new TopicPublisher(new DaprClientBuilder().build());
    }

}
