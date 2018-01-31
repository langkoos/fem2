# Review of NICTA input files
_Meeting with David Pavey, Kam, Kai, Pieter_

23 Jan 2018

## Input data
* David saved the latest version of the input files before processing by NICTA
* Will pass these along on Dropbox and Kai will share on gitlab
* Departure times areset by SES based on how long they estimate it will take to evacuate
* Departure time may have been triggered in the plans file
* Time series in CSV format of the hydrographs
* We will use Kai's conversion of hydrograph data into time series shape files
* David thinks current hydro point heights are relative to bridge heights or cut-off heights of bridges
* Java geotools operations need to be explored

## Basic requirements spec
* 2041 network was passed by John Hart and should be as good as RMS true network planned is
* No turn restrictions in EMME network
* Evacuation rate between 454 and 600 veh/hr/ln
* Max speed is documented in NICTA report
* SES is the flooding and storm services - Peter Cinque
* RMS is strategic road planning at regional level - John Hart
* Departure times from hydrographs and evac routes ten hours before unevacuateable
* Subsectors are big enough to knock on doors in hour to evac 6000 pax in 10 hr
* Subsectors are population or terrain-based
* Model may prompt road upgrades down the line, so iterative feedback
* Network has centroid connectors already in shapefile

Runs database mytardis is potential solution for this project

Recruit students in Singapore or Berlin to do conversion

Which combinations of population an dnetwork will be infeasible to evacuate in 15hr
