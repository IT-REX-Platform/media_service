package de.unistuttgart.iste.gits.media_service.dapr;

import de.unistuttgart.iste.gits.common.event.*;
import de.unistuttgart.iste.gits.media_service.persistence.entity.MediaRecordEntity;
import io.dapr.client.DaprClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Component that takes care of publishing messages to a dapr Topic
 */
@Slf4j
public class TopicPublisher {

    private static final String PUBSUB_NAME = "gits";
    private static final String RESOURCE_UPDATE_TOPIC = "resource-update";

    private static final String USER_PROGRESS_LOG_TOPIC = "content-progressed";

    private final DaprClient client;

    public TopicPublisher(DaprClient client) {
        this.client = client;
    }

    /**
     * method used to publish dapr messages to a topic
     *
     * @param dto message
     */
    private void publishChanges(ResourceUpdateEvent dto) {
        log.debug("publishing message");
        client.publishEvent(
                PUBSUB_NAME,
                RESOURCE_UPDATE_TOPIC,
                dto).block();
    }

    /**
     * method to take changes done to an entity and to transmit them to the dapr topic
     *
     * @param mediaRecordEntity changed entity
     * @param operation         type of CRUD operation performed on entity
     */
    public void notifyResourceChange(MediaRecordEntity mediaRecordEntity, CrudOperation operation) {
        ResourceUpdateEvent dto = ResourceUpdateEvent.builder()
                .entityId(mediaRecordEntity.getId())
                .contentIds(mediaRecordEntity.getContentIds())
                .operation(operation).build();
        publishChanges(dto);
    }

    /**
     * Publishes a message to the dapr topic that a user has worked on a content
     *
     * @param userProgressLogEvent event to publish
     */
    public void notifyUserWorkedOnContent(UserProgressLogEvent userProgressLogEvent) {
        client.publishEvent(PUBSUB_NAME, USER_PROGRESS_LOG_TOPIC, userProgressLogEvent).block();
    }

}
