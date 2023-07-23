package org.jholsten.me2e.mock

/**
 * Model representing a third party service to be mocked.
 */
class MockServer(
    /**
     * Unique name of this mock server.
     */
    val name: String,

    /**
     * List of paths to stub definitions. The files need to be located in `resources` folder.
     */
    val stubs: List<String> = listOf(),
) {
}
