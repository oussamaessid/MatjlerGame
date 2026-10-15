# Écran Classements — correction et nouveau design

L’accès dans le menu utilise une icône trophée accessible (« Classements ») ; le profil, le trophée et l’aide occupent des zones distinctes pour éviter les chevauchements.

Écran dédié : en-tête violet, sélecteurs jour/mois et difficulté, période toujours renseignée, podium pour les trois meilleurs joueurs réels quand ils sont disponibles, liste paginée avec avatars/victoires/temps. Les états chargement, vide et erreur sont distincts. Aucun faux joueur n’est ajouté aux résultats réels.

Correction des erreurs : un échec d’initialisation du compte interrompt le chargement sans relancer la même requête pour le classement. Les codes techniques ne sont plus affichés au joueur et le démarrage du jeu ne présente plus de fenêtre d’erreur systématique. Un échec d’envoi de scores peut être signalé discrètement quand le classement reste lisible.

Diagnostic distant vérifié : application Android enregistrée sur equal-to-81b8f, facturation désactivée (billingEnabled=false), configuration Firebase Authentication absente (GET config : CONFIGURATION_NOT_FOUND). Ces réglages distants et le déploiement des fonctions restent nécessaires pour afficher les vrais classements. Aucun réglage payant et aucun déploiement cloud n’ont été effectués pendant cette correction. La console d’activation Auth reste bloquée au chargement dans le navigateur intégré.

Validation : build APK réussi, 18 tests JVM réussis, trois tests d’interface réussis sur l’émulateur Pixel_8_Pro (sélecteurs, erreur lisible avec Réessayer, résultats/temps/pseudos longs). Le premier lancement hors ligne des tests sur appareil a nécessité de récupérer une dépendance officielle de test ; le lancement suivant a réussi.

Captures natives : classement-indisponible.png montre l’état correspondant au problème de configuration actuellement constaté. classement-apercu-donnees-test.png présente le nouveau podium avec des données de test utilisées uniquement dans les tests d’interface, jamais écrites dans Firebase ni affichées comme vrais joueurs dans l’application.
