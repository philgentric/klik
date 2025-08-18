
# To do list #

# Check that the translation are ok #

I cannot check, only  english and french : native speakers, please help!

# bug fixes #

- race condition in browser? 
- search results becomes super slow when there are a lot of results

# going native with grallvm #

- there are difficulties with graadle+javafx+graalvm
  (that I managed to overcome once but I lost it when I performed a java upgrade...)

# Installers #

Yes, that would make it easier for the average granma user...
But installers are really a pain, actually I have never managed to create 
and test well enough 3 installers (Mac, Windaube, Linux)
And klik is in rolling releases...

# Aesthetic: Better look, more styles, better icons better layouts#

I need help with that !
Icons : help me find free icons
Styles : the javafx CCS format is close to the web one, 
so a web CSS expert should be able to make meaningfully changes/additions ...
we cab create as many styles as we want, and users can choose the one they like best.

# more i18n #

- for i18n new strings translation is not a problem with the side projet: translator
- translate the README.md in more languages: why not? (free LLMs are a bit limited)
- there are a few strings that are not i18n in the code

# make "sort by" folder-specific #

Easy to do: just record the user choice for each visited folder...
could become weird like if you dont remember the last choice...

# Support for more image formats #

right ! which ones ? 

webp?

FITS? 
https://en.wikipedia.org/wiki/FITS
https://imagej.net/libs/scifio

Try supporting more formats via Java Advanced Imaging

Try faster jpeg decoding like twelveMonkey

Try better down-scaling filters (not needed on mac screens, it is built-in) for example with imgscalr

# Support for more video formats #

webm is the obvious target..

# Support for more audio formats #

Opus?


# Allow more fonts especially super legible ones #

klik already allows large fonts...

# implement AI-based voice-over for menus... and images! #

Use AI to produce explanation when the n-mouse is over ...

- a menu item: super easy use AI Test-To-Speech
- an image: use AI to produce a description of the image and then use AI Text-To-Speech


# implement tree-based search on image similarity #

Target : Vantage Point Tree (CTRBF that I published a long time ago ?-)

Reason: when similarity search is faster, it can be used to look for similar images not only in the current folder but also in whole collections (recusrsively down the directiry tree) (like the binary-exact duplicate finder)


# implement a tool for managing translations #

today, if translations for an item are wrong (e.g. the reference item in English changed), one has to find-in-all-files and manually delete all entries in the ressource files...

with this tool, one could erase all entries for a given key, and then call 'translator' to re-translate it.

# debug live #

In the code there are many debug booleans, they trigger additonal traces, possibly a lot of them.

They are all declared as 'static final', which has a huge advantage: the compiler optimizes the code and removes all the debug code that is not used (i.e. when the flag is false).

(probably, if all debug booleans were set to true, the code would be much slower, but I have not tested that)

But it could be useful if, when someone has a problem, the UI coud be used to set dbg =true, with a few choices about the scope?
And logging to file, then the --user just has to copy/paste the log file in a discord channel.

Because the booleans are static final, this is only possible by editing the code, and recompiling it.
(The recompiling part is now transparent with the launcher: it calls gradle, so the code, if changed, will be recompiled)

But the editing part is a bit tricky: the java app would edit the source code?
Is it safer to first create a git branch?




