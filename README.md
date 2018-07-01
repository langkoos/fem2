# FEM 2.0

- [Movies](#movies)
- [Run Via](#via)
- [Run MATSim](#runmatsim)
- [Clone repo](#runmatsim)
- [Setting up a post-commit hook to rsync to Dropbox](#commithook)

### Simple things

##### Movies <a name="movies"></a>

1. Go into the `scenarios` directory.  
1. Decide for a scenario that you find interesting (e.g. `fem2016`) and go into that directory.
1. Inside there, look for an `output-*` directory that you find interesting and go into that directory.
1. Inside there, look for `movie-*` files.  You can't view them directly, but you there are various ways to download them, and you can view them then.  Try that.

##### Run VIA on output files  <a name="via"></a>

1. Do steps 1.-3. above.
1. Download `output_network.xml.gz` and `output_events.xml.gz`.  Best make sure that they do not uncompress, e.g. by "Download linked file as ...".
1. Get these files into VIA.  This can be achieved in various ways; one is to open VIA and then drag the files from a file browser into VIA.
1. Run VIA and enjoy.

##### Run MATSim <a name="runmatsim"></a>

1. There should be a file directly in the `fem` directory with name approximately as `au-flood-evacuation-0.11.0-SNAPSHOT-jar-with-dependencies.jar`.
1. Double-click on that file (in a file system browser).  A simple GUI should open.
1. In the GUI, click on the "Choose" button for configuration file.  Navigate, e.g., to `scenario/fem2016` and load one of the configuration files (e.g. `configSmall.xml`).
1. Press the "Start MATSim" button.  This should run MATSim.
1. "Open" the output directory.  You can drag files into VIA as was already done above.

1. "Edit..." (in the GUI) the config file.  Re-run MATSim.



### Developer information

#### Cloning the repository and running MATSim <a name="clonerepo"></a>

This is a complicated process requiring registration with TU Berlin. Probably redundant, now that we have the latest version of the code pushed to Dropbox.

##### Setting up a post-commit hook to rsync to Dropbox <a name="commithook"></a>

This assumes you run Linux or MacOS - probably will work on cygwin too.

1. Open `clone_to_dropbox.sh` in the repository root.
1. Change the path to the shared `FEM2_SNAPSHIT` subdir in your Dropbox folder.
1. Copy this file to your git hooks directory and make executable:

```
cp clone_to_dropbox.sh .git/hooks/post-commit
chmod +x .git/hooks/post-commit
```

Now it will run every time you commit, and sync to Dropbox.
