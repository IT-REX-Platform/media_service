package de.unistuttgart.iste.gits.media_service.test_config;

import io.minio.MinioClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockMinIoClientConfiguration {
    @Primary
    @Bean
    public MinioClient getTestMinIoClient() throws Exception {
        final MinioClient client = Mockito.mock(MinioClient.class);

        Mockito.when(client.getPresignedObjectUrl(Mockito.any())).thenReturn("http://example.com");

        return client;
    }
}
