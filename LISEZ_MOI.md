# Klik: un explorateur de fichers avec une forte orientation 'image' #

Trier vos fichiers, documents et images de manière intuitive et rapide. 
Ils sont représentés par des icônes que vous pouvez glisser-déposer d'une fenêtre vers une autre, chaque fenêtre correspondant à un dossier.  

[Installation pour MacOS](file:MacOS_fr.md)

# Services réseaux de neurones #

Klik dispose de 3 services différents basés sur des réseaux de neurones pour les images
- Reconnaissance de visage
- Similarité d'images pour tout le dossier : trier le dossier par similarité d'images, les images qui se ressemblent apparaissent côte à côte.
- Similarité d'images à la demande : pour une image dans un dossier, obtenir les 5 images les plus proches dans le même dossier

Tous utilisent du code et des modèles Python open-source (basés sur tensorflow/keras et opencv)

La reconnaissance faciale et la similarité d'images reposent sur des serveurs HTTP locaux qui peuvent être interrogés par l'application klik depuis Java, ce qui signifie que les serveurs survivent aux redémarrages de l'application klik.
(Note : il y a plusieurs serveurs sur plusieurs ports TCP car Python ne supporte pas le multithreading (enfin... pas aussi bien que Java !) et le serveur HTTP Python ne gère pas très bien la mise en file d'attente, donc le code Java de klik va 'équilibrer la charge' des requêtes vers les serveurs.)

AVERTISSEMENT IMPORTANT : sans matériel approprié, ceci n'est probablement pas utilisable.
Pour la vitesse du ML, vous avez besoin d'une machine avec une carte graphique qui peut accéder à suffisamment de RAM...
(les machines à mémoire unifiée comme un MacBook ARM fonctionnent bien). De plus, ces services ML utilisent beaucoup de caches en RAM et sur disque (sinon ils seraient horriblement lents), donc vous avez besoin d'une machine avec suffisamment des deux. En d'autres termes, si vous utilisez klik sur une petite/vieille machine sans GPU supportant keras et/ou sans assez de RAM (disons moins de 16GB) et d'espace disque (plusieurs GB), n'utilisez simplement pas ces fonctionnalités car elles seront lentes et/ou risquent de faire planter l'application (ou même la machine dans les cas extrêmes !).

# Installation #
1. UNE SEULE FOIS : installer tensorflow+keras enchilada

ATTENTION : l'installation de tensorflow peut être très fastidieuse... YMMV

Voici uniquement une recette qui exploite l'accélération matérielle (metal) pour les Macs ARM64 :
(fonctionne sur mon M