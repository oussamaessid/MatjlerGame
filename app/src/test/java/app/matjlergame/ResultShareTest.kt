package app.matjlergame
import app.matjlergame.sharing.*
import org.junit.Assert.*
import org.junit.Test
class ResultShareTest {
    private fun sample(english: Boolean = false, won: Boolean = true) = ShareResult(
        "Words Game", "Joueur Test", "2026-10-14", if(english) "Daily word · English" else "Mot du jour · Français",
        won, 3, 6, 83400, english, true,
        listOf(listOf(ShareTile.CORRECT,ShareTile.PRESENT,ShareTile.ABSENT,ShareTile.ABSENT,ShareTile.CORRECT)))
    @Test fun frenchResultContainsFrozenDateAndTimeWithoutSolution() {
        val text=sample().text
        assertTrue(text.contains("14/10/2026")); assertTrue(text.contains("01:23"))
        assertTrue(text.contains("J’ai trouvé le mot du jour")); assertTrue(text.contains("🟩🟨⬛⬛🟩"))
        assertFalse(text.contains("POMME")); assertTrue(text.endsWith(ShareResult.PLAY_LINK))
    }
    @Test fun englishResultAndLossNeverClaimVictory() {
        val result=sample(english=true,won=false)
        assertTrue(result.text.contains("Time : 01:23"));assertTrue(result.text.contains("Attempts : X/6"))
        assertFalse(result.text.contains("I found"));assertTrue(result.text.contains("English"))
    }
    @Test fun eachMathDifficultyIsPreserved() {
        for(mode in listOf("Facile","Moyen","Difficile")) {
            val result=ShareResult("Equal To","Alice","2026-10-15","Mode $mode",true,2,5,3723000)
            assertTrue(result.text.contains("Mode $mode")); assertFalse(result.text.contains("Niveau")); assertTrue(result.text.contains("15/10/2026"));assertTrue(result.text.contains("1:02:03"))
            assertTrue(result.text.contains("Essais : 2/5"))
        }
    }
    @Test fun missingHistoricalTimeIsNeverInvented() {
        assertEquals("Indisponible",sample().copy(durationMs=null).timeLabel)
        assertEquals("Unavailable",sample(true).copy(durationMs=0).timeLabel)
    }
}
