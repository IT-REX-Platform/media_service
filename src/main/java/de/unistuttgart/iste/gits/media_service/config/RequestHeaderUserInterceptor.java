package de.unistuttgart.iste.gits.media_service.config;

import de.unistuttgart.iste.gits.common.user_handling.RequestHeaderUserProcessor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

/**
 * This class is used to add data from the request headers to the GraphQL context.
 */
@Configuration
public class RequestHeaderUserInterceptor implements WebGraphQlInterceptor {
    @NotNull
    @Override
    @SneakyThrows
    public Mono<WebGraphQlResponse> intercept(@NotNull WebGraphQlRequest request, @NotNull Chain chain) {
        RequestHeaderUserProcessor.process(request);
        return chain.next(request);
    }
}