package org.jholsten.me2e.utils

import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertFalse

class ColorIteratorTest {

    @Test
    fun `Color iterator should not return a color twice`() {
        val iterator = ColorIterator()
        val colors: MutableList<Color> = mutableListOf()

        for (i in 0 until 500) {
            val color = iterator.next()
            assertFalse(color in colors, "Color $color has already been returned by iterator. Current step: $i.")
            colors.add(color)
        }
    }
}
