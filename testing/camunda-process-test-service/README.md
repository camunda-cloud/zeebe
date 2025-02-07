# Camunda-Process-Test-Service

Work in progress!

## Build docker image

```
mvn clean install jib-maven-plugin:dockerBuild
```

## Run Docker image locally

```
docker run -it --rm -v $PWD:$PWD -w $PWD -v /var/run/docker.sock:/var/run/docker.sock -p 8080:8080 ghcr.io/camunda-community-hub/cpt-service
```

Refer to [Testcontainers configuration](https://java.testcontainers.org/supported_docker_environment/continuous_integration/dind_patterns/).
