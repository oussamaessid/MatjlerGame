package app.matjlergame.domain.usecase

import app.matjlergame.domain.model.TileStatus

class CalculateTileStatusesUseCase {
    operator fun invoke(guess: String, solution: String, slots: Int): List<TileStatus> {
        val targetArr = solution.toMutableList()
        val guessArr = guess.toList()
        val statuses = MutableList(slots) { TileStatus.ABSENT }

        for (i in 0 until slots) {
            if (guessArr[i] == targetArr[i]) {
                statuses[i] = TileStatus.CORRECT
                targetArr[i] = 0.toChar()
            }
        }

        for (i in 0 until slots) {
            if (statuses[i] != TileStatus.CORRECT) {
                val idx = targetArr.indexOf(guessArr[i])
                if (idx != -1) {
                    statuses[i] = TileStatus.PRESENT
                    targetArr[idx] = 0.toChar()
                }
            }
        }

        return statuses
    }
}
