# Installation de klikr pour Windows

**NOTE SUR LA SECURITE** 

1. Les commandes proposées dans cette recette d'installation sont des outils "standards" utilisés par des centaines de milliers de developpeurs dans le monde entier, le risque est très faible.
2. Klik est développé avec soin et en particulier est conçu pour ne pas effacer de fichiers (sauf en vous demandant une confirmation), mais avec klikr on peut déplacer un répertoire en un mouvement de souris et paniquer en pensant que les photos sont perdues. Il n'en est rien. Klik a une fonction "défaire" qui se souvient de tout, même après un crash. Cependant, de la même façon que "les avions ne tombent jamais", personne ne peut exclure qu'une fausse-manip puisse causer des pertes de données : faites des sauvegardes!
3. Le fait que 100% du code source de klikr soit visible est une garantie: un expert peut aller voir et vérifier que le code ne contient aucune partie malicieuse.

## Installation

**Vous avez besoin de savoir comment ouvrir un terminal.**

Sur Window c'est une Application... qui s'appelle "Power shell" 

Dans le terminal, copier-coller les commandes en gras données dans les étapes suivantes:

### installez WSL2

Avec windows 10 2004 ou plus:

**wsl --install**

Avec une version plus ancienne: powershell clicker à droite, choisissez "exécuter en tant qu'administrateur" ("Invite de commande avec droits administrateur")

**dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart**

(si votre version de windows est plus ancienne que windows 10 2004 ... désolé, ... google sera votre ami)

ensuite:

### installez sdkman

(sdkman est un outil de gestion de version des outils de base, il donne accès à la commande "sdk", qui est super pratique : plus de casse-tête à chercher sur internet où est telle ou telle version du JDK java ou de gradle, sdkman s'occupe de tout!)

**https://sdkman.io/install/#windows-installation**


### installez gradle

(gradle est un outil de compilation)

**sdk install gradle 9.0.0**

### installez java 24 (avec javaFX) 

(le Java Development Kit (JDK) est l'outil qui permet de compiler et d'exécuter le code source java ; attention les versions antérieures à 23 de Java ne sont pas supportées par klikr)

**sdk install java 24.fx-zulu**

###  installez git

(git est un outil de gestion de version du code source)

**https://git-scm.com/downloads/win**

### clonez le code source

**git clone https://github.com/philgentric/klik.git**

cette opération va créer un dossier 'klikr' avec tout le code source. 

### Démarrez klikr!

**cd klikr**

**gradle run**


Note: en alternative à utiliser gradle, vous pouvez utiliser jbang:

**sdk install jbang**

**jbang src/main/java/klikr/Klik_application.java**

### plus tard ... Recevez les dernières mises à jour

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

R: Allez dans le menu Préférences et (1) effacez les caches (2) réduisez la taille mémoire max de la JVM. 

Q: Je veux desinstaller klikr.

R: (1) Effacez le dossier du code source (2) Effacez le dossier '.klikr' à la racine de votre espace disque utilisateur, il contient les paramètres, des caches et la poubelle, presque toujours cela donne beaucoup plus de place que le code.