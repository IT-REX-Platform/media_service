package de.unistuttgart.iste.gits.media_service.controller;


import de.unistuttgart.iste.gits.common.event.ContentChangeEvent;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST Controller Class listening to a dapr Topic.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final MediaService mediaService;

    @Topic(name = "content-changes", pubsubName = "gits")
    @PostMapping(path = "/media-service/content-changes-pubsub")
    public Mono<Void> updateAssociation(@RequestBody(required = false) CloudEvent<ContentChangeEvent> cloudEvent, @RequestHeader Map<String, String> headers){

            return Mono.fromRunnable( () -> mediaService.removeContentIds(cloudEvent.getData()));
    }

}
