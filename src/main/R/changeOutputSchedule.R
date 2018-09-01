library(readr)
library(dplyr)
library(ggplot2)
# scheduleFromExperiencedPlans <- read_csv("test/output/femproto/prepare/evacuationscheduling/EvacuationScheduleFromExperiencedPlansTest/test/scheduleFromExperiencedPlans.csv") %>% 
# scheduleFromExperiencedPlans <- read_csv("test/input/femproto/prepare/evacuationscheduling/changedEvacSchedule.csv") %>% 
scheduleFromExperiencedPlans <- read_csv("test/output/femproto/run/RunFromSchedule/runFromSchedule/scheduleFromExperiencedPlans.csv") %>%
  mutate(
    start = TIME/3600,
    end = (TIME + DURATION)/3600     
         )

scheduleFromExperiencedPlans <- 
  within(
    scheduleFromExperiencedPlans,
    subsec <- factor(SUBSECTOR,
                     levels = SUBSECTOR
                     )
    
  )

#edit(scheduleFromExperiencedPlans)

ggplot(scheduleFromExperiencedPlans,aes(colour = VEHICLES)) +
  geom_segment(aes(x=start, xend=end, y=SUBSECTOR, yend=SUBSECTOR),size=1) +
  xlab("Time") +
  theme_classic()

outSchedule <- 
  scheduleFromExperiencedPlans %>% 
  mutate(
    TIME = as.integer(ifelse(end >10 ,0,runif(n = n(),min = 18000,max=36000))),
    start = TIME/3600,
    end = start + VEHICLES/600,
    DURATION = as.integer(3600*(end-start))
    )

ggplot(outSchedule, aes(colour = VEHICLES)) +
  geom_segment(aes(x=start, xend=end, y=subsec, yend=subsec),size=1) +
  xlab("Time") +
  theme_classic()

write.csv(outSchedule,file = "test/input/femproto/prepare/evacuationscheduling/changedEvacSchedule.csv",row.names = F)
