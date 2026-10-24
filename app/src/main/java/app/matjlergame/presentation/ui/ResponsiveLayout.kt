package app.matjlergame.presentation.ui

/** Uses the actual game pane after system insets, header, keyboard and banner. */
internal fun responsiveTileSize(width: Float, height: Float, columns: Int, rows: Int, gap: Float): Float {
    require(columns > 0 && rows > 0)
    val byWidth = (width - 32f - gap * (columns - 1)) / columns
    val byHeight = (height - 64f - gap * (rows - 1)) / rows
    // A short pane scrolls vertically rather than producing negative or unreadable cells.
    return minOf(byWidth, byHeight.coerceAtLeast(28f), 54f).coerceAtLeast(1f)
}
