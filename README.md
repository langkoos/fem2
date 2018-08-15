# FEM 2.0

- [Movies](#movies)
- [Run Via](#via)
- [Run MATSim](#runmatsim)
- [Clone repo](#clonerepo)
- [Setting up a post-commit hook to rsync to Dropbox](#commithook)

### Things try when you are accessing this project via the Dropbox clone

##### Movies <a name="movies"></a>

1. Go into the `scenarios` directory.  
1. Decide for a scenario that you find interesting (e.g. `fem2016_v20180307`) and go into that directory.
1. Inside there, look for an `output-*` directory that you find interesting and go into that directory.
1. Inside there, look for `movie-*` files.  You should be able to watch them.

##### Run VIA on output files  <a name="via"></a>

1. Do steps 1.-3. above.
1. Locate `output_network.xml.gz` and `output_events.xml.gz`.  
1. Get these files into VIA.  This can be achieved in various ways; one is to open VIA and then drag the files from a file browser into VIA.
1. Run VIA and enjoy.

##### ... to be continued ...

<!-- ##### Run MATSim to optimize safe node assignments

1. There should be a file directly in the `FEM` base directory with name approximately as `au-flood-evacuation-0.11.0-SNAPSHOT-jar-with-dependencies.jar`.
1. Double-click on that file (in a file system browser).  A simple GUI should open.
1. In the GUI, click on the "Choose" button for configuration file.  Navigate to `scenario/fem2016_v20180307` and load `00config-just-run-plans-file.xml`.
1. Increase memory.  8000 MB is a good number, but less should do here as well.
1. Press the "Start MATSim" button.  This should run MATSim.
1. "Open" the output directory.  You can drag files into VIA as was already done above.
1. If you run again, you will have to delete the output directory first.  There is a button for this.

##### Use MATSim to optimize safe node assignments

1. Do all steps as in [Run MATSim](#runmatsim), but load `00config-optimize-safe-nodes-by-subsector.xml`.

The optimization algorithm that is used here is a stochastic heuristic. Small changes will lead to other results, often quite different.  Such changes can be in the code, but also in the random seed.

##### Edit the config files

The GUI allows to edit the config file.  It might be best to first make a copy of the original config file before trying this. -->




### Developer information

#### Cloning the repository and running MATSim <a name="clonerepo"></a>

This is a complicated process requiring registration with TU Berlin. Probably redundant, now that we have the latest version of the code pushed to Dropbox.

##### Setting up a post-commit hook to rsync to Dropbox <a name="commithook"></a>

This assumes you run Linux or MacOS - probably will work on cygwin too.

1. Open `clone_to_dropbox.sh` in the repository root.
1. Change the path to the shared `FEM2_SNAPSHOT` subdir in your Dropbox folder.
1. Copy this file to your git hooks directory and make executable:

```
cp clone_to_dropbox.sh .git/hooks/post-commit
chmod +x .git/hooks/post-commit
```

Now it will run every time you commit, and sync to Dropbox.
