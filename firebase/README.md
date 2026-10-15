# Classement global Firebase (plan gratuit Spark)

Le classement n’utilise ni Firebase Auth ni Cloud Functions : ces deux services exigent la facturation Blaze
(`BILLING_NOT_ENABLED`). L’application écrit et lit directement dans Cloud Firestore via son API REST, avec la clé
de `app/google-services.json`. Les règles de `firestore.rules` contrôlent ce qui est accepté.

## Données

`leaderboards/{tableau}/entries/{uuidJoueur}`, avec `tableau` = `day_{EASY|MEDIUM|HARD}_{AAAA-MM-JJ}` ou
`month_{EASY|MEDIUM|HARD}_{AAAA-MM}` (périodes à l’heure de Tunis). Champs : `name`, `avatar`, `correctCount`
(victoires), `totalMs` (somme des temps), `updatedAt` (heure serveur).

- L’UUID du joueur est créé au premier lancement et conservé sur l’appareil.
- Après chaque partie, au lancement et à l’ouverture du classement, l’app envoie les totaux du jour et du mois
  (jour courant et veille, pour les envois différés). Envoi silencieux : en cas d’échec il est retenté plus tard.
- Lecture : top 50 par victoires décroissantes, puis temps total croissant ; « Voir plus » pour la suite.
  Hors ligne, l’écran affiche le dernier classement en cache, sinon une liste vide. Aucun message d’erreur.

## Déploiement des règles et de l’index

```sh
npm install -g firebase-tools
firebase login
cd firebase
firebase deploy --only firestore --project equal-to-81b8f
```

Ou à la main dans la console Firebase : Firestore → Règles (coller `firestore.rules`), et Firestore → Index →
index composite sur la collection `entries` : `correctCount` décroissant, `totalMs` croissant.

## Limites

Sans compte serveur, un joueur motivé peut envoyer un faux score. Les règles bornent les valeurs (1 victoire max
par jour et par mode, 31 par mois, 48 h max par victoire), empêchent de baisser un score et interdisent la
suppression. Pour un jeu casual c’est acceptable ; une protection plus forte demanderait Blaze (Functions) ou
App Check. Le dossier `functions/` (ancienne version Auth + Functions) n’est plus utilisé ni déployé.
