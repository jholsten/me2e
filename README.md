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

## Table of Contents
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [1. Set up a new Project](#1-set-up-a-new-project)
  - [2. Install the me2e Library](#2-install-the-me2e-library)
  - [3. Add the Docker-Compose of your Microservice System](#3-add-the-docker-compose-of-your-microservice-system)
  - [4. Define the me2e-config File](#4-define-the-me2e-config-file)
  - [5. Write your first End-to-End-Test](#5-write-your-first-end-to-end-test)
  - [6. Execute your End-to-End-Test](#6-execute-your-end-to-end-test)
- [Usage](#usage)
  - [Configuration](#configuration)
    - [Configuring the Execution Settings](#configuring-the-execution-settings)
    - [Defining the Test Environment for the Microservice System](#defining-the-test-environment-for-the-microservice-system)
      - [Recommendations for the Definition of Services in the Docker-Compose File](#recommendations-for-the-definition-of-services-in-the-docker-compose-file)
      - [Specific Configuration for Containers of type `MICROSERVICE`](#specific-configuration-for-containers-of-type-microservice)
      - [Specific Configuration for Containers of type `DATABASE`](#specific-configuration-for-containers-of-type-database)
    - [Mock Servers: Simulating external services](#mock-servers-simulating-external-services)
      - [Mock Server Definition](#mock-server-definition)
      - [Stub Definition](#stub-definition)
      - [DNS Configuration](#dns-configuration)
      - [TLS Configuration](#tls-configuration)
    - [Data Management](#data-management)
      - [Setting an initial State](#setting-an-initial-state)
      - [Resetting the State](#resetting-the-state)
      - [Data Management for other Database Management Systems](#data-management-for-other-database-management-systems)
  - [Defining Tests](#defining-tests)
    - [Injecting services](#injecting-services)
    - [Interacting with Containers](#interacting-with-containers)
    - [Interacting with Databases](#interacting-with-databases)
    - [Executing HTTP Requests and Verifying their Responses](#executing-http-requests-and-verifying-their-responses)
    - [Mock Server Verification](#mock-server-verification)
  - [Measures against Flaky Tests](#measures-against-flaky-tests)
    - [Assert Healthy](#assert-healthy)
    - [Request-Retry](#request-retry)
    - [State Reset](#state-reset)
  - [Test Report](#test-report)
    - [Contents](#contents)
    - [Customizing the Report](#customizing-the-report)
- [Running the End-to-End-Tests inside GitLab CI](#running-the-end-to-end-tests-inside-gitlab-ci)
  - [Caching](#caching)
  - [Publishing Test Reports](#publishing-test-reports)
  - [Bringing it all together: Recommended GitLab CI](#bringing-it-all-together-recommended-gitlab-ci)


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
    <summary><ins>For <b>Java</b> Projects:</ins></summary>
    
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
    <summary><ins>For <b>Kotlin</b> Projects:</ins></summary>
    
```shell
gradle init \
  --type kotlin-application \
  --test-framework kotlintest \
  --dsl groovy \
  --package com.example.e2e \
  --no-split-project
```
</details>

<br/>

> The files that are created by default by the Gradle `init` task (i.e. `App` and `AppTest`) can be deleted.
> Just remember to also delete the `application` task in your project's `build.gradle`, as the `App` class is referenced here.


### 2. Install the me2e Library
The library is published to the [GitLab Package Registry](https://gitlab.informatik.uni-bremen.de/api/v4/projects/33508/packages/maven).
To be able to download the me2e library from this repository, you first need to add the GitLab Package Registry to the repositories section of your project's `build.gradle` file.

```groovy
repositories {
    mavenCentral()
    
    maven {
        url "https://gitlab.informatik.uni-bremen.de/api/v4/projects/33508/packages/maven"
        name "GitLab"
    }
}
```

Now add the me2e library as a test dependency to your project.

<details>
    <summary><ins>For <b>Java</b> Projects:</ins></summary>
    
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
    <summary><ins>For <b>Kotlin</b> Projects:</ins></summary>
    
In case you are using Kotlin, you need to use the [kapt compiler plugin](https://kotlinlang.org/docs/kapt.html) for integrating the library's annotation processor.
    
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

<a name="container-types"></a>

| Container Type | Description                                                                                                                                                                                                                                                                                              | Represented by Class                                                                                                                                                          |
|:---------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MICROSERVICE` | A Microservice that contains a publicly accessible HTTP REST API.<br/> With containers of this type, you can access their API via an HTTP client.                                                                                                                                                        | [`MicroserviceContainer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/index.html) |
| `DATABASE`     | A container containing a database.<br/> With containers of this type, you can interact with the database by, for example, executing scripts or resetting the database state. Note that only MySQL, PostgreSQL, MariaDB and MongoDB are currently supported by default for interacting with the database. | [`DatabaseContainer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database/-database-container/index.html)             |
| `MISC`         | All other container types that do not contain a publicly accessible REST API and are not database containers.<br/> You do not need to set the label for this type. It is used by default.                                                                                                                | [`Container`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container/-container/index.html)                                       |
 
To ensure that the test execution only starts when all services are completely up and running, you should also define a healthcheck for each service, if it does not already exist.

A minimally configured Microservice definition as part of the Docker-Compose file may look like this:
```yaml
# docker-compose.yml
services:
  delivery-service:
    image: gitlab.informatik.uni-bremen.de:5005/master-thesis1/evaluation/pizza-delivery/delivery-service:latest
    ports:
      - 8082
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

#### Recommendation: Enable Autocompletion in your IDE
To enable autocompletion and obtain descriptions for the fields of the `me2e-config.yml` file as well as for the [stub definitions for the Mock Servers](#stub-definition), you may load the corresponding JSON schema into the IDE.
You can find the JSON schemas for these files here:
- me2e-config files: https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/json-schemas/config_schema.json
- stub definition files: https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/json-schemas/stub_schema.json

If you are using IntelliJ, you can simply just download the [jsonSchemas.xml](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/json-schemas/jsonSchemas.xml) file and place it in the ".idea" folder of your test project.
After you have reloaded your project, IntelliJ will automatically link all files matching the pattern `me2e-config*.yml` to the config schema and all files matching `*stub.json` to the stub schema.


### 5. Write your first End-to-End-Test
Now that you have set everything up, you can write your first End-to-End-Test.
To do this, create a test class that inherits from [`org.jholsten.me2e.Me2eTest`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e/-me2e-test/index.html).

```kotlin
class E2ETest : Me2eTest() {
    
}
```

`Me2eTest` is the base class for all End-to-End-Tests, which contains references to all components of the environment.
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
In the following, the configuration options, a more detailed description of the options for creating tests as well as the test report are provided.
For the detailed Kotlin documentation, take a look [here](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/index.html).

### Configuration
The configuration of the me2e library is mainly realized in the me2e-config file in YAML format.
The schema is divided into two sections at the top level:
- `settings`: Settings that affect the runtime execution of the tests, interactions with Docker and with the containers
- `environment`: Definition of the test environment

To enable autocompletion and obtain descriptions for the fields of the me2e-config file, follow the [instructions to load the JSON schema into your IDE](#recommendation-enable-autocompletion-in-your-ide).

When the test execuction is started, the file `me2e-config.yml` is loaded from the `resources` folder, parsed and then the test environment is started.
To change the name of the config file to be searched for, you can annotate any class with the [`Me2eTestConfig`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e/-me2e-test-config/index.html) annotation anywhere in your project and set the `config` field to the name of your config file.

```kotlin
@Me2eTestConfig(config = "my-custom-path.yml")
class AppTest {

}
```

#### Configuring the Execution Settings
In the `settings` section of the me2e-config file, you can configure the following settings:
<details>
    <summary><code>docker</code>: Configuration of Docker and Docker-Compose</summary>
    
- `docker-compose-version`: Docker-Compose version to use (one of `V1`, `V2`)
- `pull-policy`: Policy on pulling Docker images (one of `MISSING`, `ALWAYS`)
- `build-images`: Whether to always build images before starting the containers
- `remove-images`: Whether to remove images used by services after containers shut down (one of `NONE`, `ALL`, `LOCAL`)
- `remove-volumes`: Whether to remove volumes after containers shut down
- `health-timeout`: Number of seconds to wait at most until containers are healthy
</details>

<details>
    <summary><code>requests</code>: Configuration of the HTTP requests that are sent to Microservice containers</summary>
    
- `connect-timeout`: Connect timeout in seconds
- `read-timeout`: Read timeout in seconds
- `write-timeout`: Write timeout in seconds
- `retry-on-connection-failure`: Whether to retry requests when a connectivity problem is encountered
</details>

<details>
    <summary><code>mock-servers</code>: Configuration of the Mock Servers</summary>

For more information, see [here](#mock-servers-simulating-external-services)
- `keystore-path`: Path to the keystore containing the TLS certificate to use for the Mock Server instances
- `keystore-password`: Password to use to access the keystore
- `key-manager-password`: Password used to access individual keys in the keystore
- `keystore-type`: Type of the keystore
- `truststore-path`: Path to the truststore to use for the Mock Server instances
- `truststore-password`: Password used to access the truststore
- `truststore-type`: Type of the truststore
- `needs-client-auth`: Whether TLS needs client authentication
</details>

<details>
    <summary><code>state-reset</code>: Configuration for resetting the state of containers, Mock Servers and databases after each test</summary>
    
- `clear-all-tables`: Whether to clear all entries from all tables for all database containers for which a connection to the database is established after each test
- `reset-request-interceptors`: Whether to reset all request interceptors of all Microservice containers after each test
- `reset-mock-server-requests`: Whether to reset all captured requests for all Mock Servers after each test
</details>

<details>
    <summary><code>assert-healthy</code>: Configuration whether to ensure that all containers are healthy before each test</summary>
    
For more information, see [here](#assert-healthy)
</details>

#### Defining the Test Environment for the Microservice System
The individual components of the Microservice System to be tested are defined in a regular Docker-Compose file.
Before the tests are started, this file, which is defined in the me2e-config, is parsed, the services are deserialized and the environment is started by executing `docker compose up` (or `docker-compose up` if you specified to use Docker-Compose version 1 in the me2e-config file).
Only then the execution of the first test class is started.

The configuration of the individual containers of your Microservice System is performed via labels.
These labels are read when the Docker-Compose file is parsed and set as attributes in the container instances.
The basic labels that can be used for all container types include the following:

| Label                              | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `org.jholsten.me2e.container-type` | Specifies the type of the container and thus determines to which class the container is deserialized.<br/>Possible values: `MICROSERVICE`, `DATABASE`, `MISC`.<br/>For more detailed descriptions of the different types, see [here](#container-types).                                                                                                                                                                                                                                                                                                                                                                         |
| `org.jholsten.me2e.pull-policy`    | Specifies the pull policy for the specific container. By default, the value from the me2e-config file (`settings.docker.pull-policy`) is used for all containers. With this setting, you can change the policy for a specific container and, for example, ensure that the latest image is always pulled.<br/>Possible values:<br/>- `MISSING`: Only Docker images that are not yet available on the host are pulled<br/>- `ALWAYS`: The latest image from the registry is always pulled<br/>For more information, take a look at the [Docker Documentation](https://docs.docker.com/engine/reference/commandline/compose_pull). |

##### Recommendations for the Definition of Services in the Docker-Compose File
Except for the definition of the labels, you do not need to make any changes to your existing Docker-Compose file, but you should follow the recommendations below to ensure that the test execution delivers reliable results.

For one, you should define appropriate **health checks** for each service so that all services are fully up and running before the first test execution begins.
The health checks may either be defined in the Docker-Compose file or inside the Docker image itself.
When the test environment is started, it then waits with a certain timeout until all containers have the status "healthy".
The timeout is 30 seconds by default, but can be adjusted in the me2e-config file via `settings.docker.health-timeout`.

In addition, you should also adjust the **port mapping** to allow multiple test executions to be performed simultaneously on one Docker host (as it is most probably the case when running the tests inside a CI/CD pipeline).
If you specify a fixed external port for each container port, your container will always be reachable on this port on the Docker host.
If you now start several of these test environments at the same time, conflicts will arise as the ports on the host are already occupied.
The recommendation is therefore to only define the internal container port.
This will cause Docker to randomly select an available port via which the container can be reached on the host.
In the me2e library, we retrieve this external port from the container info so that we can communicate with the containers even if the port was chosen randomly.

<table>
    <tr>
        <th>&#x2705; Do's</th>
        <th>&#x274C; Don'ts</th>
    </tr>
    <tr>
        <td>

```yaml
delivery-service:
    # ...
    ports:
      - 8082 # Docker will choose the port randomly
```

</td>
<td>
        
```yaml
delivery-service:
    # ...
    ports:
      - 8082:8082 # Fixed port on the Docker host
```

</td>
</tr>
</table>

##### Specific Configuration for Containers of type `MICROSERVICE`
By default, the base URL used to access the REST API of a Microservice with the HTTP client consists of the Docker host and the first publicly accessible port of the container, e.g. `http://localhost:1234`.
If you want to use a different URL for a Microservice container instead, you can overwrite it by using the label `org.jholsten.me2e.url` in the Docker-Compose file.
This URL is then used as the base URL for the HTTP client for the Microservice container.

##### Specific Configuration for Containers of type `DATABASE`
In order to be able to interact with the database management system within a database container via the me2e interfaces, some additional information is required, which is also set via labels in the Docker-Compose file.
- `org.jholsten.me2e.database.system`: Name of the database management system. Currently supported are `MY_SQL`, `POSTGRESQL`, `MARIA_DB` and `MONGO_DB`; the value `OTHER` is set for all other systems. For more information, see [here](#data-management-for-other-database-management-systems).
- `org.jholsten.me2e.database.name`: Name of the database to be interacted with.
- `org.jholsten.me2e.database.schema`: Name of the database schema to which the database belongs. You only need to set this label for PostgreSQL databases if the database is not part of the `public` schema. For the other SQL database management systems, the schema corresponds to the name of the database.
- `org.jholsten.me2e.database.username`: Username to be used for logging into the database. It is only necessary to set this label if interaction with the database requires authentication.
- `org.jholsten.me2e.database.password`: Password to be used for logging into the database. As with the username, it is only necessary to set this label if interaction with the database requires authentication.

For a PostgreSQL database named `orderdb` as part of the default schema `public` with username `order-service` and password `123`, the configuration in the Docker-Compose file should be the following:

```yaml
# docker-compose.yml
services:
  db:
    image: postgres:12
    # ...
    environment:
      POSTGRES_DB: "orderdb"
      POSTGRES_USER: "order-service"
      POSTGRES_PASSWORD: "123"
    labels:
      "org.jholsten.me2e.container-type": "DATABASE"
      "org.jholsten.me2e.database.system": "POSTGRESQL"
      "org.jholsten.me2e.database.name": "orderdb"
      "org.jholsten.me2e.database.username": "order-service"
      "org.jholsten.me2e.database.password": "123"
```

You do not necessarily need to set the database name, username and password in the labels if it is one of the supported database management systems.
As you usually have to pass this information to the database container as environment variables anyway, me2e reads these variables if they are not explicitly set via the labels.
For this, the following mapping of the environment variables to the database properties is used:

| Database Management System | Environment Variable corresponding to label `org.jholsten.me2e.database.name`<br/>(Database Name) | Environment Variable corresponding to label `org.jholsten.me2e.database.username`<br/>(Database Username) | Environment Variable corresponding to label `org.jholsten.me2e.database.password`<br/>(Database Password) |
|----------------------------|---------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `MY_SQL`                   | `MYSQL_DATABASE`                                                                                  | `MYSQL_USER`                                                                                              | `MYSQL_PASSWORD`                                                                                          |
| `POSTGRESQL`               | `POSTGRES_DB`                                                                                     | `POSTGRES_USER`                                                                                           | `POSTGRES_PASSWORD`                                                                                       |
| `MARIA_DB`                 | `MYSQL_DATABASE`                                                                                  | `MYSQL_USER`                                                                                              | `MYSQL_PASSWORD`                                                                                          |
| `MONGO_DB`                 | `MONGO_INITDB_DATABASE`                                                                           | `MONGO_INITDB_ROOT_USERNAME`                                                                              | `MONGO_INITDB_ROOT_PASSWORD`                                                                              |

This allows you to simplify the above example of the database container definition to:

```yaml
# docker-compose.yml
services:
  db:
    image: postgres:12
    # ...
    environment:
      POSTGRES_DB: "orderdb"
      POSTGRES_USER: "order-service"
      POSTGRES_PASSWORD: "123"
    labels:
      "org.jholsten.me2e.container-type": "DATABASE"
      "org.jholsten.me2e.database.system": "POSTGRESQL"
```

In addition to the previously described basic database properties, you can apply further configurations via the labels, which are explained in more detail in the following sections (see [here](#data-management)).
- `org.jholsten.me2e.database.reset.skip-tables`: If not explicitly deactivated in the me2e-config file via `settings.state-reset.clear-all-tables`, all tables of all databases are cleared after each test. With the label `org.jholsten.me2e.database.reset.skip-tables`, you can specify the names of the tables to be skipped when the database is cleared as a comma-separated list. This configuration is particularly useful, for example, if you use a migration tool such as [Flyway](https://flywaydb.org/) and want to retain the table containing the migration history for all tests.
- `org.jholsten.me2e.database.init-script.$name`: With labels that correspond to this pattern, you can define database scripts that are to be executed when the database is started and after a reset. Each script requires a unique name (`$name`) and with the label's value, you can specify the path to the script to be executed, which must be located in the `resources` folder of your project.
- `org.jholsten.me2e.database.connection.implementation`: If you want to interact with a database of a database management system that is not yet supported, you must provide an implementation of the abstract class [`DatabaseConnection`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database.connection/-database-connection/index.html) yourself. Use the label `org.jholsten.me2e.database.connection.implementation` to specify the full class name of your implementation so that it can be used to establish the connection when the container is started. 

For instance, a PostgreSQL database for which the table `flyway_schema_history` is to be skipped when the database is cleared and for which the initialization script named `init_db`, which is located in the `resources` folder at path `database/init_orderdb.sql`, is to be configured in the Docker-Compose as follows:

```yaml
# docker-compose.yml
services:
  db:
    image: postgres:12
    # ...
    environment:
      POSTGRES_DB: "orderdb"
      POSTGRES_USER: "order-service"
      POSTGRES_PASSWORD: "123"
    labels:
      "org.jholsten.me2e.container-type": "DATABASE"
      "org.jholsten.me2e.database.system": "POSTGRESQL"
      "org.jholsten.me2e.database.init-script.init_db": "database/init_orderdb.sql"
      "org.jholsten.me2e.database.reset.skip-tables": "flyway_schema_history"
```

#### Mock Servers: Simulating external services
If your Microservice System communicates with any third-party systems, you should simulate, i.e. mock, the behavior of these external services for the End-to-End-Tests.
Not only will this make the tests more reliable, as they no longer depend on the availability and functionality of the external systems, but it will also ensure that the actual behavior of the external system is not triggered with every test run.
For example, if an online shop system sends requests to a parcel service provider when processing an order, the parcels should of course not actually be sent when an automated test is executed.

In order to simulate these external systems as realistically as possible, me2e offers Mock Servers for their representation, which are started as part of the test environment before the tests are executed.
Any request to an external service is then no longer received by the actual service, but by the Mock Server, which returns a predefined response.
For these predefined responses, rules must be defined in advance that determine under which conditions which response is returned.
Such a combination of a request pattern and a predefined response is referred to as a "**stub**" in the me2e context.

The definition of a Mock Server, which is addressed using the HTTP protocol, is done in 3 steps:
1. Define the Mock Server with the hostname of the external service to be simulated in the me2e-config file
2. Define stubs for the requests to the external service
3. Point the DNS entry for the external service to the IP address of the Mock Server

If communication takes place via HTTP over TLS (a.k.a HTTPS), additional configurations are necessary, which are described [here](#tls-configuration).

##### Mock Server Definition
The Mock Servers are defined in the me2e-config file in the `environment.mock-servers` section.
Similar to a Docker-Compose file, you need to assign a unique key for each service to be mocked, for which you need to enter the following information:
- `hostname`: Hostname of the service to be mocked, e.g. `example.com`
- `stubs`: List of stubs for this Mock Server (see [stub definition](#stub-definition)) each as the path to the stub file in the `resources` folder

Please note that a separate Mock Server must be defined for each domain and each subdomain.

<ins>Example</ins>\
If your Microservice System communicates with two external systems that are accessible via http://example.com and http://payment.example.com, the Mock Server definition in the me2e-config could look like this:

```yaml
# me2e-config.yml
environment:
  docker-compose: docker-compose.yml
  mock-servers:
    example-service:
      hostname: example.com
      stubs:
        - stubs/example-service/example_request_stub.json
    payment-service:
      hostname: payment.example.com
      stubs:
        - stubs/payment-service/payment_request_stub.json
        - stubs/payment-service/payment_authorization_request_stub.json
```

##### Stub Definition
With one stub, you specify for a Mock Server which requests the server should respond to and how (i.e. with which response code, body and headers).
When the Mock Server is started, these stubs are registered for the specified hostname so that the server returns the response whose request pattern is closest to the actual request.
If the actual request does not match any of the registered stubs, the Mock Server returns a response with status code 404 and information on similar, possibly targeted stubs.

Each stub is defined in a separate JSON file, which must correspond to the [stub schema](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/json-schemas/stub_schema.json).
To enable autocompletion and obtain descriptions for the fields of the stub definition file, follow the [instructions to load the JSON schema into your IDE](#recommendation-enable-autocompletion-in-your-ide).

A stub definition consists of the following primary parts:
- `request`: Definition of the request to which the Mock Server should respond.
- `response`: Definition of the response that the Mock Server should return for a request that matches the specified `request`.

In addition, you can optionally define a  name for this stub under the `name` key, which you can later use to [verify the requests to the Mock Server](#mock-server-verification).
Note that the name must be unique for each Mock Server.

<ins>Example</ins>\
A stub definition for the payment request mentioned in the example above may look like this: 

```json
{
  "name": "payment_request",
  "request": {
    "method": "POST",
    "path": {
      "equals": "/payment"
    }
  },
  "response": {
    "status-code": 201,
    "body": {
      "json-content": {
        "payment_id": "8f6a1905-ebc6-424b-a2b3-37b7612b6a5b"
      }
    },
    "headers": {
      "Content-Type": [
        "application/json"
      ]
    }
  }
}
```

With this stub, the Mock Server responds to every `POST` request to the path `/payment` with status code 201, content type `application/json` and the following response body:
```json
{
  "payment_id": "8f6a1905-ebc6-424b-a2b3-37b7612b6a5b"
}
```

The components of the stubs are explained in more detail below.

###### Request
With the `request`, you specify the properties of the requests for which the stub should be applied.
You can specify the following properties:
- `method`: HTTP method of the request.
- `path`: URL path of the request as a [string matcher](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.mock.stubbing.request/-string-matcher/index.html). Matches if the actual path of the request matches the specified string matcher.
- `query-parameters`: Query parameters of the request as a map of query parameter names and string matchers for the values. Only matches if the actual query parameters conform to all specifications (i.e. each query parameter name is included and the corresponding query parameter values conform to the specified string matcher).
- `headers`: Headers of the request as a map of header names and string matcher for the values. Only matches if the actual request headers conform to all specifications (i.e. each header name is included and the corresponding header values conform to the specified string matcher).
- `body-patterns`: List of string matchers for the request body. Only matches if the actual request body matches all of the specified string matchers.

All fields are optional and for each property that is not set in the request definition, the stub matches any value.
For matching most of the properties, a [string matcher](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.mock.stubbing.request/-string-matcher/index.html) is used to compare string values.
You can specify comparisons of different types:

<table>
    <tr>
        <th>Field</th>
        <th>Description</th>
        <th>Example Definition</th>
        <th>Examples of actual Values</th>
    </tr>
    <tr>
        <td><code>equals</code></td>
        <td>Matches if the actual string is exactly the same as the specified string.</td>
        <td>
        
```json
{
  "equals": "ABC"
}
```
           
</td>
    <td>
    
&#x2705; `"ABC"`

&#x274C; `"XYZ"`
        
</td>
    </tr>
    <tr>
        <td><code>not-equals</code></td>
        <td>Matches if the actual string is not exactly the same as the specified string.</td>
        <td>
        
```json
{
  "not-equals": "ABC"
}
```
           
</td>
    <td>
    
&#x2705; `"XYZ"`

&#x274C; `"ABC"`
        
</td>
    </tr>
    <tr>
        <td><code>matches</code></td>
        <td>Matches if the actual string matches the specified regex.</td>
        <td>
        
```json
{
  "matches": "^[A-Z]{3}$"
}
```
           
</td>
    <td>
    
&#x2705; `"ABC"`

&#x274C; `"123"`
        
</td>
    </tr>
    <tr>
        <td><code>not-matches</code></td>
        <td>Matches if the actual string does not match the specified regex.</td>
        <td>
        
```json
{
  "not-matches": "^[A-Z]{3}$"
}
```
           
</td>
    <td>
    
&#x2705; `"123"`

&#x274C; `"ABC"`
        
</td>
    </tr>
    <tr>
        <td><code>contains</code></td>
        <td>Matches if the actual string contains the specified string.</td>
        <td>
        
```json
{
  "contains": "A"
}
```
           
</td>
    <td>
    
&#x2705; `"ABC"`

&#x274C; `"XYZ"`
        
</td>
    </tr>
    <tr>
        <td><code>not-contains</code></td>
        <td>Matches if the actual string does not contain the specified string.</td>
        <td>
        
```json
{
  "not-contains": "A"
}
```
           
</td>
    <td>
    
&#x2705; `"XYZ"`

&#x274C; `"ABC"`
        
</td>
    </tr>
    <tr>
        <td><code>ignore-case</code></td>
        <td>Switches off the case sensitivity for the string comparisons (only useful in combination with the other fields).</td>
        <td>
        
```json
{
  "equals": "ABC",
  "ignore-case": true
}
```
           
</td>
    <td>
    
&#x2705; `"abc"`

&#x274C; `"XYZ"`
        
</td>
    </tr>
</table>

<br/>


<ins>Examples</ins>
<table>
    <tr>
        <th>Request Definition</th>
        <th>Example of a Request matching the Stub</th>
    </tr>
    <tr>
        <td>

```json
"request": {
    "method": "POST",
    "path": {
      "equals": "/payment"
    }
}
```
</td>
        <td>

```
POST /payment HTTP/1.1
Host: payment.example.com
Content-Type: application/json
Content-Length: 19

{"amount": "12.43"}
```
</td>
    </tr>
    <tr>
        <td>

```json
"request": {
    "method": "PUT",
    "path": {
      "matches": "/account/(.*)/authorize$",
      "contains": "ABC",
      "not-contains": "123",
      "ignore-case": true
    },
    "query-parameters": {
      "client": {
        "equals": "some-client"
      }
    },
    "body-patterns": [
      {
        "contains": "user123"
      },
      {
        "contains": "12.43"
      }
    ]
}
```
</td>
        <td>

```
PUT /account/abcxyz/authorize HTTP/1.1
Host: payment.example.com
Content-Type: application/json
Content-Length: 47

{"account-id": "user123abc", "amount": "12.43"}
```
</td>
    </tr>
</table>

###### Response
With the response of a stub, you specify which response the Mock Server should return for this stub.
You can specify the following properties of the response:
- `status-code`: HTTP status code of the response
- `headers` (*optional*): HTTP headers of the response as a map of header name and list of values as strings
- `body` (*optional*): HTTP response body. You can specify the content of the body either as a string (via `string-content`), JSON array or object (via `json-content`) or as Base64 (via `base64-content`).

<ins>Examples</ins>
<table>
    <tr>
        <th>Response Definition</th>
        <th>Actual HTTP Response</th>
    </tr>
    <tr>
        <td>

```json
"response": {
  "status-code": 201,
  "body": {
    "string-content": "Created"
  },
  "headers": {
    "Content-Type": [
      "text/plain"
    ]
  }
}
```
</td>
        <td>

```
HTTP/1.1 201 Created
Content-Type: text/plain
Content-Length: 9

Created
```
</td>
    </tr>
    <tr>
        <td>

```json
"response": {
  "status-code": 200,
  "body": {
    "json-content": {
      "authorized": true,
      "transaction_id": "8f6a1905-ebc6-424b-a2b3-37b7612b6a5b"
    }
  },
  "headers": {
    "Content-Type": [
      "application/json"
    ]
  }
}
```
</td>
        <td>

```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 78

{"authorized": true, "transaction_id": "8f6a1905-ebc6-424b-a2b3-37b7612b6a5b"}
```
</td>
    </tr>
</table>

##### DNS Configuration
Technically, the simulation of the external services is implemented in such a way that one HTTP server is started on the host which is running the tests and the stubbed requests are assigned to the correct Mock Server instance via the `Host` header.
This HTTP server can be reached on the standard HTTP port 80 and the standard HTTPS port 443.
In theory, you could therefore replace the URL of the external system with the IP address of the test runner for all services of your Microservice System that communicate with external systems.
However, as this may involve a modification of the code base and may require a great deal of effort, the recommended approach is to have the DNS entry for the hostname of the external system point to the IP of the test runner instead.
To do this, add an [extra_hosts](https://docs.docker.com/compose/compose-file/compose-file-v3/#extra_hosts) entry for the domain to be simulated to the Docker-Compose file for each Microservice that communicates with the third-party service.
If the host on which the tests are executed is the same as the host on which Docker is running, you can use Dockers predefined `host-gateway` entry for the IP address of the test runner.
However, if the tests are executed within a CI/CD pipeline with [Docker-in-Docker](https://docs.gitlab.com/ee/ci/docker/using_docker_build.html#use-docker-in-docker), the IP addresses of the Docker host and the test runner host differ.
Therefore, you should use the value of the environment variable `RUNNER_IP` for the IP address of the test runner and use the `host-gateway` as the default value if this variable is not set.
This allows you to run the tests both locally and inside the CI/CD pipeline.
For more information on how to set this environment variable for the CI/CD pipeline, take a look at the [section on how to run tests inside a GitLab pipeline](#running-the-end-to-end-tests-inside-gitlab-ci). 

<ins>Example</ins>\
To forward requests to `example.com` and `payment.example.com` to the corresponding Mock Server instances, set:

```yaml
# docker-compose.yml
services:
  order-service:
    # ...
    extra_hosts:
      - "example.com:${RUNNER_IP:-host-gateway}"
      - "payment.example.com:${RUNNER_IP:-host-gateway}"
```

In case you are receiving 403 errors and your operating system is Windows, you may need to [disable the automatic proxy detection](https://support.rezstream.com/hc/en-us/articles/360025156853-Disable-Auto-Proxy-Settings-in-Windows-10) (see [GitHub Issue #13127](https://github.com/docker/for-win/issues/13127)).

##### TLS Configuration
###### Server Authentication
If you want to enable the Microservices to communicate with a Mock Server via HTTP over TLS, a common TLS certificate must be issued for all Mock Server instances.
To do this, a keystore must first be created, for which the domains of the external services to be simulated must be specified as the *Subject Alternative Names* (SAN).
To create such a keystore with the Java Keytool, execute the following command for a certificate for the domains `example.com` and `payment.example.com` to be mocked.

```shell
keytool -genkey -keyalg RSA -keysize 2048 -alias mock_server -validity 3650 -keypass mock_server -keystore mock_server_keystore.jks -storepass mock_server -ext SAN=dns:example.com,dns:payment.example.com
```

Now you can export the TLS certificate in PEM format with the following commands.

```shell
keytool -exportcert -alias mock_server -keystore mock_server_keystore.jks -storepass mock_server -file mock_server_certificate.crt
openssl x509 -inform der -in mock_server_certificate.crt -out mock_server_certificate.pem
```

Now place the keystore file `mock_server_keystore.jks` in the `resources` folder of your test project.
In order for the Mock Servers to use the certificate, you need to configure the following settings in the `settings` section of the me2e-config file:

```yaml
# me2e-config.yml
settings:
  mock-servers:
    keystore-path: mock_server_keystore.jks
    keystore-password: mock_server
    key-manager-password: mock_server
```

If you have used a keystore tool other than the Java Keytool, you must also specify the `keystore-type` in these settings.

With these settings, the Mock Servers will now use the previously created certificate.
However, as this certificate is self-signed, you also need to ensure that the Microservices trust this certificate.

###### Client Authentication
If you also want to enable the Microservices to authenticate against the Mock Server, you must first generate a truststore.
You can also use any tool for this, the type of which you must then specify in the me2e-config file under `truststore-type`.
The path to the truststore and the truststore password also need to be specified here.

```yaml
# me2e-config.yml
settings:
  mock-servers:
    truststore-path: mock_server_truststore.jks
    truststore-password: mock_server
```

#### Data Management
me2e offers the possibility to manage the data of a database container by
- setting an initial state using the execution of a script and
- resetting the state after each test.

The prerequisite for this is that the database management system of the container is one of the supported ones (i.e. `MY_SQL`, `POSTGRESQL`, `MARIA_DB` or `MONGO_DB`) and that me2e can establish a connection to the database.
This requires the name of the database as well as the username and password to be set.
So first take a look [here](#specific-configuration-for-containers-of-type-database) at how to configure the database container correctly so that this data is available.

If the database management system is not one of those supported by default, you can also perform these function, but you must first apply certain configurations for this database connection.
For instructions on how to do this, see [here](#data-management-for-other-database-management-systems).

##### Setting an initial State
The initial state of a database can be set by executing a script.
For SQL databases, an SQL script is required; for MongoDB, a JavaScript script is needed.
Place these scripts in the `resources` folder of your test project and add a label `org.jholsten.me2e.database.init-script.$name` to the corresponding database container in the Docker-Compose file for each script, where `$name` is an arbitrary but unique name for your script and the value of the label contains the path to the script in the `resources` folder.
The script's `$name` does not play a decisive role, but allows you to uniquely identify scripts by name and execute them yourself as desired.

<ins>Example</ins>\
Probably the most useful purpose for the initialization scripts is to create accounts before the tests are executed.
The following PostgreSQL script, which is located in the `resources` folder under `scripts/accounts.sql`, could serve as an example for the creation of these accounts.

<details open>
    <summary><ins><b>PostgreSQL</b> Script (<code>scripts/accounts.sql</code>)</ins></summary>
    
```sql
-- scripts/accounts.sql
INSERT INTO account(id, username, password, role)
VALUES ('00000000-1111-1111-1111-000000000000', 'user', '2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b', 'USER'),
       ('00000000-1111-1111-1111-000000000001', 'admin', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'ADMIN');
```
</details>

<details>
    <summary><ins><b>MongoDB</b> Script (<code>scripts/accounts.js</code>)</ins></summary>
    
```javascript
// scripts/accounts.js
conn = new Mongo("mongodb://dbuser:password@localhost:27017"); // For a database without authentication, use `conn = new Mongo();`
db = conn.getDB("orderdb");

db.account.insertMany([
    {
        id: '00000000-1111-1111-1111-000000000000',
        username: 'user',
        password: '2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b',
        role: 'USER'
    },
    {
        id: '00000000-1111-1111-1111-000000000001',
        username: 'admin',
        password: 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',
        role: 'ADMIN'
    }
]);
```
</details>

We now assign the name `create_accounts` to this script and add the following label to the database container in the Docker-Compose:

<details open>
    <summary><ins>Configuration for <b>PostgreSQL</b> Script <code>scripts/accounts.sql</code></ins></summary>

```yaml
  db:
    # ...
    labels:
      # ...
      "org.jholsten.me2e.database.init-script.create_accounts": "scripts/accounts.sql"
```
</details>

<details>
    <summary><ins>Configuration for <b>MongoDB</b> Script <code>scripts/accounts.js</code></ins></summary>

```yaml
  db:
    # ...
    labels:
      # ...
      "org.jholsten.me2e.database.init-script.create_accounts": "scripts/accounts.js"
```
</details>

This script is now executed as soon as the container is started and all containers in the environment have the status "healthy".
It is particularly important to define a healthcheck for the database container here, as the initialization scripts can only be executed once the database is fully up and running.

If several scripts are defined for one database container, these are executed in the order in which they are specified in the labels.

##### Resetting the State
By default, the state of all databases is reset after each test (see [here](#state-reset)), so that each test starts with the same state and the order in which the tests are executed has no influence on the results.
When resetting the state, all tables of the database are cleared first and then the initialization scripts are executed again.
To exclude certain tables that should not be cleared, you can use the label `org.jholsten.me2e.database.reset.skip-tables`.
To do this, enter a comma-separated list of the table names that should not be cleared under this label.

<ins>Example</ins>\
To prevent the tables `table1` and `table2` from being cleared when the database state is reset, enter the following label to the database container in the Docker-Compose.

```yaml
# docker-compose.yml
services:
  db:
    # ...
    labels:
      "org.jholsten.me2e.database.reset.skip-tables": "table1,table2"
``` 

##### Data Management for other Database Management Systems
The interaction with the databases takes place via the abstract class [`DatabaseConnection`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database.connection/-database-connection/index.html), for which me2e provides 2 default implementations:
- [`SQLDatabaseConnection`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database.connection/-s-q-l-database-connection/index.html): for interacting with PostgreSQL, MySQL and MariaDB databases
- [`MongoDBConnection`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database.connection/-mongo-d-b-connection/index.html): for interacting with MongoDB databases

In order to interact with a currently not supported database management system via the me2e interfaces, you need to implement the [`DatabaseConnection`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database.connection/-database-connection/index.html) yourself.
To do this, create a class that inherits from `DatabaseConnection` and that contains a class `Builder` that inherits from [`DatabaseConnection.Builder`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database.connection/-database-connection/-builder/index.html).

```kotlin
package com.example

import org.jholsten.me2e.container.database.connection.DatabaseConnection

class CustomDatabaseConnection(host: String, port: Int, database: String, username: String?, password: String?, container: DatabaseContainer?) : DatabaseConnection(
    host = host,
    port = port,
    database = database,
    username = username,
    password = password,
    container = container,
) {
    // ...

    class Builder : DatabaseConnection.Builder<Builder>() {
        // ...
    }
}
```

Now add the following label with the full class name of your custom database connection class to the database container that should use this implementation:

```yaml
# docker-compose.yml
services:
  db:
    # ...
    labels:
      "org.jholsten.me2e.database.connection.implementation": "com.example.CustomDatabaseConnection"
```

With this setting, as soon as all containers are healthy, me2e will no longer attempt to use one of the default implementations `SQLDatabaseConnection` and `MongoDBConnecion`, but instead will set a new instance of your custom class in the `connection` attribute of the corresponding [`DatabaseContainer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database/-database-container/index.html) instance using the builder of your custom class.
Please note that only the attributes defined in the `DatabaseConnection.Builder` (i.e. `host`, `port`, `database`, `username`, `password`, `container`) are available in your builder class. 

### Defining Tests
Now that the configuration options for the me2e library have been explained in more detail, we will take a close look at the options for defining End-to-End-Tests.
The prerequisite for using the functions described below is that the test class in which you defined the tests either directly or indirectly inherits from [`Me2eTest`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e/-me2e-test/index.html).

#### Injecting services
With the [`@InjectService`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.injection/-inject-service/index.html) annotation, you can inject individual container or Mock Server instances from your test environment into attributes of your test class using the instance's names.
- For the Docker container instances, this name corresponds to the key of the container in the `services` section of the Docker-Compose file.
- For the Mock Server instances, this name corresponds to the key of the Mock Server in the `mock-servers` section of the me2e-config file. 

The name of the instance to be injected can either be specified via the attribute name or you can specify the name directly in the `@InjectService` annotation via the `name` parameter.
If the `name` is not set in the annotation, the attribute name is converted to Kebab-Case and this converted value is used as the service name.
Depending on the data type of the attribute, an attempt is then made to find either a container (for attributes of type [`Container`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container/-container/index.html)) or a Mock Server instance (for attributes of type [`MockServer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.mock/-mock-server/index.html)) with exactly this name and the corresponding instance is set as the value of the annotated attribute.
If no such instance can be found in the test environment or the datatype of the field does not match the datatype of the instance, an exception is thrown and each test of the test class fails.

<ins>Examples</ins>
<table>
    <tr>
        <th>Using the attribute name to specify the name of the service to inject</th>
        <th>Using the <code>@InjectService</code>'s name parameter to specify the name of the service to inject</th>
    </tr>
    <tr>
<td>

```kotlin
@InjectService
private lateinit var backendApi: MicroserviceContainer
```
</td>
<td>

```kotlin
@InjectService("backend-api")
private lateinit var someContainer: MicroserviceContainer
```
</td>
    </tr>
    <tr>
<td>

```kotlin
@InjectService
private lateinit var paymentService: MockServer
```
</td>
<td>

```kotlin
@InjectService("payment-service")
private lateinit var someMockServer: MockServer
```
</td>
    </tr>
</table>

#### Interacting with Containers
In your tests, you can interact with the container instances defined in the Docker-Compose via its [interfaces](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container/-container/index.html), for example to execute certain commands or to copy files to or from the container.
You can also use the attributes and functions to retrieve certain properties of the container, such as the Docker container ID or the container's logs.
If you want to monitor the status of a container, you can attach consumers to the container to be notified when the status changes.
To be specific, the following properties can be monitored with consumers:
- Logs ([`addLogConsumer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container/-container/add-log-consumer.html)): notifies you of every new log message that the container outputs
- Resource Usage Statistics ([`addStatsConsumer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container/-container/add-stats-consumer.html)): notifies you once per second about the resource consumption of the container
- Events ([`addEventConsumer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container/-container/add-event-consumer.html)): notifies you about every [Docker Event](https://docs.docker.com/engine/reference/commandline/system_events/#containers) for the container

<ins>Examples</ins>
<table>
    <tr>
        <th>Checking whether a Container logged a certain message</th>
<td>
    
```kotlin
@Test
fun `Container should log certain message`() {
    val logs = container.getLogs()
    assertTrue(logs.any { it.message == "Received notification." }, "Container did not log expected message.")
}
```
</td>
    </tr>
    <tr>
        <th>Checking whether a file in the Container contains certain content</th>
<td>
    
```kotlin
@Test
fun `File in container should have certain content`() {
    val file = container.copyFileFromContainer("/app/file.txt", "/home/container_file.txt")
    assertEquals("Expected Content", file.readText())
}
```
</td>
    </tr>
    <tr>
        <th>Checking whether an environment variable is set to a certain value</th>
<td>
    
```kotlin
@Test
fun `Environment variable should have certain value`() {
    val result = backendApi.execute("echo", "\$MY_ENV")
    assertEquals(0, result.exitCode)
    assertEquals("ExpectedValue", result.stdout)
}
```
</td>
    </tr>
</table>

#### Interacting with Databases
The prerequisite for interacting with databases via the me2e interfaces is that a connection to the database has been established, i.e. all the required properties are set in the Docker-Compose (see [here](#specific-configuration-for-containers-of-type-database) and [here](#data-management)) and the database management system is either one of those that is supported by default or you have provided a custom implementation of the [`DatabaseConnection`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database.connection/-database-connection/index.html) yourself (see [here](#data-management-for-other-database-management-systems)).
If these requirements are met, you can interact with the database via the functions of the [`DatabaseContainer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.database/-database-container/index.html) and, for example, read the entries from the database or execute certain scripts.

<ins>Examples</ins>
<table>
    <tr>
        <th>Checking whether certain tables are available in the database</th>
<td>
    
```kotlin
@Test
fun `Certain tables should be available in the database`() {
    val tables = database.tables
    assertEquals(listOf("table1", "table2"), tables)
}
```
</td>
    </tr>
    <tr>
        <th>Checking whether a certain table contains certain entries</th>
<td>
    
```kotlin
@Test
fun `Certain entries should be available in the database`() {
    val result = database.getAllFromTable("table1")
    assertEquals(listOf("column1", "column2"), result.columns)
    assertEquals(listOf("value1", "value2", "value3"), result.getEntriesInColumn("column1"))
}
```
</td>
    </tr>
</table>

#### Executing HTTP Requests and Verifying their Responses
With containers of type `MICROSERVICE`, which are represented by instances of the [`MicroserviceContainer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/index.html) class, you can interact via their HTTP REST API.
You can execute [`GET`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/get.html), [`POST`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/post.html), [`PUT`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/put.html), [`PATCH`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/patch.html) and [`DELETE`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/delete.html) requests using the integrated HTTP client, which is initialized with the container's base URL after it is started.
For all of these functions, you need to pass the URL of the endpoint to be addressed, relative to the base URL of the microservice, as the first parameter.
To specify this [relative URL](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.model/-relative-url/index.html), you can use either its constructor or its builder class.

<ins>Examples</ins>\
Assuming that the microservice has the following base URL: `http://localhost:1234`, the following relative URLs are resolved as follows:

<table>
    <tr>
        <th>Relative URL using Constructor</th>
        <th>Relative URL using Builder</th>
        <th>Complete URL</th>
    </tr>
    <tr>
<td>

```kotlin
RelativeUrl("/account?id=123")
```
</td>
<td>

```kotlin
RelativeUrl.Builder()
    .withPath("/account")
    .withQueryParameter("id", "123")
    .build()
```
</td>
    <td><code>http://localhost:1234/account?id=123</code></td>
    </tr>
    <tr>
<td>

```kotlin
RelativeUrl("/search?q=abc&q=xyz#p=42")
```
</td>
<td>

```kotlin
RelativeUrl.Builder()
    .withPath("/search")
    .withQueryParameter("q", "abc")
    .withQueryParameter("q", "xyz")
    .withFragment("p=42")
    .build()
```
</td>
    <td><code>http://localhost:1234/search?q=abc&q=xyz#p=42</code></td>
    </tr>
</table>

<ins>Request Examples</ins>
<table>
    <tr>
        <th>Executing a <code>GET</code> request</th>
<td>
    
```kotlin
val response = microservice.get(RelativeUrl("/account?id=123"))
```
</td>
    </tr>
    <tr>
        <th>Executing a <code>POST</code> request with body</th>
<td>
    
```kotlin
val response = microservice.post(
    RelativeUrl("/account"),
    HttpRequestBody.Builder()
        .withJsonContent(CreateAccountDto(username = "user"))
        .build()
)
```
</td>
    </tr>
</table>

##### Verifying HTTP Responses
To verify that the response of a Microservice meets your expectations, you can use the [`assertThat`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions/assert-that.html) method in combination with the functions from the [`assertions` package](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions/index.html).
This allows you to ensure that all possible properties of the response have the expected value.

To specify assertions response bodies, you can use the following functions to compare parts or the entire content of the body:
- [`body`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/body.html): Specify expectations on the content of the body, encoded as string
  - <ins>Example</ins>: Assuming the response body is `Some Response Text`, we can execute the following assertions, for example:
    - Assert that the content of the body corresponds exactly to the string `Some Response Text`:

        ```kotlin
        assertThat(response).body(equalTo("Some Response Text"))
        ```

    - Assert that the content of the body includes the string `Some`:

        ```kotlin
        assertThat(response).body(containsString("Some"))
        ```

    - Assert that the content of the body matches the regular expression `.*Response Text`:

        ```kotlin
        assertThat(response).body(matchesPattern(".*Response Text"))
        ```

- [`objectBody`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/object-body.html): Specify expectations on the content of the body, deserialized to an instance of a given class
  - <ins>Example</ins>: Assuming the response body is `{"first": "value", "second": 2}`, we can execute the following assertion for example:

     ```kotlin
     val expectedBody = Pair("value", 2)
     assertThat(response).objectBody(equalTo(expectedBody))
     ```

- [`binaryBody`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/binary-body.html): Specify expectations on the raw binary content of the body
  - <ins>Example</ins>: Assuming the response body is a binary value of `[97, 98, 99]`, we can execute the following assertion for example:

     ```kotlin
     assertThat(response).binaryBody(equalTo(byteArrayOf(97, 98, 99)))
     ```

- [`base64Body`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/base64-body.html): Specify expectations on the raw binary content of the body, encoded as Base 64
  - <ins>Example</ins>: Assuming the response body is a binary value of `[97, 98, 99]`, which is encoded as Base64 to `YWJj`, we can execute the following assertion for example:

     ```kotlin
     assertThat(response).base64Body(equalTo("YWJj"))
     ```

- [`jsonBody`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/json-body.html): Specify expectations on the content of the body, parsed to a JSON object
  - To specify assertions for a JSON body, you can pass the entire expected JSON object as a `JsonNode` and compare it with [`equalTo`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions/equal-to.html).
    - <ins>Example</ins>: Assuming the response body is `{"first": "value", "second": 2}`, we can execute the following assertion, for example:

        ```kotlin
        val expectedBody = JsonNodeFactory.instance.objectNode()
            .put("first", "value")
            .put("second", 2)
        assertThat(response).jsonBody(equalTo(expectedBody))
        ```

    - In case you want to ignore certain nodes when comparing the JSON objects, use [`ignoringNodes`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions.matchers/-json-body-equality-assertion/ignoring-nodes.html).
      - <ins>Example</ins>: Assert that the JSON response body is equal to the expected object, while ignoring field `_id`:

        ```kotlin
        assertThat(response).jsonBody(equalTo(expectedBody).ignoringNodes("$._id"))
        ```

  - Alternatively, you can use the [`containsNode`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions/contains-node.html) function to specify assertions for individual nodes of the body. Specify the path to this node whose value you want to compare using a [JSONPath](https://datatracker.ietf.org/doc/draft-ietf-jsonpath-base/) expression. Such a JSONPath expressions begins with the root element `$`, after which you can specify the path to the corresponding node using `.` as the path separator. For detailed information on the JSONPath syntax and examples, take a look at this [IETF draft](https://datatracker.ietf.org/doc/draft-ietf-jsonpath-base/).
    - <ins>Example</ins>: Assuming the response body is `{"first": "value", "second": 2}`, we can execute the following assertion, for example:

        ```kotlin
        assertThat(response).jsonBody(containsNode("$.first").withValue("value"))
        assertThat(response).jsonBody(containsNode("$.second").withValue("2"))
        ```

Particularly with larger response bodies, the test cases can quickly become hard to read and difficult to maintain if the expectations are specified directly in the code.
It is therefore recommended to save the expected response bodies in files in the `resources` folder and to use them in combination with the [`equalToContentsFromFile`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions/equal-to-contents-from-file.html) function.
With this method, specify the path of the file containing the expected response body and use [`asString`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions.matchers/-file-contents-equality-assertion/as-string.html), [`asBinary`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions.matchers/-file-contents-equality-assertion/as-binary.html), [`asJson`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions.matchers/-file-contents-equality-assertion/as-json.html) or [`asObject`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions.matchers/-file-contents-equality-assertion/as-object.html) to specify how the content of this file should be read.
This function can be used in combination with all the functions presented above.
- <ins>Example using [`body`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/body.html)</ins>:

     ```kotlin
     assertThat(response).body(equalToContentsFromFile("expected.txt").asString())
     ```
  
- <ins>Example using [`objectBody`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/object-body.html)</ins>:

     ```kotlin
     assertThat(response).objectBody(equalToContentsFromFile("expected.json").asObject<Pair<String, Int>>())
     ```
  
- <ins>Example using [`binaryBody`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/binary-body.html)</ins>:

     ```kotlin
     assertThat(response).binaryBody(equalToContentsFromFile("expected_binary").asBinary())
     ```
  
- <ins>Example using [`base64Body`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/base64-body.html)</ins>:

     ```kotlin
     assertThat(response).base64Body(equalToContentsFromFile("expected_binary").asBase64())
     ```
  
- <ins>Example using [`jsonBody`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/json-body.html)</ins>:

     ```kotlin
     assertThat(response).jsonBody(equalToContentsFromFile("expected.json").asJson().ignoringNodes("$._id"))
     ```

<ins>Examples</ins>
<table>
    <tr>
        <th>Expected Response</th>
        <th>Assertions</th>
    </tr>
    <tr>
<td>

```
HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: 18

Some Response Text
```
</td>
<td>

```kotlin
assertThat(response)
    .statusCode(equalTo(200))
    .message(equalTo("OK"))
    .protocol(equalTo("HTTP/1.1"))
    .contentType(equalTo("text/plain"))
    .body(equalTo("Some Response Text"))
```
</td>
    </tr>
    <tr>
<td>

```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 31

{"id": 123, "name": "John Doe"}
```
</td>
<td>

```kotlin
assertThat(response)
    .statusCode(equalTo(200))
    .contentType(equalTo("application/json"))
    .jsonBody(containsNode("$.id").withValue(equalTo("123")))
    .jsonBody(containsNode("$.name").withValue(equalTo("John Doe")))
```
</td>
    </tr>
    <tr>
<td>

```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 470
Server: Apache/2.0

{
     "title": "Developing, Verifying, and Maintaining High-Quality Automated Test Scripts",
     "authors": [
         {
             "firstname": "Vahid",
             "lastname": "Garousi"
         },
         {
             "firstname": "Michael",
             "lastname": "Felderer"
         }
     ],
     "year": 2016,
     "keywords": ["Software Testing", "Test Automation"],
     "journal": {
         "title": "IEEE Software",
         "volume": 33,
         "issue": 3
     }
}
```
</td>
<td>

```kotlin
assertThat(response)
    .statusCode(between(200, 299))
    .protocol(containsString("HTTP"))
    .headers(containsKey("Server").withValue(equalTo("Apache/2.0")))
    .jsonBody(containsNode("$.title").withValue(containsString("Automated Test Scripts")))
    .jsonBody(containsNode("$.authors[0].lastname").withValue(equalTo("Garousi")))
    .jsonBody(containsNode("$.authors[0]").withValue(equalTo("{\"firstname\":\"Vahid\",\"lastname\":\"Garousi\"}")))
    .jsonBody(containsNode("$.year").withValue(equalTo("2016")))
    .jsonBody(containsNode("$.keywords[1]").withValue(equalTo("Test Automation")))
```
</td>
    </tr>
    <tr>
<td>

```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 32

{"first": "value1", "second": 2}
```
</td>
<td>

```kotlin
val expectedResponse = Pair("value1", 2)

assertThat(response)
    .statusCode(equalTo(200))
    .objectBody(equalTo(expectedResponse))
```
</td>
    </tr>
</table>

###### Defining Expected Responses
With the functions presented above, the test is terminated as soon as the first property of the response does not meet the expectations.
If you want to test several properties at the same time instead and receive a collective evaluation at the end after all properties have been tested, you can use the [`ExpectedResponse`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-expected-response/index.html).
As with the previous functions, you can use instances of this class to define all the expected properties of the response and then pass this object to the [`conformsTo`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.request.assertions/-assertable-response/conforms-to.html) function.

<ins>Example</ins>
<table>
    <tr>
        <th>Expected Response</th>
        <th>Assertions</th>
    </tr>
    <tr>
<td>

```
HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: 18

Some Response Text
```
</td>
<td>

```kotlin
val expectedResponse = ExpectedResponse()
    .expectStatusCode(equalTo(200))
    .expectMessage(equalTo("OK"))
    .expectProtocol(equalTo("HTTP/1.1"))
    .expectContentType(equalTo("text/plain"))
    .expectBody(equalTo("Some Response Text"))

assertThat(response).conformsTo(expectedResponse)
```
</td>
    </tr>
</table>

##### Authentication
If you want to send an authenticated request to a Microservice, for example with a Bearer token in the Authorization header, you can use the [`authenticate`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/authenticate.html) method of the [`MicroserviceContainer`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice/-microservice-container/index.html) class.
You need to pass an instance of the [`Authenticator`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice.authentication/-authenticator/index.html) class to this method, which uses the [`getRequestInterceptor`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice.authentication/-authenticator/get-request-interceptor.html) function to provide a request interceptor that performs the required authentication and adapts the request accordingly.
Note that this authenticator is used for all subsequent requests to this Microservice in the current test.
Unless set otherwise in the me2e-config file in `settings.state-reset.reset-request-interceptors` (see [here](#state-reset)), this request interceptor is only valid for the execution of a single test and the interceptors are reset afterwards.

By default, me2e only provides [`UsernamePasswordAuthentication`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice.authentication/-username-password-authentication/index.html) for basic authentication and [`ApiKeyAuthentication`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice.authentication/-api-key-authentication/index.html) for authentication using an API key.
If you want to use other authentication methods, you need to provide a corresponding implementation yourself through a class that inherits from the [`Authenticator`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.container.microservice.authentication/-authenticator/index.html).

<ins>Example Usage</ins>
```kotlin
@Test
fun `Executing authenticated GET request should succeed`() {
    microservice.authenticate(ApiKeyAuthentication("secret-api-key"))

    val response = microservice.get(RelativeUrl("/secured"))

    assertThat(response).statusCode(equalTo(200))
}
```

#### Mock Server Verification
If you have set up a [Mock Server](#mock-servers-simulating-external-services) in your test environment to simulate an external system, you can also use the me2e functions to check whether an expected request has been set to a certain Mock Server.
As [with the functions for verifying HTTP responses presented before](#verifying-http-responses), use the [`assertThat`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions/assert-that.html) method in combination with the functions from the [`assertions` package](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.assertions/index.html).
Unlike before, however, in this context it only makes sense to compare all the properties of the requests that the Mock Server has received with the expectations.
To do this, instantiate an object of the [`ExpectedRequest`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.mock.verification/-expected-request/index.html) class and set all the properties of the expected request.
You can then use the [`receivedRequest`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.mock.verification/-mock-server-verification/received-request.html) method to verify that the Mock Server has received this expected request either any number of times or a certain number of times. 

As with the verification of HTTP responses, similar functions for comparing the properties of the requests are available here as well, such as [`withBody`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.mock.verification/-expected-request/with-body.html) or [`withJsonBody`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.mock.verification/-expected-request/with-json-body.html).
Alternatively, if you have assigned unique names to your stubs (see the [section on the stub definition](#stub-definition)), you can verify that a Mock Server has received a request that matches the request pattern defined in the stub with the given name using the function [`matchingStub`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.mock.verification/-expected-request/matching-stub.html).

<ins>Examples</ins>
<table>
    <tr>
        <th>Verify that the Mock Server has received the expected request exactly once</th>
<td>

```kotlin
assertThat(exampleServer).receivedRequest(
    1,
    ExpectedRequest()
        .withMethod(equalTo(HttpMethod.GET))
        .withPath(equalTo("/account"))
        .withQueryParameters(containsKey("id").withValue(equalTo("123")))
)
```
</td>
    </tr>
    <tr>
        <th>Verify that the Mock Server has only received the expected request and no other</th>
<td>

```kotlin
assertThat(exampleServer).receivedRequest(
    ExpectedRequest()
        .withMethod(equalTo(HttpMethod.GET))
        .withPath(equalTo("/account"))
        .withQueryParameters(containsKey("id").withValue(equalTo("123")))
        .andNoOther()
)
```
</td>
    </tr>
    <tr>
        <th>Verify that the Mock Server has received the expected request at least once</th>
<td>

```kotlin
assertThat(exampleServer).receivedRequest(
    ExpectedRequest()
        .withMethod(equalTo(HttpMethod.GET))
        .withPath(equalTo("/account"))
        .withQueryParameters(containsKey("id").withValue(equalTo("123")))
)
```
</td>
    </tr>
    <tr>
        <th>Verify that the Mock Server has received the expected request with the expected request body in file <code>requests/example_request.json</code></th>
<td>

```kotlin
assertThat(exampleServer).receivedRequest(
    ExpectedRequest()
        .withMethod(equalTo(HttpMethod.POST))
        .withPath(equalTo("/account"))
        .withJsonBody(equalToContentsFromFile("requests/example_request.json").ignoringNodes("$._id"))
)
```
</td>
    </tr>
    <tr>
        <th>Verify that the Mock Server has received a request which matches the request pattern of the stub with name <code>example_stub</code></th>
<td>

```kotlin
assertThat(exampleServer).receivedRequest(ExpectedRequest().matchingStub("example_stub"))
```
</td>
    </tr>
</table>

### Measures against Flaky Tests
As already described in the introduction, flaky tests - i.e. tests that sometimes produce positive and sometimes negative results without changing the code - are major problem, especially in End-to-End-Tests for Microservice Systems.
To prevent this problem, me2e offers a number of functions that can be used to reduce the risk and impact of flaky tests:
- **Assert Healthy**: Before each test, it is ensured that all containers in the test environment are healthy
- **Request-Retry**: Requests that fail due to connectivity problems are repeated
- **State Reset**: The state of all services is reset after each test so that the order of the test execution does not affect their results

All these functions are activated by default, but you can deactivate them in the `settings` section of the me2e-config file.

| Measure        | Key in the me2e-config File                                                                                                                                    |
|:---------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Assert Healthy | - `settings.assert-healthy`                                                                                                                                    |
| Request-Retry  | - `settings.requests.retry-on-connection-failure`                                                                                                              |
| State-Reset    | - `settings.state-reset.clear-all-tables`<br/>- `settings.state-reset.reset-request-interceptors`<br/>- `settings.state-reset.reset-mock-server-requests`<br/> |

#### Assert Healthy
Before each test, it is checked whether all Docker containers for which a healthchek is defined are currently healthy.
If one of the containers is not healthy, it is assumed that this would lead to a flaky and therefore meaningless test result.
Therefore, an attempt is made to restart the container once.
If this attempt fails or if the container is still unhealthy, the current test is aborted.

#### Request-Retry
If the `retry-on-connection-failure` flag is activated, HTTP requests that fail due to a connectivity problem are silently retried by the HTTP client.

#### State Reset
To ensure that the state changes triggered by the execution of a test do not affect the results of subsequent tests, the state of all containers, Mock Servers and databases is reset by default after each test.
This includes the following states:
- for all database containers, all tables are cleared except for those specified in the `org.jholsten.me2e.database.reset.skip-tables` label
- all request interceptors are removed for all Microservice containers
- the list of all previously received requests is reset for all Mock Server instances

### Test Report
After all tests have been executed, a test report is generated that contains detailed information about the executed tests and the state of the test environment.
By default, the [`HtmlReportGenerator`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.report.generator.html/-html-report-generator/index.html) is used to generate this report, but you can also provide a custom implementation to generate the report or customize the existing HTML report.
The following sections explain how you can realize this customizability and which contents are available for the report.

#### Contents
During the execution of the tests, a report data aggregator collects various data on the tests and the test environment.
This includes:
- standard metrics on the tests, i.e. execution times, status and cause of errors, among others

![Test Report Metrics Example](docs/test_report_example_detailed_metrics.png)

- logs of the test runner and all Docker containers of the test environment

![Test Report Logs Example](docs/test_report_example_detailed_logs.png)

- resource consumption of all Docker container over time

![Test Report Resource Usage Example](docs/test_report_example_detailed_resource_usage.png)

- HTTP network traces for all HTTP requests sent in the Docker bridge networks of the Docker-Compose including status, headers and payloads, among others

![Test Report HTTP Traces Example](docs/test_report_example_detailed_traces.png)

After the execution of all tests has been completed, this data is aggregated and transferred to the report generator in the form of the [`TestExecutionResult`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.report.result.model/-test-execution-result/index.html).

#### Customizing the Report
To customize the test report to your needs, you have various options:
- Customization of the [`HtmlReportGenerator`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.report.generator.html/-html-report-generator/index.html) used by default
- Implementation of your own report generator

For both customization options, you must register your custom implementation as a report generator in me2e so that it is used to generate reports after the test execution.
To do this, place the [`Me2eTestConfig`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e/-me2e-test-config/index.html) annotation anywhere in your project on any class and set the `reportGenerators` parameter to include your custom implementation.
For example, to use the default HTML report and your custom implementation, add the following annotation:

```kotlin
@Me2eTestConfig(
    reportGenerators = [HtmlReportGenerator::class, CustomReportGenerator::class]
)
class AppTest {

}
```

For all report generators defined here, the [`generate`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.report.generator/-report-generator/generate.html) method is called after the test execution, passing the aggregated test execution result as a parameter.

##### Customization of the `HtmlReportGenerator`
The [`HtmlReportGenerator`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.report.generator.html/-html-report-generator/index.html) uses [Thymeleaf](https://www.thymeleaf.org/) as a template engine to fill HTML templates with data.
The generator uses 2 different templates:
- Landing page of the test results: `report/templates/index.html` (*attribute `indexTemplate`*)
- Test details per class: `report/templates/test-detail.html` (*attribute `testDetailTemplate`*)

The `additionalResources` attribute is used to define additional resources that are copied to the output directory, and the `outputDirectory` attribute is used to specify the directory in which the generated HTML files are to be stored.
This class has been especially implemented so that you have as many options as possible to customize individual parts of its functionality.
By implementing your own class that inherits from the `HtmlReportGenerator`, you can customize the following properties, for example:
- Changing the templates: instead of using the predefined templates, you can also use your own Thymeleaf templates
  - To do this, overwrite the attributes `indexTemplate` and/or `testDetailTemplate`, e.g.:
```kotlin
class CustomHtmlReportGenerator : HtmlReportGenerator(
    testDetailTemplate = "my-custom-test-detail-template.html"
)
```

- Changing the design: instead of using the standard `report-style.css` for styling the HTML files, you can also use a custom CSS file
  - If you want to continue using the default Thymeleaf templates, the easiest way is to change the value for `additionalResources["css/report-style.css"]` when initializing your class, e.g.:
```kotlin
class CustomHtmlReportGenerator : HtmlReportGenerator() {
    init {
        additionalResources["css/report-style.css"] = "my-custom-style.css"
    }
}
```
- Execution of custom functions after the report generation, e.g. sending the report by E-Mail
  - Overwrite the `generate` function and execute your function after calling the method of the superclass, e.g.:
```kotlin
class CustomHtmlReportGenerator : HtmlReportGenerator() {
    override fun generate(result: TestExecutionResult): List<File> {
        val generatedFiles = super.generate(result)
        sendMail(generatedFiles)
        return generatedFiles
    }
}
```

##### Implementation of your own report generator
If you want to generate a report in a completely different format, such as a PDF file of an XML report, you need to implement the [`ReportGenerator`](https://master-thesis1.glpages.informatik.uni-bremen.de/me2e/kdoc/me2e/org.jholsten.me2e.report.generator/-report-generator/index.html) class yourself.
To do this, create a new class that inherits from the `ReportGenerator` and implement the `generate` function.
The return value of this function should then contain the files that were generated with your report generator.

## Running the End-to-End-Tests inside GitLab CI
To run your End-to-End-Tests inside a CI/CD pipeline, such as the [GitLab CI](https://docs.gitlab.com/ee/topics/build_your_application.html), you need an environment in which both a JDK in the correct version and Docker and Docker-Compose are installed.
The easiest way to do this is to define a Dockerfile in which all the required programs are installed.
For example, you can use the following Dockerfile for Java version 17:

```dockerfile
FROM openjdk:17.0.2-jdk-slim

RUN apt-get update && \
    apt-get install -y curl

RUN curl -fsSL https://get.docker.com -o get-docker.sh
RUN sh get-docker.sh
```

If you don't want to build the Docker image yourself, you can alternatively use one of the images from the [me2e GitLab Container Registry](https://gitlab.informatik.uni-bremen.de/master-thesis1/me2e/container_registry):

| JDK-Version | Image                                                               |
|:------------|:--------------------------------------------------------------------|
| 17.0.2      | `gitlab.informatik.uni-bremen.de:5005/master-thesis1/me2e:17.0.2`   |
| 18.0.2.1    | `gitlab.informatik.uni-bremen.de:5005/master-thesis1/me2e:18.0.2.1` |

You can now use this image for the test execution inside the GitLab CI using [Docker-in-Docker (dind)](https://docs.gitlab.com/ee/ci/docker/using_docker_build.html#use-docker-in-docker).

> &#x26A0; **Important Note**\
> If an image of your test environment is not publicly accessible, you need to log in to the corresponding registry first using `docker login`.

When using Docker-in-Docker, the Docker host is no longer the same as the host that executes the tests.
To ensure that the HTTP packets sent by the test runner can still be correctly assigned to this host in the test report and that the [DNS entries created for the Mock Servers](#dns-configuration) continue to point to the correct IP address, you need to export the IP address of the test runner to the environment variable `RUNNER_IP`.

```shell
export RUNNER_IP=$(hostname -I)
```

Overall, the following GitLab CI executes your tests within the Docker image presented above using the command `./gradlew test`.
If at least one of the executed tests fails, the test report is uploaded as an artifact (see `artifacts.when`).

```yaml
# .gitlab-ci.yml
variables:
  DOCKER_TLS_CERTDIR: ""

test:
  stage: test
  image: gitlab.informatik.uni-bremen.de:5005/master-thesis1/me2e:17.0.2
  # Use Docker-in-Docker:
  services:
    - name: docker:20.10.20-dind
      alias: docker
      command: [ "--tls=false" ]
  before_script:
    # Change access permission for gradlew executable:
    - chmod +x ./gradlew
  script:
    # If there is any image inside your environment which is not publicly accessible, you need to log in to the registry:
    - docker login -u REGISTRY_TOKEN -p $REGISTRY_TOKEN $CI_REGISTRY
    # Export IP address of the test runner to environment variable:
    - export RUNNER_IP=$(hostname -I)
    # Run End-to-End-Tests using gradle:
    - ./gradlew test
  variables:
    DOCKER_HOST: "tcp://docker:2375"
  artifacts:
    when: on_failure # Only uploads test report when at least one test has failed
    paths:
      - app/build/reports/me2e
```

> &#x26A0; **Important Note**\
> This GitLab CI assumes that your test project is located in the `app/` subdirectory and that the report is therefore stored in the `app/build/reports/me2e` folder.
> If your project is located in a different directory, you need to adjust the path in the `artifacts` section accordingly.

To display the logs of your tests during the pipeline execution, you should adjust the `test` task in your `build.gradle` as follows:

```groovy
// build.gradle
test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    // Show log output while running tests in pipeline
    testLogging.showStandardStreams = true
}
```

<details>
    <summary><ins>GitLab Runner Configuration (<code>/etc/gitlab-runner/config.toml</code>)</ins></summary>
    
The GitLab runner required for the GitLab CI presented above does not require any special configuration; the default settings for the [Docker executor](https://docs.gitlab.com/runner/executors/docker.html) are sufficient.
    
```toml
# /etc/gitlab-runner/config.toml
[[runners]]
  name = "E2E Docker Executor"
  url = "$GITLAB_URL"
  token = "$GITLAB_RUNNER_TOKEN"
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
    shm_size = 0
```
</details>

### Caching
The previously presented GitLab CI job does not include any caching, so that with each pipeline execution
- the entire Gradle project is built and
- all Docker images are pulled

To improve the performance of your pipeline and thus reduce the runtime, you can set up both a Gradle and a Docker cache.

#### Gradle Build Cache
Add the following entries to your GitLab CI to set up the [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html):

```yaml
variables:
  GRADLE_USER_HOME: $CI_PROJECT_DIR/.gradle

cache:
  - key:
      files:
        - gradle/wrapper/gradle-wrapper.properties
    paths:
      - .gradle/wrapper
      - .gradle/caches
```

In order for the cache to be used when executing the tests, you need to add the `--build-cache` flag to the Gradle test task.

```shell
./gradlew --build-cache test
```

<details>
    <summary><ins>Complete GitLab CI with Gradle Build Cache</ins></summary>
    
```yaml
# .gitlab-ci.yml
variables:
  DOCKER_TLS_CERTDIR: ""
  GRADLE_USER_HOME: $CI_PROJECT_DIR/.gradle

cache:
  - key:
      files:
        - gradle/wrapper/gradle-wrapper.properties
    paths:
      - .gradle/wrapper
      - .gradle/caches

test:
  stage: test
  image: gitlab.informatik.uni-bremen.de:5005/master-thesis1/me2e:17.0.2
  services:
    - name: docker:20.10.20-dind
      alias: docker
      command: [ "--tls=false" ]
  before_script:
    - chmod +x ./gradlew
  script:
    # If there is any image inside your environment which is not publicly accessible, you need to log in to the registry:
    - docker login -u REGISTRY_TOKEN -p $REGISTRY_TOKEN $CI_REGISTRY
    - export RUNNER_IP=$(hostname -I)
    - ./gradlew --build-cache test
  variables:
    DOCKER_HOST: "tcp://docker:2375"
  artifacts:
    when: on_failure
    paths:
      - app/build/reports/me2e
```
</details>

#### Docker Image Cache
By default, no image cache is saved when using Docker-in-Docker, i.e. all images of your environment are completely pulled again with each pipeline execution.
One way to maintain the image cache across pipeline executions is to use [`docker save`](https://docs.docker.com/engine/reference/commandline/image_save/).
This command saves all specified images in a tarred repository, which can then be loaded with [`docker load`](https://docs.docker.com/engine/reference/commandline/image_load/).

To save all Docker images used in the pipeline execution to a file named `cache.tar`, add the following `after_script` step to your GitLab CI job:

```yaml
after_script:
  - echo "Storing Docker Image Cache..."
  - mkdir -p docker
  - docker save $(docker images -q) -o docker/cache.tar
```

To load this image cache before executing the tests, add the following command to your `before_script` step:

```shell
if [[ -f "docker/cache.tar" ]]; then
echo "Loading Docker Image Cache...";
docker load -i docker/cache.tar;
fi
```

Finally, you need to add the following entry to your GitLab CI so that the cache in the `docker` folder is available across the pipeline executions:

```yaml
cache:
  - key: docker-cache
    paths:
      - docker/
```

<details>
    <summary><ins>Complete GitLab CI with Docker Image Cache</ins></summary>
    
```yaml
# .gitlab-ci.yml
variables:
  DOCKER_TLS_CERTDIR: ""

cache:
  - key: docker-cache
    paths:
      - docker/

test:
  stage: test
  image: gitlab.informatik.uni-bremen.de:5005/master-thesis1/me2e:17.0.2
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
    # If there is any image inside your environment which is not publicly accessible, you need to log in to the registry:
    - docker login -u REGISTRY_TOKEN -p $REGISTRY_TOKEN $CI_REGISTRY
    - export RUNNER_IP=$(hostname -I)
    - ./gradlew test
  after_script:
    - echo "Storing Docker Image Cache..."
    - mkdir -p docker
    - docker save $(docker images -q) -o docker/cache.tar
  variables:
    DOCKER_HOST: "tcp://docker:2375"
  artifacts:
    when: on_failure
    paths:
      - app/build/reports/me2e
```
</details>

### Publishing Test Reports
With the GitLab CI presented above, the HTML and CSS files forming the test report are published in the job's artifacts and can be downloaded as a ZIP file.
To ease access to this report, you can alternatively publish it in the [GitLab Pages](https://docs.gitlab.com/ee/user/project/pages/) of your repository so that anyone can access it directly via the browser.
To publish every test report - regardless of whether the test execution was successful or not - you should first adjust the `artifacts.when` setting for your test job as follows:

```yaml
artifacts:
  when: always
  paths:
    - app/build/reports/me2e
```
 
Now add the following job to your GitLab CI to publish the report generated in the `test` job on your repository's GitLab pages:

```yaml
pages:
  stage: publish
  allow_failure: true
  script: 
    - mkdir -p public/$CI_PIPELINE_ID
    - cp app/build/reports/me2e/* public/$CI_PIPELINE_ID/ -R
    - echo "Published test report to $CI_PAGES_URL/$CI_PIPELINE_ID/index.html"
  artifacts:
    paths:
      - public
``` 

This copies the files from the directory `app/build/reports/me2e` saved in the `test` job to the `public/$CI_PIPELINE_ID` directory so that the test reports for each pipeline are each in their own directory.
The `allow_failure` flag is set to `true` for this job, as it is possible that no report has been published in the `test` job, e.g. if you have not changed the code and the [Gradle Build Cache](#gradle-build-cache) is activated.

To ensure that several test reports are accessible in the GitLab pages at the same time and that one execution of the `pages` job does not overwrite all previously published test reports, you also need to add the following entry to your GitLab CI: 

```yaml
cache:
  - paths:
      - public
```

As a result, the test report for every pipeline execution is now accessible at the URL `{CI_PAGES_URL}/{CI_PIPELINE_ID}/index.html`.

<details>
    <summary><ins>Complete GitLab CI with Publication of Test Reports</ins></summary>
    
```yaml
# .gitlab-ci.yml
variables:
  DOCKER_TLS_CERTDIR: ""

stages:
  - test
  - publish

cache:
  - paths:
      - public

test:
  stage: test
  image: gitlab.informatik.uni-bremen.de:5005/master-thesis1/me2e:17.0.2
  services:
    - name: docker:20.10.20-dind
      alias: docker
      command: [ "--tls=false" ]
  before_script:
    - chmod +x ./gradlew
  script:
    # If there is any image inside your environment which is not publicly accessible, you need to log in to the registry:
    - docker login -u REGISTRY_TOKEN -p $REGISTRY_TOKEN $CI_REGISTRY
    - export RUNNER_IP=$(hostname -I)
    - ./gradlew test
  variables:
    DOCKER_HOST: "tcp://docker:2375"
  artifacts:
    when: always
    paths:
      - app/build/reports/me2e
        
pages:
  stage: publish
  allow_failure: true
  script: 
    - mkdir -p public/$CI_PIPELINE_ID
    - cp app/build/reports/me2e/* public/$CI_PIPELINE_ID/ -R
    - echo "Published test report to $CI_PAGES_URL/$CI_PIPELINE_ID/index.html"
  rules:
    - exists:
        - build/report/me2e
  artifacts:
    paths:
      - public
```
</details>

### Bringing it all together: Recommended GitLab CI
The following GitLab CI pipeline includes [Gradle Build Cache](#gradle-build-cache), [Docker Image Cache](#docker-image-cache) and publishes each test report on the repository's [GitLab Pages](#publishing-test-reports).

```yaml
# .gitlab-ci.yml
variables:
  DOCKER_TLS_CERTDIR: ""
  GRADLE_USER_HOME: $CI_PROJECT_DIR/.gradle

stages:
  - test
  - publish

test:
  stage: test
  image: gitlab.informatik.uni-bremen.de:5005/master-thesis1/me2e:17.0.2
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
  variables:
    DOCKER_HOST: "tcp://docker:2375"
  cache:
    - key:
        files:
          - gradle/wrapper/gradle-wrapper.properties
      paths:
        - .gradle/wrapper
        - .gradle/caches
    - key: docker-cache
      paths:
        - docker/
  artifacts:
    when: always
    paths:
      - app/build/reports/me2e

pages:
  stage: publish
  allow_failure: true
  script:
    - mkdir -p public/$CI_PIPELINE_ID
    - cp app/build/reports/me2e/* public/$CI_PIPELINE_ID/ -R
    - echo "Published test report to $CI_PAGES_URL/$CI_PIPELINE_ID/index.html"
  cache:
    paths:
      - public
  artifacts:
    paths:
      - public
```
