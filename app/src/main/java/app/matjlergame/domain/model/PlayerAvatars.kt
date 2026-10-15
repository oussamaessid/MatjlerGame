package app.matjlergame.domain.model

/** Persisted IDs: keep 0–7 for existing profiles; LudoKing p1–p48 are IDs 8–55. */
object PlayerAvatars {
    const val COUNT = 56
    const val DEFAULT = 11 // Ludo 04: male portrait
    fun isValid(id: Int) = id in 0 until COUNT
}
