package app.matjlergame

import app.matjlergame.domain.model.GameTimer
import org.junit.Assert.*
import org.junit.Test

class GameTimerTest {
    @Test fun restoreResumesFromSavedTimeOnly() {
        val timer = GameTimer(60000, 500)
        assertEquals(62000L, timer.elapsed(2500))
    }
    @Test fun timeOutsideTheGameIsNotCounted() {
        val timer = GameTimer(0, 0)
        assertEquals(3000L, timer.pause(3000))
        assertEquals(3000L, timer.elapsed(90000))
        timer.resume(90000)
        assertEquals(5000L, timer.elapsed(92000))
    }
    @Test fun winningTimeFreezesBeforeAnimationsAndNavigation() {
        val timer = GameTimer(0, 500)
        assertEquals(5000L, timer.stop(5500))
        assertEquals(5000L, timer.elapsed(9000))
        timer.resume(10000)
        assertEquals(5000L, timer.stop(12000))
    }
    @Test fun completedGameRetainsRecordedDuration() {
        val timer = GameTimer(199000, 100)
        timer.restoreFinished(4500)
        assertEquals(4500L, timer.elapsed(100000))
    }
    @Test fun readableMinutesAndHours() {
        assertEquals("00:00", GameTimer.format(-1))
        assertEquals("02:05", GameTimer.format(125000))
        assertEquals("1:01:01", GameTimer.format(3661000))
    }
}
