This run is fem-v1.1 and we thus deprecated.  For completeness, still the following information:
* Output (evidently) is in `matsim output`.  It looks like the run was made with a network in WGS84 coordinates.  Presumably, MATSim went through, although, as we know, one should be careful with that.  VIA, however, is not able to visualize the network with these coordinates.
* In order to address this issue, Pieter has written a converter that converts the network.  The result is under
```
matsim input
-->
hn_pop80_2011_safe5_netcf_flood1867-i1.0_0_1430446683628
-->
network-hn_pop80_2011_safe5_netcf_flood1867-i1.0.xy.xml
```
Fortunately, VIA computes the coordinates from the events internally, so one can just apply VIA to the output events plus that transformed network.

Some observations with respect to the run:
* There are fewer safe nodes than in fem-v1.2, and they are "farther out".  
* The evacuation is done after 9 hrs.  This is much faster than what comes out of v1.2.
* There seems to be a departure staging.
* The routing does _not_ only use the evacuation links but also others.

In the end, as stated, presumably not a particularly important data point.
