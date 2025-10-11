# This documents using 'the mill build tool' a.k.a. 'mill' to build klik

dowload the mill build tool from 

https://mill-build.org

assuming you have the mill executable at:

../mill

run with a JVM24  (see details in build.mill)

../mill run

make an executable jar:

../mill show assembly

output is :

/Users/dqssdqss/Kode/java/klik/out/assembly.dest/out.jar

and this file is executable!

both WORKS! (but only for klik, not for launcher and audio-player)

Caveat: VIPS FFM does not work

# Now trying to build a native image

in build.mill change the commented top line:

old:

object `package` extends PublishModule with MavenModule {
//object `package` extends JavaModule with NativeImageModule {

new:

//object `package` extends PublishModule with MavenModule {
object `package` extends JavaModule with NativeImageModule {

../mill run
or
../mill show assembly

... almost works, but not quite:
resources are not copied to the native image, 
so it does work, but there is no CSS styling and no icons