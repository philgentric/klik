
# To do list #

# Check that the translation are ok #

I can check only english and french : native speakers, please help!

# bug fixes #

known bugs:
- race condition in browser? (rare)
- search results becomes super slow when there are a lot of results

# going native with grallvm #

- done
- the start time is small (start times: native < java -jar < gradle start)
- the run-time speed benefit looks small (difficult to benchmark)
- some features are lost like VIPS FFM does not work

# Installers #

You have installers for macOS (.dmg) and Windows (.msi)

Note the dmg is not signed so you will have the usual hiccup from macOS, until you authorise Klikr in the security settings.

Otherwise, from source, Klikr is in rolling releases...

# Aesthetic: Better look, more styles, better icons better layouts#

I need help with that !
Icons : help me find/make better AND free icons
CSS : the javafx CCS format is close to the web one,
so a web CSS expert should be able to make meaningfully changes/additions ...
we can create as many styles as we want, and users can choose the one they like best.

# more i18n #

- for i18n new strings translation is not a problem with the side projet: translator
- translate the README.md in more languages: why not?
- there are still a few strings that are not i18n in the code

# minor: make "sort by" folder-specific #

Easy to do: just record the user choice for each visited folder...
could become weird like if you dont remember the last choice...

I think it should be a "pin it" choice: user chooses which folders remember their sorting method?

# Support for more image formats #

right ! which ones ?

Klik already supports a lot of formats via:
- JavaFX Image "native" (jpeg, png, bmp, gif, animated gif)
- GraphicsMagick (92 image formats) via conversion to png
  (deprecated: - FITS via the nasa java lib)

Try supporting more formats via Java Advanced Imaging?

Try faster jpeg decoding like twelveMonkey?

# Support for more video formats #

webm is an obvious target..

# Support for more audio formats #

Opus?

# Allow more fonts especially super legible ones #

Klikr already allows large fonts... and uses Atkinson Hyperlegible

# implement AI-based voice-over for menus... and images! ??

Let the OS do that ?

# implement tree-based search on image similarity #

Target : Vantage Point Tree (CTRBF that I published a long time ago ?-)

Reason: when similarity search is faster, it can be used to look for similar images not only in the current folder but also in whole collections (recusrsively down the directiry tree) (like the binary-exact duplicate finder)


# implement a tool for managing translations #

today, if translations for an item are wrong (e.g. the reference item in English changed), one has to find-in-all-files and manually delete all entries in the ressource files...

with this tool, one could erase all entries for a given key, and then call 'translator' to re-translate it.

# debug live #

In the code there are many debug booleans, they trigger additional traces, possibly a lot of them.

They are all declared as 'static final', which has a huge advantage: the compiler optimizes the code and removes all the debug code that is not used (i.e. when the flag is false).

(probably, if all debug booleans were set to true, the code would be much slower, but I have not tested that)

But it could be useful if, when someone has a problem, the UI could be used to set dbg =true, with a few choices about the scope?
And logging to file, then the --user just has to copy/paste the log file in a discord channel.

Because the booleans are static final, this is only possible by editing the code, and recompiling it.
(The recompiling part is now transparent with the launcher: it calls gradle, so the code, if changed, will be recompiled)

But the editing part is a bit tricky: the java app would edit the source code?
Is it safer to first create a git branch?
