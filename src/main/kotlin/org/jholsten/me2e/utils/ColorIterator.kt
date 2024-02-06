package org.jholsten.me2e.utils

import java.awt.Color
import java.math.BigDecimal
import java.math.MathContext

/**
 * Deterministic iterator which iterates over all possible colors.
 * Adapted from https://stackoverflow.com/a/21863798/17203788.
 * Note that depending on the initial step size, the number of possible colors is limited due to precision.
 * For an initial step size of `0.15`, there are 668 possible colors.
 */
internal class ColorIterator(initialStepSize: BigDecimal = BigDecimal("0.15")) {
    init {
        require(initialStepSize < BigDecimal.ONE && initialStepSize > BigDecimal.ZERO) { "Initial step size needs to be between 0 and 1." }
    }

    private var stepSize: BigDecimal = initialStepSize
    private var step: BigDecimal = BigDecimal.ZERO
    private val mc: MathContext = MathContext(100)

    /**
     * Returns the next color of this iterator.
     * If the current [step] exceeds 1.0, the step size is decreased.
     * If a color has already been visited, the current [step] is skipped.
     */
    @JvmSynthetic
    fun next(): Color {
        step += stepSize
        if (step > BigDecimal.ONE) {
            stepSize = stepSize.divide(BigDecimal.TEN, mc)
            step = stepSize
        } else if ((step % (stepSize * BigDecimal.TEN)).compareTo(BigDecimal.ZERO) == 0) {
            step += stepSize
        }
        return Color.getHSBColor(step.toFloat(), 0.9F, 1.0F)
    }
}
