# Galerie des joueurs

Les 48 images p1.png à p48.png sont reprises à l’identique depuis le projet local fourni par le propriétaire : `/Users/apple/StudioProjects/LudoKing/app/src/main/res/drawable`. Les fichiers sources n’ont pas été modifiés. Copie dans `app/src/main/res/drawable-nodpi/avatar_ludo_01.png` à `avatar_ludo_48.png` ; leur contenu est vérifié par SHA-256 après copie.

Les identifiants 0–7 conservent les huit anciens avatars. Les images Ludo utilisent les identifiants persistants 8–55 ; le premier avatar Ludo (8) devient le défaut des nouveaux profils. La sélection est disponible dans une grille défilante de hauteur limitée pour garder le bouton Enregistrer accessible. Affichage arrondi dans le profil, le menu et les classements.

La validation serveur accepte les identifiants 0–55. Ces fonctions doivent être déployées avec la mise à jour Android pour synchroniser les nouveaux choix d’avatar. Aucun déploiement cloud n’a été effectué pour cet import.
