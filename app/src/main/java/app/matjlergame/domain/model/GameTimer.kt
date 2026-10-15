package app.matjlergame.domain.model

/** Wall time restores a saved start; monotonic time measures this process without clock jumps. */
class GameTimer(startedAtWallMs: Long, nowWallMs: Long, private val startedAtMonotonicMs: Long) {
    private val restoredMs = (nowWallMs - startedAtWallMs).coerceAtLeast(0)
    private var frozenMs: Long? = null
    fun elapsed(nowMonotonicMs: Long): Long = frozenMs ?: (restoredMs +
        (nowMonotonicMs - startedAtMonotonicMs).coerceAtLeast(0))
    fun stop(nowMonotonicMs: Long): Long = elapsed(nowMonotonicMs).also { frozenMs = it }
    fun restoreFinished(durationMs: Long) { frozenMs = durationMs.coerceAtLeast(0) }
    companion object {
        fun format(durationMs: Long): String {
            val seconds = durationMs.coerceAtLeast(0) / 1000
            return if (seconds < 3600) String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
            else String.format(java.util.Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
        }
    }
}
