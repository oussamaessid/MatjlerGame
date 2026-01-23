package app.matjlergame.domain.model

enum class TileStatus { EMPTY, FILLED, CORRECT, PRESENT, ABSENT }
enum class GameMode { EASY, MEDIUM, HARD }
enum class Screen { MODE_SELECT, LEVEL_SELECT, GAME,HOW_TO_PLAY }

data class Level(
    val number: Int,
    val target: Int,
    val solution: String,
    val slots: Int,
    val maxGuesses: Int
)

data class GameState(
    val guesses: List<List<String>> = emptyList(),
    val tileStatuses: List<List<TileStatus>> = emptyList(),
    val currentGuess: Int = 0,
    val currentPos: Int = 0,
    val gameOver: Boolean = false,
    val message: String = "",
    val isShaking: Boolean = false,
    val isWon: Boolean = false
)
