# Model assumptions

## Mapping of agents to evacuation links
- currently, evac nodes are on centroids, no physical reality where traffic enters the network on a 20k veh/hr capacity link
- matsim defaults is to map agents to nearest link from their coordinate
- here we assign them directly to a link
- this might produce incorrect behaviour as centroid links will enjoy preference due to high capacity
- for now we will assume that they can evacuate from these centroid links, but if this produces anomalous behaviour, suggest mapping them to actual road links, either from a random coordinate in the subsector, or probabilistically assigned coordinate taking land use and building stock into account.
- **question:** how do pax evacuate in reality? do they sign out at evac node?
