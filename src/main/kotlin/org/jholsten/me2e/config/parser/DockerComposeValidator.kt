package org.jholsten.me2e.config.parser

import org.jholsten.me2e.container.docker.DockerCompose
import org.jholsten.me2e.container.docker.DockerComposeVersion
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.parsing.utils.Validator
import java.io.File

/**
 * Validator for the contents of a Docker-Compose file.
 * Uses `docker compose config` command which returns a non-zero exit code in case the file is invalid.
 */
internal class DockerComposeValidator : Validator<File> {

    /**
     * Validates the contents of the given Docker-Compose file using `docker compose config`.
     * @param value File to validate.
     * @throws ValidationException if validation failed.
     */
    override fun validate(value: File) {
        val dockerCompose = DockerCompose.Builder("validator", value, DockerComposeVersion.V2)
            .withLocalCompose(true)
            .build()

        dockerCompose.validate()
    }
}
