library(dplyr)

subsectorMappingTravTime <- read.delim("test/output/femproto/demand/SubSectorsToPopulationTest/readSubSectorsShapeFile/subsectorMappingTravTime.txt")

subsectorMappingTravTime %>% 
  group_by(safenode) %>% 
  arrange(traveltime) %>% 
  mutate(offset = 1:n()) %>% 
  write.csv(row.names = F,file = "test/output/femproto/demand/SubSectorsToPopulationTest/readSubSectorsShapeFile/subsectorMappingTravTimeRanked.csv")

library(foreign)
subsectors <- read.dbf("data/20180706_DPavey_TestInputFiles/inputFilesScenario1A/hn_evacuationmodel_PL2016_V12subsectorsVehic2016.dbf")
library(readr)
X2016_safe_node_priorities_rev12 <- read_csv("data/20180706_DPavey_TestInputFiles/inputFilesScenario1A/2016_safe_node_priorities_rev12.csv")
View(X2016_safe_node_priorities_rev12)
subsectors %>% 
  left_join(X2016_safe_node_priorities_rev12) %>% 
  mutate(
    EVAC_NODE = as.integer(EVAC_NODE)
    ,n1 = as.integer(n1)
    ,n2 = as.integer(n2)
    ,n3 = as.integer(n3)
    ,n4 = as.integer(n4)
    ,n5 = as.integer(n5)
  ) %>% 
  select(SUBSECTOR=Subsector
         ,EVAC_NODE
         ,SAFE_NODE1=n1
         ,SAFE_NODE2=n2
         ,SAFE_NODE3=n3
         ,SAFE_NODE4=n4
         ,SAFE_NODE5=n5
         ) %>% 
  arrange(SUBSECTOR) %>% 
  write.table(file='test/input/femproto/prepare/demand/2026_scenario_2C_v20180706/2026_subsectors_safe_node_mapping.txt', 
              col.names = T, row.names = F, sep=";",quote = F,na = "")
