package de.unistuttgart.iste.gits.media_service;

import de.unistuttgart.iste.gits.generated.dto.MediaType;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;


/**
 * This is the entry point of the application.
 */
@SpringBootApplication
@Slf4j
@EnableScheduling
public class MediaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }

    @Component
    @Profile("!test")
    public static class CommandLineAppStartupRunner implements CommandLineRunner {

        private final MinioClient minioInternalClient;

        public CommandLineAppStartupRunner(MinioClient minioInternalClient) {
            this.minioInternalClient = minioInternalClient;
        }

        @Override
        @SneakyThrows
        public void run(String...args) {
            List<String> buckets =  Arrays.stream(MediaType.values()).map(type -> type.toString().toLowerCase()).toList();

            for (String bucket : buckets) {
                boolean bucket_exists = minioInternalClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!bucket_exists) {
                    minioInternalClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Bucket {} created.", bucket);
                } else {
                    log.info("Bucket {} already exists.", bucket);
                }
            }
        }
    }

}
