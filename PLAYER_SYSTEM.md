# Joueurs, chronomètre et classement

Profil invité automatique, pseudo modifiable et 56 avatars prédéfinis : 48 images copiées de LudoKing et huit icônes historiques. Écran de bienvenue et profil ; modifications conservées sur l’appareil et mises en attente si la connexion échoue. Configuration réelle de equal-to-81b8f dans app/google-services.json, incluse dans l’APK.

Le jeu affiche un chronomètre : reprise après fermeture depuis le début sauvegardé, mesure monotone pendant la session, arrêt immédiat à la réponse gagnante avant l’animation et arrêt à la défaite définitive. Les pauses et les publicités comptent. Le temps affiché est celui de l’appareil ; le classement utilise le temps calculé par le serveur jusqu’à la réception de la réponse.

Six classements globaux indépendants : Facile, Moyen et Difficile, chacun pour le jour et le mois. Pour la difficulté sélectionnée : nombre de parties gagnées décroissant, puis somme des temps des victoires croissante, puis UID pour stabiliser les égalités. Une victoire par mode/jour, protection transactionnelle contre les doublons, périodes Africa/Tunis. Les résultats non classés ne sont pas envoyés au classement.

Quand le démarrage d’une partie classée échoue, le joueur peut réessayer, revenir ou choisir explicitement Jouer sans classement. Ce choix est conservé pour cette partie afin de ne pas commencer le chronométrage serveur après avoir déjà vu l’énigme. Une indication Partie non classée est affichée.

État cloud vérifié : Firestore Standard créé en europe-west1, règles fermées aux clients et index de classement déployés. Firebase Hosting n’est pas activé. Les fonctions de validation ne sont pas déployées et Auth n’est pas initialisé. Google confirme billingEnabled=false ; l’initialisation Auth par l’API utilisée renvoie BILLING_NOT_ENABLED. Avec l’architecture actuelle, le classement entre téléphones ne peut pas encore fonctionner tant que le propriétaire n’a pas activé Blaze et que les fonctions ne sont pas déployées.

Validation : build debug et tests unitaires Android/JVM, tests de logique serveur. Les tests sur appareil restent à exécuter ; le démarrage de l’émulateur a été empêché par des restrictions de l’environnement. Voir firebase/README.md pour le déploiement.
