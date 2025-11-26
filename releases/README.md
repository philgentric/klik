
Klik has installers for:
- MacOS
- Windows11

This makes it easy for 'the usual user' to get Klik using familiar mechanisms. 

Caveats are:
- You have to wait for a new release to get bug fixes and new features
- You have to download the whole installer (more than 100MB).
- On MacOS, because Klik is not signed/notarized (yet), you have to visit the 'security' settings to enable Klik, everytime.

For the 'developer' or 'computer savvy' people, klik is available from source. Here are the major differences with the 'installer' method:
- Klik is 'recompiled' everytime you start it (well, only when the source code changed)
- This implies that to start klik you either:
- (a) type a gradle command (e.g. gradle klik), or 
- (b) click on a script that will do the same, or
- (c) use the launcher = use (a) or (b) to start it and then starting Klik is just a button.
- the big advantage is that getting updates, new features, and bug fixes is super fast and easy: git pull the last source and restart. (git pull is available from a klik or launcher menu)

Said otherwise, Klik is in 'full rolling releases mode', but if you use installers, you will not get all releases...

Klik version number is M.m.p (for example 1.0.123)
where M is the Major version, m is the minor version and p is the 'patch' version. 
The patch version is simply the number of commits to the main branch.
