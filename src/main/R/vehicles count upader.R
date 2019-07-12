library(dplyr)
library(foreign)

a0 <- read.dbf('scenarios/FEM2TestDataOctober18/ScenarioA0/FEM2_Subsectorvehicles_A0_2016.dbf')
a1 <- read.dbf('scenarios/FEM2TestDataOctober18/ScenarioA1/FEM2_Subsectorvehicles_A1_2016.dbf')
a2 <- read.dbf('scenarios/FEM2TestDataOctober18/ScenarioA2/FEM2_Subsectorvehicles_A2_2016.dbf')

combined <- 
  a0 %>% 
  select(SECTOR, SUBSECTOR, TOTAL_VEH) %>% 
  mutate(scenario = 'A0') %>% 
  rbind(
    a1 %>% 
  select(SECTOR, SUBSECTOR, TOTAL_VEH)  %>% 
    mutate(scenario = 'A1')
  ) %>% 
  rbind(
    a2 %>% 
  select(SECTOR, SUBSECTOR, TOTAL_VEH)  %>% 
    mutate(scenario = 'A2')
  ) 

write.csv(combined,"combined.csv")
