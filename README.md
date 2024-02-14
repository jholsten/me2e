# me2e Library for End-to-End Tests for Microservice Systems
me2e (*Microservice End-to-End*) is a library for writing functional End-to-End-Tests for REST APIs of Microservice Systems in JUnit5.
These tests, referred to as *subcutaneous* by [Martin Fowler](https://martinfowler.com/bliki/SubcutaneousTest.html), can be used to verify whether the Microservices work together as expected at the level of their REST over HTTP APIs.

In contrast to testing Monoliths, testing Microservice Systems, which may consist of a multitude of independent components, poses numerous challenges.
One of these challenges is that one transaction usually spans across multiple services, which complicates debugging and makes the data flow difficult to trace.
In addition, the communication through the network increases the risk of *flaky* tests - that is tests that unpredictably produce sometimes positive and sometimes negative results without changes to the code base - which makes the test results unreliable and can no longer be relied upon as a safety net.
Furthermore, the heterogeneity of the individual components of a Microservice System also makes it difficult to set up the test environment on which the End-to-End-Tests are executed.

The fundamental aim of me2e is to reduce these difficulties when developing End-to-End-Tests for Microservice Systems and to simplify writing such tests with as little effort as possible.
To this end, me2e uses [JUnit5](https://junit.org/junit5/) (a.k.a. JUnit Jupiter) as a test framework and [Docker-Compose](https://docs.docker.com/compose/) for defining and setting up the test environment.
The library offers interfaces for the following core functions:
- **Setting up the Test Environment:** Using Docker-Compose, a temporary test environment is started for each execution of the End-to-End-Tests
- **Simulating external Services:** Mocking the REST APIs of third-party services
- **Data Management:** Setting the initial state of databases and resetting their state after each test
- **Executing HTTP Requests and Verifying their Responses**

In addition, a detailed test report is generated after the execution of all tests, which, besides basic metrics such as the success rate and execution times, also shows the logs of all Docker containers, their resource consumption over time and traces of the HTTP requests across the various components.

## Prerequisites
The definition and starting of the test environment relies on Docker and Docker-Compose.
Accordingly, a prerequisite for using this library is that the Microservices are available as Docker images and that Docker-Compose (version 1 or 2) is installed on the system that executes the tests.

## Getting Started
### 1. Set up a new Project
As the End-to-End-Tests affect the entire Microservice System and therefore cannot be assigned to an individual component, a new project should be set up for their development.
You can choose between Java and Kotlin for the programming language and between Gradle and Maven for the build tool.
The following explanations contain instructions for setting up the project with Gradle.
For Maven, the procedure is similar.

- Create a new directory for your project and switch into this directory.
```shell
mkdir e2e
cd e2e
```
- Execute the [Gradle `init`](https://docs.gradle.org/current/userguide/build_init_plugin.html) task.

<details>
    <summary><u>For <b>Java</b> Projects:</u></summary>
    
 ```shell
 gradle init \
   --type java-application \
   --test-framework junit-jupiter \
   --dsl groovy \
   --package com.example.e2e \
   --no-split-project
 ```
</details>

<details open>
    <summary><u>For <b>Kotlin</b> Projects:</u></summary>
    
```shell
gradle init \
  --type kotlin-application \
  --test-framework kotlintest \
  --dsl groovy \
  --package com.example.e2e \
  --no-split-project
```
</details>

### 2. Install the me2e Library
Add the me2e library as a test dependency to your project.

<details>
    <summary><u>For <b>Java</b> Projects:</u></summary>
    
 ```groovy
// build.gradle
dependencies {
     // ...
     testImplementation "org.jholsten:me2e:1.0.0"
     testAnnotationProcessor "org.jholsten:me2e:1.0.0"
}
 ```
</details>

<details open>
    <summary><u>For <b>Kotlin</b> Projects:</u></summary>
In case you are using Kotlin, you need to use the <a href="https://kotlinlang.org/docs/kapt.html">kapt compiler plugin</a> for integrating the library's annotation processor.
    
```groovy
// build.gradle
plugins {
    // ...
    id "org.jetbrains.kotlin.kapt" version "1.8.20" // Should be the same as your Kotlin version
}
dependencies {
     // ...
     testImplementation "org.jholsten:me2e:1.0.0"
     kaptTest "org.jholsten:me2e:1.0.0"
}
 ```
</details>

### 3. Add the Docker-Compose of your Microservice System
Place the Docker-Compose file, which contains all the components of your Microservice System, in the `resources` folder of your newly created test project.
To be able to use the functions of the library as effectively as possible, you should make some configurations using labels in the [`labels` section](https://docs.docker.com/compose/compose-file/compose-file-v3/#labels-2) of the services.
For the most basic configuration, you should use the `org.jholsten.me2e.container-type` label to specify the container type of each service.

```yaml
# docker-compose.yml
services:
  microservice:
    # ...
    labels:
      "org.jholsten.me2e.container-type": "MICROSERVICE"
```

In me2e, we distinguish between 3 different container types:

| Container Type | Description                                                                                                                                                                                                                                                                                             | Represented by Class                                                                                                                                                          |
|:---------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MICROSERVICE` | A Microservice that contains a publicly accessible HTTP REST API.<br/> With containers of this type, you can access their API via an HTTP client.                                                                                                                                                       | [`MicroserviceContainer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/index.html) |
| `DATABASE`     | A container containing a database.<br/> With containers of this type, you can interact with the database by, for example, executing scripts or resetting the database state. Not that only MySQL, PostgreSQL, MariaDB and MongoDB are currently supported by default for interacting with the database. | [`DatabaseContainer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database/-database-container/index.html)             |
| `MISC`         | All other container types that do not contain a publicly accessible REST API and are not database containers.<br/> You do not need to set the label for this type. It is used by default.                                                                                                               | [`Container`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container/-container/index.html)                                       |
 
To ensure that the test execution only starts when all services are completely up and running, you should also define a healthcheck for each service, if it does not already exist.

A minimally configured Microservice definition as part of the Docker-Compose file may look like this:
```yaml
# docker-compose.yml
services:
  delivery-service:
    image: gitlab.informatik.uni-bremen.de:5005/master-thesis1/evaluation/pizza-delivery/delivery-service:latest
    ports:
      - 8082:8082
    healthcheck:
      test: ["CMD-SHELL", "curl --fail http://localhost:8082/health || exit 1"]
      interval: 5s
      timeout: 5s
      retries: 10
    labels:
      "org.jholsten.me2e.container-type": "MICROSERVICE"
```

### 4. Define the me2e-config File
For configuring the test runner, me2e relies on a me2e-config file in which you can define the test environment and adjust additional settings, such as the Docker-Compose version to be used or different timeouts.
You can find more information about the contents and format of the me2e-config file [here](#configuration).
For the minimal configuration, first create a file named `me2e-config.yml` in the `resources` folder of your test project with the following contents:

```yaml
# me2e-config.yml
environment:
  docker-compose: docker-compose.yml
```

### 5. Write your first End-to-End-Test
Now that you have set everything up, you can write your first End-to-End-Test.
To do this, create a test class that inherits from [`org.jholsten.me2e.Me2eTest`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e/-me2e-test/index.html).

```kotlin
class E2ETest : Me2eTest() {
    
}
```

`Me2eTest` is the base class for all End-to-End-Tests, which starts the test environment once during initialization and contains references to all components of the environment.
You can use this class to access the containers via their names in the Docker-Compose file using the [`containerManager`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e/-me2e-test/-companion/container-manager.html).

```kotlin
val deliveryService = containerManager.containers["delivery-service"]
```

However, it is simpler and also the recommended procedure to inject the container instances using the [`@InjectService`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.injection/-inject-service/index.html) annotation.
To do this, define an attribute in your test class with the name of the service to be injected.
The name of the attribute is automatically converted to Kebab-Case and an attempt is made to find a service in the Docker-Compose file that has exactly this key.

```kotlin
class E2ETest : Me2eTest() {

    @InjectService
    private lateinit var deliveryService: MicroserviceContainer
}
```

Alternatively, you can also specify the name of the service explicitly using the `name` parameter in the `@InjectService` annotation.
For more information on the annotation, see the section on [injecting services](#injecting-services).

You can now interact with the container and, in the case of a service of type `MICROSERVICE`, send requests to its REST API via the integrated HTTP client, for example.

```kotlin
val response = deliveryService.get(RelativeUrl("/health"))
```

To verify that the response meets the expectations, use the [`assertThat`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions/assert-that.html) method.
This method, in combination with the [`assertions`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions/), allows you to check all possible properties of the response.
To find out more about the available assertions, take a look [here](#verifying-http-responses).

```kotlin
class E2ETest : Me2eTest() {

    @InjectService
    private lateinit var deliveryService: MicroserviceContainer
    
    @Test
    fun `Requesting health status should return OK`() {
        val response = deliveryService.get(RelativeUrl("/health"))
        
        assertThat(response)
            .statusCode(equalTo(200))
            .body(containsString("OK"))
    }
}
```

### 6. Execute your End-to-End-Test
You can now run the tests either directly in your IDE or via the terminal with the following command:

```shell
./gradlew test
```

Before starting the test execution, the Docker-Compose is started and it is waited until all containers are healthy.
The tests are then executed and finally the report is generated using the [`HtmlReportGenerator`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.report.generator.html/-html-report-generator/index.html), the path of which can be found in the logs.

The following report was generated for the E2E test example above:

| Landing Page                                               | Details                                                     |
|------------------------------------------------------------|-------------------------------------------------------------|
| ![Test Report Example](docs/test_report_example_index.png) | ![Test Report Example](docs/test_report_example_detail.png) |

On the landing page, you can see an overview of all executed test classes and their tests as well as their execution status and duration.
Below the "Executed Tests" section you will also find statistics on the resource usage of all containers of the environment over the entire duration of the test execution.
Finally, in the bottom section you can see the aggregated logs of the Test Runner and all services of your environment that were collected during the execution.
As the environment is started before the actual test execution, you will also find the logs that were output when the environment was started here.

Clicking on the test class takes you to the details page of the report, where you can see the HTTP network traces and the specific logs for a test execution along with basic metrics for each test of a test class.
Please note that the resource usage statistics for a test class are only displayed if data was collected during the execution of the specific test class.
As Docker only sends the statistics entries once per second, you will not see any data at this point if the execution of the class took less than 1 second.

Detailed information on the contents of the test report and its customizability can be found [here](#test-report).


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
To enable forwarding requests to third-party services to the corresponding Mock Server, you need to point the services' hostname to the host's IP address.
For each microservice which is communication with the third-party service, add an [extra_hosts](https://docs.docker.com/compose/compose-file/compose-file-v3/#extra_hosts) entry to the Docker-Compose file.

Example: To forward requests to `example.com` and `google.com` to the corresponding Mock Servers, set:
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
