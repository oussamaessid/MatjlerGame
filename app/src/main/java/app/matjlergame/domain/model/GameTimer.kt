package app.matjlergame.domain.model

/**
 * Pausable chronometer: only time spent on the game screen counts. [savedMs] is the time already
 * played in earlier visits; monotonic time measures the current visit without clock jumps.
 */
class GameTimer(savedMs: Long, nowMonotonicMs: Long) {
    private var accumulatedMs = savedMs.coerceAtLeast(0)
    private var runningSince: Long? = nowMonotonicMs
    private var frozenMs: Long? = null
    fun elapsed(nowMonotonicMs: Long): Long = frozenMs ?: (accumulatedMs +
        (runningSince?.let { (nowMonotonicMs - it).coerceAtLeast(0) } ?: 0))
    /** Leaving the screen or the app: banks the running time and stops counting. */
    fun pause(nowMonotonicMs: Long): Long = elapsed(nowMonotonicMs).also {
        if (frozenMs == null) { accumulatedMs = it; runningSince = null }
    }
    fun resume(nowMonotonicMs: Long) { if (frozenMs == null && runningSince == null) runningSince = nowMonotonicMs }
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
