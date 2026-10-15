package app.matjlergame.presentation.model

import app.matjlergame.data.repository.FirebaseRequestException
import java.io.IOException

/** No raw server message, API key or technical configuration is displayed to players. */
fun playerServiceMessage(error: Exception): String {
    if (error is IOException) return "Connexion au classement impossible. Vérifiez Internet puis réessayez. Votre résultat reste enregistré sur cet appareil."
    if (error is FirebaseRequestException) {
        val detail = error.message.orEmpty()
        if (detail.contains("MISSING_APP_CONFIGURATION") || detail.contains("CONFIGURATION_NOT_FOUND"))
            return "Configuration du classement absente de cette version. Mettez l’application à jour."
        if (error.status == "PERMISSION_DENIED" || error.reason?.startsWith("API_KEY") == true)
            return "Le service refuse la connexion au classement. Votre résultat est conservé ; réessayez après la mise à jour de l’application."
        if (error.status == "FAILED_PRECONDITION")
            return "Le classement est temporairement indisponible. Votre résultat est conservé ; réessayez dans un instant."
        if (error.status == "INVALID_ARGUMENT")
            return "Un résultat n’a pas pu être synchronisé. Il reste enregistré sur cet appareil."
    }
    return "Le classement n’a pas pu être actualisé. Votre résultat reste enregistré sur cet appareil."
}
