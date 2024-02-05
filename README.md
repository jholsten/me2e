# me2e Library for End-to-End Tests for Microservice Applications

me2e (Microservice End-to-End) is a library for writing end-to-end tests for microservice applications.

## Usage
### Import
The library is published to the [GitLab Package Registry](https://gitlab.informatik.uni-bremen.de/api/v4/projects/33508/packages/maven).
To import this package, follow the instructions below depending on whether the importing project uses Maven or Gradle.

#### Import in Maven projects
Add the following entries to the `pom.xml` of your project to access the GitLab Package Registry.

```xml
<repositories>
    <repository>
        <id>gitlab-maven</id>
        <url>https://gitlab.informatik.uni-bremen.de/api/v4/projects/33508/packages/maven</url>
    </repository>
</repositories>

<distributionManagement>
    <repository>
        <id>gitlab-maven</id>
        <url>https://gitlab.informatik.uni-bremen.de/api/v4/projects/33508/packages/maven</url>
    </repository>
    
    <snapshotRepository>
        <id>gitlab-maven</id>
        <url>https://gitlab.informatik.uni-bremen.de/api/v4/projects/33508/packages/maven</url>
    </snapshotRepository>
</distributionManagement>
```

Accessing the GitLab Package Registry requires authenticating via a token.
To authenticate with the Deploy-Token of this project, create a file named `settings.xml` in the root directory of your project and add the following contents.

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>gitlab-maven</id>
            <configuration>
                <httpHeaders>
                    <property>
                        <name>Deploy-Token</name>
                        <value>W-yPs8LvVqmfsWCSpBPq</value>
                    </property>
                </httpHeaders>
            </configuration>
        </server>
    </servers>
</settings>
```

To use these settings, you may need to configure your IDE.
For instance for IntelliJ IDEA, set the path to the newly created file in the project settings.

![IntelliJ Settings](docs/intellij_config_settings.png)

Now you can include the library as a dependency with the selected version.

```xml
<dependencies>
    <dependency>
        <groupId>org.jholsten</groupId>
        <artifactId>me2e</artifactId>
        <version>$me2eVersion</version>
    </dependency>
</dependencies>
```

#### Import in Gradle Projects
Add the following entry to the `repositories` section of the project's `build.gradle` file.

```groovy
repositories {
    maven {
        url "https://gitlab.informatik.uni-bremen.de/api/v4/projects/33508/packages/maven"
        name "GitLab"
        credentials(HttpHeaderCredentials) {
            name = 'Deploy-Token'
            value = 'W-yPs8LvVqmfsWCSpBPq'
        }
        authentication {
            header(HttpHeaderAuthentication)
        }
    }
}
```

The library can then be included as a dependency with the selected version.

```groovy
dependencies {
    implementation "org.jholsten:me2e:$me2eVersion"
}
```

### Configuration

To configure the End-to-End test and define which services should be started, you need to define a test configuration.

TODO: How to set ME2E-Environment in Docker-Containers?

- Spring: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.external-config.files
- via Environment-Variables

#### Mock Server Configuration
To enable forwarding requests to third-party services to the corresponding mock server, you need to point the services' hostname to the host's IP address.
For each microservice which is communication with the third-party service, add an [extra_hosts](https://docs.docker.com/compose/compose-file/compose-file-v3/#extra_hosts) entry to the Docker-Compose file.

Example: To forward requests to `example.com` and `google.com` to the corresponding mock servers, set:
```yaml
    extra_hosts:
      - "example.com:host-gateway"
      - "google.com:host-gateway"
```

In case you are receiving 403 errors and your operating system is Windows, you may need to [disable the automatic proxy detection](https://support.rezstream.com/hc/en-us/articles/360025156853-Disable-Auto-Proxy-Settings-in-Windows-10) (see [GitHub Issue](https://github.com/docker/for-win/issues/13127)).

------------
We recommend to not set the public port of the services, as this may lead to port conflicts.
Instead, only set the internal port, and Docker will assign a port automatically.
------------

## Private Registries
```
docker login -u REGISTRY_USER -p URhN8CYa_Ks_AAvucNMg gitlab.informatik.uni-bremen.de:5005
```

## Run E2E-Tests inside GitLab CI
### With Docker Cache:
```    
test:
  stage: test
  image: $CI_REGISTRY_IMAGE:test
  services:
    - name: docker:20.10.20-dind
      alias: docker
      command: [ "--tls=false" ]
  before_script:
    - chmod +x ./gradlew
    - if [[ -f "docker/cache.tar" ]]; then
      echo "Loading Docker Image Cache...";
      docker load -i docker/cache.tar;
      fi
  script:
    - docker login -u REGISTRY_TOKEN -p $REGISTRY_TOKEN $CI_REGISTRY
    - export RUNNER_IP=$(hostname -I)
    - ./gradlew --build-cache test
  after_script:
    - echo "Storing Docker Image Cache..."
    - mkdir -p docker
    - docker save $(docker images -q) -o docker/cache.tar
  cache:
    - <<: *gradle_cache
    - key: docker-cache
      paths:
        - docker/
  variables:
    DOCKER_HOST: "tcp://docker:2375"
  artifacts:
    when: always
    reports:
      junit: build/test-results/test/**/TEST-*.xml
```


### In case you can configure the GitLab Runner
https://java.testcontainers.org/supported_docker_environment/continuous_integration/gitlab_ci/

#### /etc/gitlab-runner/config.toml
```
[[runners]]
  name = "MACHINE_NAME"
  url = "https://gitlab.com/"
  token = "GENERATED_GITLAB_RUNNER_TOKEN"
  executor = "docker"
  [runners.custom_build_dir]
  [runners.cache]
    [runners.cache.s3]
    [runners.cache.gcs]
    [runners.cache.azure]
  [runners.docker]
    tls_verify = false
    image = "docker:20.10.20"
    privileged = true
    disable_entrypoint_overwrite = false
    oom_kill_disable = false
    disable_cache = false
    volumes = ["/var/run/docker.sock:/var/run/docker.sock", "/cache"]
    cache_dir = "/cache"
    shm_size = 0
    extra_hosts=["host.docker.internal:host-gateway"]
```

#### gitlab-ci.yml
```
variables:
  DOCKER_TLS_CERTDIR: ""
  DOCKER_DRIVER: overlay2

test:
  stage: test
  image: $CI_REGISTRY_IMAGE:test
  services:
    - name: docker:20.10.20-dind
      alias: docker
      command: [ "--tls=false" ]
  before_script:
    - chmod +x ./gradlew
  script:
    - docker login -u REGISTRY_TOKEN -p $REGISTRY_TOKEN $CI_REGISTRY
    - ./gradlew test
  variables:
    TESTCONTAINERS_HOST_OVERRIDE: "host.docker.internal"
```

#### Dockerfile
```
FROM openjdk:17.0.2-jdk-slim

RUN apt-get update && \
    apt-get install -y curl

RUN curl -fsSL https://get.docker.com -o get-docker.sh
RUN sh get-docker.sh
```

### In case you cannot configure the GitLab Runner
#### /etc/gitlab-runner/config.toml
```
[[runners]]
  name = "MACHINE_NAME"
  url = "https://gitlab.com/"
  token = "GENERATED_GITLAB_RUNNER_TOKEN"
  executor = "docker"
  [runners.custom_build_dir]
  [runners.cache]
    [runners.cache.s3]
    [runners.cache.gcs]
    [runners.cache.azure]
  [runners.docker]
    tls_verify = false
    image = "docker:20.10.20"
    privileged = true
    disable_entrypoint_overwrite = false
    oom_kill_disable = false
    disable_cache = false
    volumes = ["/cache"]
    cache_dir = "/cache"
    shm_size = 0
```

#### gitlab-ci.yml
```
variables:
  DOCKER_TLS_CERTDIR: ""
  DOCKER_DRIVER: overlay2

test:
  stage: test
  image: docker:20.10.20
  services:
    - name: docker:20.10.20-dind
      alias: docker
      command: [ "--tls=false" ]
  before_script:
    - chmod +x ./gradlew
  script:
    - docker login -u REGISTRY_TOKEN -p $REGISTRY_TOKEN $CI_REGISTRY
    - cp /root/.docker/config.json $CI_PROJECT_DIR/config.json
    - docker run --rm -v $PWD:$PWD -w $PWD
      -v /var/run/docker.sock:/var/run/docker.sock
      -v $CI_PROJECT_DIR/config.json:/root/.docker/config.json
      -e GRADLE_USER_HOME=$CI_PROJECT_DIR/.gradle
      -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal
      --add-host=host.docker.internal:host-gateway
      $CI_REGISTRY_IMAGE:test
```

#### Dockerfile
```
# Dockerfile to use for tests. Includes JDK, Docker and Docker-Compose.
FROM openjdk:17.0.2-jdk-slim

RUN apt-get update && \
    apt-get install -y curl

RUN curl -fsSL https://get.docker.com -o get-docker.sh
RUN sh get-docker.sh

CMD ./gradlew test
```

## Mock Server Authentication
### Generate Keystore
https://gist.github.com/dentys/1bdd2897a53b1a8b56007a480243c33a
https://www.ivankrizsan.se/2018/03/03/mocking-http-services-with-wiremock-part-2/

```shell
keytool -genkey -keyalg RSA -keysize 2048 -alias mock_server -validity 3650 -keypass mock_server -keystore mock_server_keystore.jks -storepass mock_server -ext SAN=dns:example.com,dns:payment.example.com
```

### Generate Server Certificate
```shell
keytool -exportcert -alias mock_server -keystore mock_server_keystore.jks -storepass mock_server -file mock_server_certificate.crt
openssl x509 -inform der -in mock_server_certificate.crt -out mock_server_certificate.pem
```
