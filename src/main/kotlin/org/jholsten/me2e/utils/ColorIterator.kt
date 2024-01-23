package org.jholsten.me2e.utils

import java.awt.Color

/**
 * Deterministic iterator which iterates over all possible colors.
 * Adapted from https://stackoverflow.com/a/21863798/17203788.
 */
class ColorIterator(initialStepSize: Float = 0.1F) {
    init {
        require(initialStepSize < 1 && initialStepSize > 0) { "Initial step size needs to be between 0 and 1." }
    }

    private var stepSize: Float = initialStepSize
    private var step: Float = 0.0F

    /**
     * Returns the next color of this iterator.
     * If the current [step] exceeds 1.0, the step size is decreased.
     * If a color has already been visited, the current [step] is skipped.
     */
    fun next(): Color {
        step += stepSize
        if (step > 1) {
            stepSize /= 10
            step = stepSize
        } else if (step % (step * 10) == 0.0F) {
            step += stepSize
        }
        return Color.getHSBColor(step, 1.0F, 1.0F)
    }
}
