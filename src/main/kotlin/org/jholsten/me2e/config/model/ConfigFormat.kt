package org.jholsten.me2e.config.model

import org.jholsten.me2e.config.parser.ConfigParser
import org.jholsten.me2e.config.parser.YamlConfigParser

/**
 * Supported formats for the test configuration.
 */
enum class ConfigFormat(
    /**
     * Parser to use for reading the test configuration.
     */
    @JvmSynthetic
    internal val parser: ConfigParser
) {
    YAML(YamlConfigParser())
}
