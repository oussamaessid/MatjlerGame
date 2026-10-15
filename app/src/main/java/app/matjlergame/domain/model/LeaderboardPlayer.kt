package app.matjlergame.domain.model

data class LeaderboardPlayer(val uid: String, val name: String, val avatar: Int, val wins: Int, val durationMs: Long,
    /** Position in the whole board; 0 = unknown (row index is used). */ val rank: Int = 0)
