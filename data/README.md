###Notes 2018-07-24
Received reviewed data files from David Pavey.
These are for 2 population scenarios and the same flooding conditions.

Files are stored in `2018-07-06_DPavey_TestInputFiles`.


### Notes 2018-01-24

Subsections evacuate to certain safe nodes.  Questions:
* Do we have them?  Are they just emme/2 nodes?

There are a lot of diverse input files.  Do we know what all of them mean?  Is there overlapping information?  Can Phil find this out?  Do we need to read in all of these files, or just some of them?

There is some 2041 network information in the 2011 directory.  Is that duplicate?  If so, can we delete it?

There is supposedly better/newer studies "flinders lakes" (?).  --> Where are those input files?

### Notes

Current suspicions, with corroborating evidence:
* The "hydrograph points" are height points.  They give the terrain height in meters above sea level at that point.  The file `wma_hydrograph_points/wma_hydrograph_points.shp` gives that information.  WMA is, I think, just the name of a consultancy.
* This can be verified by looking at terrain height in google earth at these locations.
* The file `hydrographs_data_example/d04711_H14.xlsx` presumably gives the time series of water level at each hydrograph point.  The hydrograph point may be higher than the water level, in which case there is (presumably) no flooding.
* Points 311, 312, 315 are close to the river (= Eastern Creek) and thus flood earlier.  (I have color-coded the xlsx to demonstrate this.)
* Points 313 and 314 are somewhat away from the river and thus flood later.  They also do not flood to the same height as 311, 312, 315, since when the water has reached 313/314, the water near the river is already going down.

What we seem to have:
* About 200 hydrograph points.
* We probably have the mapping of those points onto the road network (see `../2011_evacuation_network/ wma_ref_points_1_to_1522_links_and_nodes_penrith_lakes.shp`). I find this overly heavyweight, see below.



What we don't have:
* All hydrograph points.  I think it says somewhere that there should be about 1500.  We only have about 200.
* Flooding time series.  We only have a small sample.
* Translation of the emme network into MATSim.  The emme data comes with the usual DATA1, DATA2, DATA3 fields, and one can only speculate how these correspond to freespeed, capacity, ... .  See `RunNetworkEmme2MatsimExample` in `matsim-code-examples`.

Thoughts:
* I find a direct mapping of hydrograph points into the network overly heavyweight.  It means that replacing the network is a major effort.  A shape file with flooding boundaries would be much easier to handle.  One could, potentially, derive a shape file from the hydrograph points plus the flood level time series.  We would, however, have to make sure that a flood that covers part of a link but neither of the two nodes will be caught.
* Given that also the translation of the emme network is not clear, I would probably have a tendency to also construct a network based on OSM.  If one also had the flooding as shape file, then one could at least run lightweight comparison simulations.  But maybe this is too much extra work.

### Conversion of points to polygons

This here http://desktop.arcgis.com/en/arcmap/latest/extensions/production-mapping/converting-points-to-lines-or-polygons.htm is arcgis, but it points to "convex hull".  Feels pretty straightforward: identify all hydrograph points which are below flood level, and do convex hull in geotools.  (Probably returns multiple polygons, as it should.) -- However, interpolation would be better.

Search for something like "contour lines from points", maybe plus "geotools".

Looks like
* http://docs.geotools.org/latest/javadocs/org/geotools/process/vector/BarnesSurfaceProcess.html has process to convert irregular data points into regular grid, and
* http://docs.geotools.org/latest/javadocs/org/geotools/process/raster/ContourProcess.html converts the regular grid into contours
