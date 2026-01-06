
# Klik: un explorateur de système de fichers avec une forte orientation image #

Trier vos fichiers, documents et images de manière intuitive et rapide. 
Ils sont représentés par des icônes que vous pouvez glisser-déposer d'une fenêtre vers une autre, chaque fenêtre correspondant à un dossier.  

Klik est 100% gratuit.


[Installation pour MacOS](MacOS_fr.md)

[Installation pour Windows](Windows_fr.md)

[Installation pour Linux](Linux.md)


# Fonctionnement intuitif #

- Les images et les PDFs sont représentées avec des icônes, les films avec des icônes animées.
- On peut glisser-déposer les fichiers/images et les dossiers
- Dans Klik, tous les objects (boutons, fenêtres) représentant des dossiers sont des "terrains d'atterrissage" pour le glisser-déposer

# Fonctionnement sûr #

- Dans Klik, toutes les actions peuvent être défaites (menu : Marque page & Historique / Défaire). La liste des choses à défaire est sauvée sur le disque, de sorte que vous pouvez revenir en arriere même après un crash inopiné.    
- Les fichiers éffacés sont en fait seulement deplacés dans la corbeille de Klik (qui est indépendante de celle de la plateforme)
- Quand on déplace un fichier, si un fichier du même nom existe dans le dossier de destination, Klik le renomme en ajoutant un suffixe numérique ... à moins que le fichier ne soit exactement identique.
- Le code source de klikr est en open-source sur github, une garantie que des experts peuvent relire le code afin de vérifier qu'il ne contient aucune partie malicieuse.

# 2 types de fenêtre #

- Fenêtre de navigation (1 fenêtre = 1 dossier)
- Fenêtre de visualisation (1 fenêtre = 1 image)

# De nombreux réglages #

Vous pouvez choisir :
- La taille et la position des fenêtres, et Klik s'en souvient, même si vous avez plusieurs écrans
- La langue (allemand, anglais, breton, chinois, espagnol, français, italien, japonais, coréen, portugais)
- Le style du thème (clair ou sombre, expérimental : thème 'bois')
- La taille des polices
- La taille des icônes
- La taille des colones
- Afficher pour les dossiers le nombre total de fichiers, y compris dans les sous-dossiers 
- Afficher pour les dossiers la taille sur le disque
- Si les fichiers/dossiers invisibles sont visibles ou non
- Si les boutons qui représentent les dossiers ont une icône animée qui échantillonne les images dans le dossier
- L'ordre des items dans la fenêtre de navigation, qui est aussi l'ordre des items dans la fenêtre de visualisation (choix : nom des fichiers, date de création, taille sur le disque, largeur des images, hauteur des images, rapport hauteur/largeur des images, et, par IA : similarité des images ! voir plus bas)
- Afficher les ressources consommées en temps réel (fils d'exécution et consommation mémoire vive)

# Les bonnes surprises  #

- Klik a une application pour jouer vos fichiers de musique (mp3 etc) avec des listes de lectures (si vous en avez marre de vous faire harceler/racketter par qui vous savez)
- Les fenêtres de visualisation ont un mode diapositive, un mode plein écran, un mode (1 pixel = 1 pixel) et un mode zoom
- Il y a de nombreux accélérateurs clavier (mais c'est pas super bien documenté)
- Les fenêtres de navigation ont un mode diapositive
- Mettez des gommettes de couleur pour les dossiers !
- Klik possède un service ultra-rapide de recherche par mot clés dans les noms des fichiers (tapez "k")
- Klik sait générer une planche-contact PDF des images dans un dossier
- Klik peut importer (retrouver et faire une copie) les photos que ApplePhoto planque sur votre disque dur :-)
- Klik peut ranger vos photos dans des dossiers annuels (1 année = 1 dossier)
- Klik peut vous aider à identifier les fichiers en doubles (fichiers identiques)
- Klik peut vous aider à identifier les images similaires (fichiers non identiques) par ML
- Klik peut visualiser les dossiers cachés, et vous indique les liens symboliques

# Services IA #

(Si votre machine est assez puissante)

Klik dispose de plusieurs services basés sur des réseaux de neurones pour les images
- Similarité d'images pour tout le dossier : trier le dossier par similarité d'images, les images qui se ressemblent apparaissent côte à côte.
- Similarité d'images à la demande : pour une image dans un dossier, obtenir les 5 images les plus proches dans le même dossier
- Deduplication par similarité : pour les images dans un dossier, itérez sur les images qui se ressemblent (mais le fichier n'est pas forcément identique) avec l'option d'effacer la version qui ne vous plait pas. (exemple sympathique : ce service détecte les images en miroir !!)
- Reconnaissance de visages. Pour des raisons légales, aucun modèle n'est fourni, donc ce service nécessite une étape d'apprentissage *supervisé* pour laquelle vous devez *fournir les données avec les étiquettes* , c'est donc du boulot... mais ça marche vraiment bien !

Tous utilisent du code et des modèles Python open-source

La reconnaissance faciale et la similarité d'images reposent sur des serveurs HTTP locaux qui peuvent être interrogés par l'application klikr depuis Java, ce qui signifie que les serveurs survivent aux redémarrages de l'application klikr.
(Note : il y a plusieurs serveurs sur plusieurs ports TCP car Python ne supporte pas le multithreading (enfin... pas aussi bien que Java !) et le serveur HTTP Python ne gère pas très bien la mise en file d'attente, donc le code Java de klikr va 'équilibrer la charge' des requêtes vers les serveurs.)

AVERTISSEMENT IMPORTANT : sans matériel approprié, ceci n'est probablement pas utilisable.
Pour la vitesse du ML, vous avez besoin d'une machine avec une carte graphique qui peut accéder à suffisamment de RAM...
(les machines à mémoire unifiée comme un MacBook ARM fonctionnent bien). De plus, ces services ML utilisent beaucoup de caches en RAM et sur disque (sinon ils seraient horriblement lents), donc vous avez besoin d'une machine avec suffisamment des deux. En d'autres termes, si vous utilisez klikr sur une petite/vieille machine sans GPU supportant keras et/ou sans assez de RAM (disons moins de 16GB) et d'espace disque (plusieurs GB), n'utilisez simplement pas ces fonctionnalités car elles seront lentes et/ou risquent de faire planter l'application (ou même la machine dans les cas extrêmes !).

### Installation ###

Utilisez le menu "plus de préférences".

# Services optionnels avancés et expérimentaux #

Le menu 'préférences' vous donne accès a des services expérimentaux comme:
- Un moteur de sauvegarde "qui n'éfface jamais rien" (pour ceux qui veulent garder les anciennes versions de leurs fichiers)
- Un moteur d'offuscation des fichiers (pour les paranos du cloud: protégez vos fichiers avec un pin-code)
- Des étiquettes sur les fichiers ("tags") qui peuvent servir de filtres avec 4 types :  qui, categorie, type, qualité

# Appel à suggestion et signalement des problèmes #

Si vous avez des problèmes, des bogues, des suggestions, des idées de nouveaux services, des corrections pour l'orthographe d'un item de menu, contactez moi !

### Recommendations pour signaler des bogues ###


- Dans le menu des préférences: 

(a) Activez les logs en fichier 

(b) Relancez Klik 

(c) Les fichiers de log sont des fichier nommés "Klik_application_xxxxx.txt" où 'xxxx' contient la date et l'heure, ils sont dans la poubelle de Klik (appuyez sur la poubelle en haut à droite pour visiter ce répertoire)

(Si vous avez démarer Klik depuis le code source, les logs s'écrivent dans le terminal)

- Essayez de reproduire le bogue

- Décrivez ce que vous étiez en train de faire

- Faites des copies d'écran 

Merci par avance.
