package app.matjlergame.domain.repository

import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.Level


interface LevelRepository {
    fun getLevelsForMode(mode: GameMode): List<Level>
    fun getLevelByNumber(mode: GameMode, levelNumber: Int): Level?
}