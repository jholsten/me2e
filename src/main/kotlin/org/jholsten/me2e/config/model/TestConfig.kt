package org.jholsten.me2e.config.model

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.mock.MockServer

/**
 * Model containing the test configuration to use.
 */
class TestConfig(
    /**
     * List of Docker containers to start for the end-to-end test.
     */
    val containers: List<Container>,

    /**
     * List of third party services to be mocked.
     */
    val mockServers: List<MockServer>,
) {

}
