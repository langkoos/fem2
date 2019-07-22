library(dplyr)
library(foreign)

subsectors <- read.dbf('test/input/femproto/scenario/source/FEM2_Test_Subsectorvehicles_2016_01/FEM2_Test_Subsectorvehicles_2016.dbf')

links <- read.dbf('test/input/femproto/scenario/source/FEM2__TEST_Links_Scenrio1A_2016_01/FEM2__TEST_Links_Scenrio1A_2016.dbf')

linklookup <- read.delim("test/input/femproto/scenario/linkLookup.txt")
sslookup <- read.delim("test/input/femproto/scenario/SSLookup.txt")

subsectors %>% 
  left_join(sslookup) %>% 
  write.dbf('test/input/femproto/scenario/source/FEM2_Test_Subsectorvehicles_2016_01/FEM2_Test_Subsectorvehicles_2016.dbf',factor2char = T,max_nchar = 254)

links %>% 
  left_join(linklookup) %>% 
  write.dbf('test/input/femproto/scenario/source/FEM2__TEST_Links_Scenrio1A_2016_01/FEM2__TEST_Links_Scenrio1A_2016.dbf',factor2char = T,max_nchar = 254)
  