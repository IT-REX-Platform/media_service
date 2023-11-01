
# Media Service

The Media Service is a component designed to fulfill the following functions within the platform:

1. **Content Upload and Management:** It provides a platform for educators to upload and manage multimedia content, including PDF documents, MP4 videos, PowerPoint presentations (PPTX), and more.

2. **MinIO Object Storage Integration:** To ensure efficient storage and accessibility, the service utilizes the MinIO object storage system. Different types of media are stored in separate buckets within MinIO, allowing for structured storage.

3. **GraphQL Endpoint:** Users can interact with the service through a GraphQL endpoint, enabling the retrieval of URLs for uploading and downloading media files.



## Media Service

## Environment variables

### Relevant for deployment

| Name                       | Description                               | Value in Dev Environment                       | Value in Prod Environment                                        |
|----------------------------|-------------------------------------------|------------------------------------------------|------------------------------------------------------------------|
| spring.datasource.url      | PostgreSQL database URL                   | jdbc:postgresql://localhost:3032/media_service | jdbc:postgresql://media-service-db-postgresql:5432/media-service |
| spring.datasource.username | Database username                         | root                                           | gits                                                             |
| spring.datasource.password | Database password                         | root                                           | *secret*                                                         |
| DAPR_HTTP_PORT             | Dapr HTTP Port                            | 3000                                           | 3500                                                             |
| MINIO_ACCESS_KEY           | Minio Access Key                          | minioadmin                                     | gits                                                             |
| MINIO_ACCESS_SECRET        | Minio Access Secret (Password/Secret Key) | *secret*                                       | *secret*                                                         |
| MINIO_URL                  | URL of the Minio server                   | http://localhost:3010                          | http://minio:9000/                                               |
| minio.external.url         | URL of external minio                     | https://minio.it-rex.ch/                       | https://minio.it-rex.ch/                                         |
| server.port                | Port on which the application runs        | 3001                                           | 3001                                                             |
 | mediarecord.delete.cron   | Cron Expression for deleting mediarecords | 0 0 3 * * *                                    | 0 0 3 * * *                                                      |

### Other properties
| Name                                      | Description                               | Value in Dev Environment                | Value in Prod Environment               |
|-------------------------------------------|-------------------------------------------|-----------------------------------------|-----------------------------------------|
| spring.graphql.graphiql.enabled           | Enable GraphiQL web interface for GraphQL | true                                    | true                                    |
| spring.graphql.graphiql.path              | Path for GraphiQL when enabled            | /graphiql                               | /graphiql                               |
| spring.profiles.active                    | Active Spring profile                     | dev                                     | prod                                    |
| spring.jpa.properties.hibernate.dialect   | Hibernate dialect for PostgreSQL          | org.hibernate.dialect.PostgreSQLDialect | org.hibernate.dialect.PostgreSQLDialect |
| spring.datasource.driver-class-name       | JDBC driver class                         | org.postgresql.Driver                   | org.postgresql.Driver                   |
| spring.sql.init.mode                      | SQL initialization mode                   | always                                  | always                                  |
| spring.jpa.show-sql                       | Show SQL queries in logs                  | true                                    | false                                   |
| spring.sql.init.continue-on-error         | Continue on SQL init error                | true                                    | true                                    |
| spring.jpa.hibernate.ddl-auto             | Hibernate DDL auto strategy               | create                                  | update                                  |
| hibernate.create_empty_composites.enabled | Enable empty composite types in Hibernate | true                                    | true                                    |
| DAPR_GRPC_PORT                            | Dapr gRPC Port                            | -                                       | 50001                                   |

## GraphQL API

The API is documented in the [api.md file](api.md).

It can be accessed at `/graphql` and explored via the GraphiQL Playground at `/graphiql`.

## Get started

A guide how to start development can be
found in the [wiki](https://gits-enpro.readthedocs.io/en/latest/dev-manuals/backend/get-started.html).


