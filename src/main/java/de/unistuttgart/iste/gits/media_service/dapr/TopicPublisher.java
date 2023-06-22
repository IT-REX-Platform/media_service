package de.unistuttgart.iste.gits.media_service.dapr;

import de.unistuttgart.iste.gits.common.dapr.CrudOperation;
import de.unistuttgart.iste.gits.common.dapr.ResourceUpdateDTO;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Component that takes care of publishing messages to a dapr Topic
 */
@Component
@Slf4j
public class TopicPublisher {

    private static final String PUBSUB_NAME = "gits";
    private static final String TOPIC_NAME = "resource-update";

    private final DaprClient client;

    public TopicPublisher(){
        client = new DaprClientBuilder().build();
    }

    /**
     * method used to publish dapr messages to a topic
     * @param dto message
     */
    private void publishChanges(ResourceUpdateDTO dto){
        log.info("publishing message");
        client.publishEvent(
                PUBSUB_NAME,
                TOPIC_NAME,
                dto).block();
    }

    /**
     * method to take changes done to an entity and to transmit them to the dapr topic
     * @param mediaRecordEntity changed entity
     * @param operation type of CRUD operation performed on entity
     */
    public void notifyChange(MediaRecordEntity mediaRecordEntity, CrudOperation operation){

        ResourceUpdateDTO dto = ResourceUpdateDTO.builder()
                .entityId(mediaRecordEntity.getId())
                .contentIds(mediaRecordEntity.getContentIds())
                .operation(operation).build();
        publishChanges(dto);
    }


}
