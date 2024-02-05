package org.jholsten.me2e.utils

/**
 * Lazy property whose value can be reset to be recomputed.
 */
internal class ResettableLazy<out T>(private var initializer: () -> T) : Lazy<T> {

    @Volatile
    private var wrap = Wrap()

    /**
     * Reference to the lazy value which is currently stored.
     */
    @get:JvmSynthetic
    override val value: T get() = wrap.lazy.value

    /**
     * Returns whether the lazy getter is initialized.
     */
    @JvmSynthetic
    override fun isInitialized() = wrap.lazy.isInitialized()

    /**
     * Resets the lazy property to initiate a recomputation when the value is retrieved
     * the next time.
     */
    @JvmSynthetic
    fun reset() {
        wrap = Wrap()
    }

    override fun toString() = wrap.lazy.toString()

    private inner class Wrap {
        val lazy = lazy(initializer)
    }
}
