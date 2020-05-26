# Setup for and build spark-inequality-impact

Revised 2020 May 25.

Tested on lubuntu 18.10 on ThinkPad W530, i7-3720QM @ 2.6 GHz, 4 cores, 24GB,<br>
and on MacBook Pro, 2.4 GHz 8-core intel core i9, 32 GB, OS 10.15.2 and 10.15.4.

1. Ensure you have curl available (to test, we installed 7.61.0 using the system<br>
   package manager muon under lubuntu; curl was already installed on the MacBook Pro).

2. Install [SDKMAN](https://sdkman.io/install) to allow easier installation and removal of some of the prerequisites.<br>
   (We installed SDKMAN 5.7.4+362). It allows installation and use of multiple<br>
   versions of java, scala, gradle, and spark.  Currently the instructions are to<br>
   open a terminal window and run
```bash
        curl -s "https://get.sdkman.io" | bash
        source "$HOME/.sdkman/bin/sdkman-init.sh"
```
   To allow connecting to the internet via home wireless, it sufficed<br>
   (may not all be necessary) to edit
```bash
        $HOME/.sdkman/etc/config
```
   setting
```bash
        sdkman_curl_connect_timeout=30
        bsdkman_curl_max_time=50
```
   then close the terminal window and open another to use.  Config change may<br>
   not be necessary when installing on the MacBook Pro on a corporate network.

3. Ensure that you have jdk 1.8 by using the command
```bash
        java -version
```
   If it doesn't give something starting with 1.8, use sdkman to install it. Run
```bash
        sdk list java
```
   and choose a version at least 8.0.121 to install, via e.g.
```bash
        sdk install java 8.0.242-librca
```
4. To check whether proper versions of gradle, scala, and spark are installed, run
```bash
        gradle -version
        scala -version
        spark-submit --version
```
   If not, run the following (higher versions may also work).  Gradle is only<br>
   needed to build the software in step 6 or to produce scaladoc as in step 7,<br>
   not simply to use the included jar file.
```bash
        sdk install gradle 5.2.1
        sdk install scala 2.11.8
        sdk install spark 2.3.0
```
5. Choose the parent directory for the spark-inequality-impact directory and cd<br>
   to it.  Then if you haven't already made a local copy of the repo, resulting<br>
   in the spark-inequality-impact directory, either

   (a) use the git clone command given in the repo, or

   (b) use the Download tgz link, put the tgz file in the parent directory, then
```bash
        tar xzvf spark-inequality-impact-master.tgz
```
   In any case, for the sake of subsequent steps or to run the README examples
```bash
        cd spark-inequality-impact
```
6. The spark-inequality-impact.jar file containing the compiled software is in<br>
   your local copy of the repo to use as is.  Optionally, to build it, run
```bash
        gradle build
```
   This should build and test the code, including running under spark locally.<br>
   It should say BUILD SUCCESSFUL and the runtime (before this there may be<br>
   some test-expected error messages to stdout and stderr).  Test results will<br>
   be at `./spark-inequality-impact/build/reports/tests/test/index.html`.

7. To build the scaladoc, run
```bash
        gradle scaladoc
```
   It will be at `./spark-inequality-impact/build/docs/scaladoc/index.html`.

8. To undo the prerequisite installations, you can use sdk uninstall, the<br>
   same commands as to install but substitute uninstall.  There are ways to<br>
   switch between versions; use sdk help and the web for command explanations.
