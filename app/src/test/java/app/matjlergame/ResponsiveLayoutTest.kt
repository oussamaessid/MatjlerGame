package app.matjlergame

import app.matjlergame.presentation.ui.responsiveTileSize
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveLayoutTest {
    @Test fun gridFitsPhoneWidthsAndRemainsPositiveInShortWindows() {
        for (width in listOf(240f, 280f, 320f, 360f, 412f, 600f)) {
            for (height in listOf(40f, 120f, 220f, 500f)) {
                for (columns in 4..8) for (rows in 4..6) {
                    val tile = responsiveTileSize(width, height, columns, rows, 3f)
                    assertTrue("Invalid tile for $width x $height", tile > 0f)
                    assertTrue("Grid wider than pane", tile * columns + 3 * (columns - 1) <= width - 32 + 0.01f)
                    assertTrue("Tile larger than intended", tile <= 54f)
                }
            }
        }
    }

    @Test fun gridFitsAvailableHeightWhenMinimumCellSizeFits() {
        for (height in listOf(260f, 400f, 600f)) {
            val tile = responsiveTileSize(412f, height, 6, 6, 3f)
            assertTrue(tile * 6 + 15 + 64 <= height + 0.01f)
        }
    }
}
