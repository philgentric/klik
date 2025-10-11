# Installation de klik sur MacOS

**NOTE SUR LA SECURITE** 

1. Les commandes proposées dans cette recette d'installation sont des outils "standards" utilisés par des centaines de milliers de developpeurs dans le monde entier, le risque est très faible.
2. Klik est développé avec soin et en particulier est conçu pour ne pas effacer de fichiers (sauf en vous demandant une confirmation), mais avec klik on peut déplacer un répertoire en un mouvement de souris et paniquer en pensant que les photos sont perdues. Il n'en est rien. Klik a une fonction "défaire" qui se souvient de tout, même après un crash. Cependant, de la même façon que "les avions ne tombent jamais", personne ne peut exclure qu'une fausse-manip puisse causer des pertes de données : faites des sauvegardes!
3. Le fait que 100% du code source de klik soit visible est une garantie : un expert peut aller voir et vérifier que le code ne contient aucune partie malicieuse.

## Installation

**Vous avez besoin de savoir comment ouvrir un terminal.**

Sur MacOS c'est une Application... qui s'appelle "Terminal" !-)

Dans le terminal, copier-coller les commandes en gras données dans les étapes suivantes:


### installez homebrew

**/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"**

### installez sdkman

**brew install sdkman**

### installez gradle

**sdk install gradle 9.0.0**

### installez java 24 (avec javaFX) 

**sdk install java 24.fx-zulu**

### installez git

**brew install git**

### obtenez le code source (128MB)

**git clone https://github.com/philgentric/klik.git**

cette opération va créer un dossier 'klik' avec tout le code source. 

### Démarrez klik!

**cd klik**

**gradle run**

### Installez ffmpeg

(klik utilise ffmpeg pour fabriquer les icones animées des vidéos)

**brew install ffmpeg**

### Installez graphicsmagick

(klik utilise graphicsmagick pour fabriquer les icônes des fichiers PDF, les icônes animées des dossiers etc)

**brew install graphicsmagick**



Note: en alternative à utiliser gradle, vous pouvez utiliser jbang:

**sdk install jbang**

**jbang src/main/java/klik/Klik_application.java**

### Plus tard ... Recevez les dernières mises à jour

dans le dossier 'klik', tapez:

**git pull**

Cette opération va mettre à jour le code source.

**IMPORTANT:**
Si après une mise à jour du code source vous avez une erreur au démarrage, revenez lire cette documentation. En effet, certaines mises à jour du code source nécessitent aussi la mise à jour des outils de compilation 'gradle' et 'java', cette documentation sera aussi mise à jour et vous indiquera les nouvelles lignes de commande.

Tentez:

**gradle clean run**

Ou alors, si klik ne se lance plus, c'est peut-être que vous avez laissé un enfant (ou un chat ?) jouer avec votre ordi, et qu'il a mis le souk dans le code source de klik ?

Au lieu de tout effacer et tout recommencer, faites :

**git stash**

**git pull**

# Foire Aux Questions

Q: Je fais "gradle run", mais klik ne démarre pas ?

R: Vérifiez que vous êtes dans le dossier 'klik' du code source de klik. (le message d'erreur est : Directory 'xxxxx' does not contain a Gradle build.)

Q: Je n'ai plus de place sur mon disque.

R: Allez dans le menu Préférences et effacez les caches. Visitez la poubelle pour vérifier que ce qu'elle contient peut partir au paradis des bits avant de faire "vider la poubelle".

Q: Quand klik tourne, les autres applications manquent de RAM.

R: (Vraiment?) Allez dans le menu Préférences et effacez les caches. Pour les experts: Editez build.gradle pour réduire la taille mémoire max de la JVM. 

Q: Je veux désinstaller klik.

R: (1) Effacez le dossier du code source (2) Effacez le dossier '.klik' à la racine de votre espace disque utilisateur, il contient des caches et la poubelle, en général, cela donne beaucoup plus de place que le code.