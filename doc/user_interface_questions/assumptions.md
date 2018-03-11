# Model assumptions

## Mapping of agents to evacuation links
- currently, evac nodes are on centroids, no physical reality where traffic enters the network on a 20k veh/hr capacity link
- matsim defaults is to map agents to nearest link from their coordinate
- here we assign them directly to a link
- this might produce incorrect behaviour as centroid links will enjoy preference due to high capacity
- for now we will assume that they can evacuate from these centroid links, but if this produces anomalous behaviour, suggest mapping them to actual road links, 
  either from a random coordinate in the subsector, or probabilistically assigned coordinate taking land use and building stock into account.
- **question:** how do pax evacuate in reality? do they sign out at evac node? 
- **update:** from the meeting on 7 March, it became clear that we should, instead, use SES link flow rates, 
rather than EMME flow rates. The centroids connectors don't have unrealistically high SES flow rates.

## Mapping of agents to safe nodes
 - we currently assume agents all go to the same safe node, but there are multiple safe nodes for each subsector
 - FEM 1.0 looks like it has differnt safe nodes for differnet flooding scenarios, so we need to reconstruct if we are to match FEM1
 - if it's Java need to make it a replanning strategy
 - ask Dhirendra to investiagte  how it was implemented in v1.0
 - how does it work in reality; do door knockers tell people in the same subsector to go to different safe nodes?
    - answer (Peter Cinque): no, they are routed  at intersections such that they won't be sent to 
      safe nodes that ar already flooded
 
## Phase 2 behaviour
- What is the kind of behaviour we want from the simualtion
- do agents simply map

## Traffic accident modelling
- traffic safety factor was used in version 1
- we currently assume safety factor to be implicit in the SES flow rate, 
  but we might need to vary it as an input in sensitivity analysis.