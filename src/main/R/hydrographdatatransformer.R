library(dplyr)
library(foreign)

subsectors <- read.dbf('scenarios/EIS_May2019/ScenarioA0/FEM2_Subsectorvehicles_A0_2016.dbf')

links <- read.dbf('scenarios/EIS_May2019/ScenarioA0/FEM2_ScenarioA0_links.dbf')

linklookup <- read.delim("~/git/au-flood-evacuation/scenarios/EIS_May2019/ScenarioA0/linkLookup.txt")
sslookup <- read.delim("~/git/au-flood-evacuation/scenarios/EIS_May2019/ScenarioA0/SSLookup.txt")

subsectors %>% 
  left_join(sslookup) %>% 
  write.dbf('scenarios/EIS_May2019/ScenarioA0/FEM2_Subsectorvehicles_A0_2016.dbf',factor2char = T,max_nchar = 254)

links %>% 
  left_join(linklookup) %>% 
  write.dbf('scenarios/EIS_May2019/ScenarioA0/FEM2_ScenarioA0_links.dbf',factor2char = T,max_nchar = 254)
  