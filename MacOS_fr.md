# Installation de klikr sur MacOS

**NOTE SUR LA SECURITE** 

1. Les commandes proposées dans cette recette d'installation sont des outils "standards" utilisés par des centaines de milliers de developpeurs dans le monde entier, le risque est très faible.
2. Klikr est développé avec soin et en particulier est conçu pour ne pas effacer de fichiers (sauf en vous demandant une confirmation), mais avec Klikr on peut déplacer un répertoire en un mouvement de souris et paniquer en pensant que les photos sont perdues. Il n'en est rien. Klikr a une fonction "défaire" qui se souvient de tout, même après un crash. Cependant, de la même façon que "les avions ne tombent jamais", personne ne peut exclure qu'une fausse-manip puisse causer des pertes de données : faites des sauvegardes!
3. Le fait que 100% du code source de klikr soit visible est une garantie : un expert peut aller voir et vérifier que le code ne contient aucune partie malicieuse.

## Installation 'classique'

Télécharger l'application d'installation 'dmg'


## Installation avec le code source

**Vous avez besoin de savoir comment ouvrir un terminal.**

Sur MacOS c'est une Application... qui s'appelle "Terminal" !-)

Par exemple: iterm2. (https://iterm2.com/downloads/stable/latest)

Dans le terminal, copier-coller les commandes en gras données dans les étapes suivantes:


### installez homebrew

**/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"**

### installez sdkman

**brew install sdkman**

### installez gradle

**sdk install gradle 9.2.0**

### installez java 25 avec javaFX

**sdk install java 25.fx-zulu**

### installez git

**brew install git**

### clonez le code source

**git clone https://github.com/philgentric/klikr.git**

cette opération va créer un dossier 'klikr' avec le code source. 

### Démarrez klikr!

**cd klikr**

**gradle klilr**

### Installez ffmpeg

(klikr utilise ffmpeg pour fabriquer les icones animées des vidéos)

Utilisez le bouton dans le menu "plus de préférences" ou bien:

**brew install ffmpeg**

### Installez graphicsmagick

(klikr utilise graphicsmagick pour fabriquer les icônes des fichiers PDF, les icônes animées des dossiers etc)


Utilisez le bouton dans le menu "plus de préférences" ou bien:

**brew install graphicsmagick**


### Recevez les dernières mises à jour

dans le dossier 'klikr', tapez:

**git pull**

Cette opération va mettre à jour le code source.

**IMPORTANT:**
Si après une mise à jour du code source vous avez une erreur au démarrage, revenez lire cette documentation. En effet, certaines mises à jour du code source nécessitent aussi la mise à jour des outils de compilation 'gradle' et 'java', cette documentation sera aussi mise à jour et vous indiquera les nouvelles lignes de commande.

Tentez:

**gradle clean run**

Ou alors, si klikr ne se lance plus, c'est peut-être que vous avez laissé un enfant (ou un chat ?) jouer avec votre ordi, et qu'il a mis le souk dans le code source de klikr ?

Au lieu de tout effacer et tout recommencer, faites :

**git stash**

**git pull**

# Foire Aux Questions

Q: Je fais "gradle run", mais klikr ne démarre pas ?

R: Vérifiez que vous êtes dans le dossier 'klikr' du code source de klikr. (le message d'erreur est : Directory 'xxxxx' does not contain a Gradle build.)

Q: Je n'ai plus de place sur mon disque.

R: Allez dans le menu Préférences et effacez les caches. Visitez la poubelle pour vérifier que ce qu'elle contient peut partir au paradis des bits avant de faire "vider la poubelle".

Q: Quand klikr tourne, les autres applications manquent de RAM.

R: (Vraiment?) Allez dans le menu Préférences et effacez les caches. Pour les experts: Editez build.gradle pour réduire la taille mémoire max de la JVM. 

Q: Je veux désinstaller klikr.

R: (1) Effacez le dossier du code source (2) Effacez le dossier '.klikr' à la racine de votre espace disque utilisateur, il contient des caches et la poubelle, en général, cela donne beaucoup plus de place que le code.