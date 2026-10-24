# Equal To — améliorations produit

Avis et demande enregistrés le 23 octobre 2026 : rendre le jeu plus facile à découvrir, donner envie de revenir et faciliter sa recommandation.

## Livré dans cette mise à jour

- Exemple guidé interactif au premier lancement, indépendant des vrais défis et des scores. Accessible à nouveau dans « Comment jouer » ; choix conservé sur l’appareil.
- Accueil en français avec une promesse claire, progression des trois défis du jour et statistiques personnelles existantes (parties, victoires, réussite).
- Cartes indiquant les défis terminés et permettant de retrouver leur résultat.
- Résultats déjà enregistrés consultables sans imposer une connexion à Internet.
- Contenu central défilant pour garder les options accessibles sur petits écrans.

Les résultats et statistiques existants sont conservés. Le partage actuel dispose déjà d’un lien Google Play et d’un résultat sans solution dévoilée.

## Prochaines étapes proposées

1. Tester avec 20 à 30 personnes : compréhension sans aide, première partie terminée, retour le lendemain, partage.
2. Ajouter un entraînement libre hors ligne avec des énigmes distinctes, sans toucher au classement quotidien.
3. Vérifier le classement entre deux vrais téléphones et terminer la configuration des services selon firebase/README.md.
4. Ajouter les traductions arabe et anglaise, y compris mise en page de droite à gauche.
5. Évaluer la fréquence des publicités à partir des abandons observés et proposer un rappel quotidien facultatif.
6. Fiabiliser le calcul des séries par jours consécutifs avant de mettre cet indicateur en avant.

Aucune promesse d’augmentation des installations : la validation nécessite des essais auprès de joueurs et des mesures d’utilisation. Cette mise à jour n’est pas une publication sur Google Play.

## Vérifications de cette version

- Compilation de l’APK debug réussie.
- 24 tests unitaires réussis.
- Deux tests du tutoriel réussis sur l’émulateur Pixel 8 Pro : progression avec retour coloré puis fin, et fermeture anticipée.
- Vérification des espaces et du diff réussie.

## Refonte de l’accueil — 24 octobre 2026

À la demande du propriétaire, suppression du défilement de l’accueil. Nouvelle palette claire avec encre sombre et accents citron, hiérarchie simplifiée, cartes de difficulté sobres, progression quotidienne compacte. Mise en page avec hauteurs flexibles, variantes compactes et disposition en deux colonnes en paysage. Profil, aide, classement et résultats conservés. La bannière conserve un emplacement fixe et est libérée lors de la fermeture de l’écran.

## Adaptation aux formats de téléphone — 24 octobre 2026

- Accueil sans défilement : variante compacte, textes secondaires masqués lorsque l’espace manque, traitement des caractères agrandis et limitation de largeur sur les écrans larges.
- Jeu : dimensionnement des cases d’après l’espace réellement disponible après les marges système ; grille et clavier côte à côte en paysage, clavier sur trois rangées dans ce mode ; grille défilante seulement si nécessaire pour préserver la lisibilité.
- Profil : prise en compte du clavier logiciel et titre flexible.
- Aide, tutoriel, chargement et fenêtres de connexion : contenu accessible sur les faibles hauteurs.
- Résultats : statistiques sur deux colonnes dans les fenêtres étroites ou avec de grands caractères.
- Tests de calcul sur des largeurs de 240 à 600 dp ; tests d’interface réussis sur 280×480, 320×568, 360×640, texte jusqu’à 200 % et paysage 640×320 dp.

Validation finale : compilation debug réussie, 26 tests unitaires et 3 tests d’interface réussis sur l’émulateur. Ces formats couvrent les cas vérifiés ; ils ne constituent pas une certification de tous les téléphones existants.
