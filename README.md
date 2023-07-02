# me2e Library for End-to-End Tests for Microservice Applications
me2e (Microservice End-to-End) is a library for writing end-to-end tests for microservice applications.

## Usage
### Import
The library is published to the [GitLab Package Registry](https://gitlab.informatik.uni-bremen.de/api/v4/projects/33508/packages/maven).
Import the library by adding the registry to the repository section of your Maven or Gradle project.

**Maven (`pom.xml`):**
```xml

```

**Gradle (`build.gradle`):**
```groovy
maven {
    url "https://gitlab.informatik.uni-bremen.de/api/v4/projects/33508/packages/maven"
    name "GitLab"
    credentials(HttpHeaderCredentials) {
        name = 'Deploy-Token'
        value = '<DEPLOY-TOKEN>'
    }
    authentication {
        header(HttpHeaderAuthentication)
    }
}
```

The library can then be included as a dependency with the selected version.

**Maven (`pom.xml`):**
```xml

```

**Gradle (`build.gradle`):**
```groovy
dependencies {
    implementation "org.jholsten:me2e:$me2eVersion"
}
```
