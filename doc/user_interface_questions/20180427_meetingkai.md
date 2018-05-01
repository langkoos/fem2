1. confirm if we have consensus about network
2. departure scheduling: not optimised
3. stagoing: do we take nicta code -> d61, or o it in matsim?
4. sort out the deadlines for differernt dta 
5. Is there always a single priority node as per the description of the algorithm in the nicta report?
6. clarify procedure for mapping agents to safe nodes
   - evacuate to the first safe node then next?
   - do we decide the best for them?
   c. do we minimise the evacuation time?
   d. if we cant evac one subsector to the same node?
   e. how much optimisation do they want from the algorithm - some manual intervention beyond safe node priority mapping?
7. report not very clear which combination of classes were used - can't say which to extract for fem 2
8. talk to peter about this offline?
9. ask david pavey who ran v1.2 sims?
   - is this what the client really wants?
   - define for each subsector a safe node and evacuation time and run?
   - have optmization as separate software run, then bring back instructions into simulation, and execute
10. PRIORITY: get routing right, check evac to safe node mappings
       why do agents i the south evacuate back towards the river? tis the bridge there high enough to warrant this in the plan
       is it ok to leave designated evacuation links for short stretches if it is useful?
       so ability to mark intersections on highways as evacuation links when necessary
11. what is the status of hosting files outwide australia, e.g. githb? becasue we can use git-lfs
12. set up a private repo
    - everybody is a developer
    - data61 has github implemntation but not cclear if there is lfs, and the 
    - is it ok if peter cinque signs up on github
    - how to deal with git lfs
    