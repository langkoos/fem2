#!/bin/bash
mvn clean
mvn package -DskipTests
#couldnt get the maven packaging to overwrite log4j.xml so doing it manually
jar -uf target/au-flood-evacuation-0.11.0-SNAPSHOT-jar-with-dependencies.jar -C src/main/resources log4j.xml
#cp target/*-jar-with-dependencies.jar ./
#pandoc -f markdown -t html -o README.html README.md
#rsync -av scenarios/FEM2TestDataOctober18/clusterConfigs ../fem2-scenarios/
#rsync -av scenarios/EIS_May2019/Scenario* ../fem2-scenarios/
#rsync -av scenarios/FEM2TestDataOctober18/wma-flood-events ../fem2-scenarios/
rsync -av target/au-flood-evacuation-0.11.0-SNAPSHOT-jar-with-dependencies.jar ../fem2-scenarios/
git log -n 1 > ../fem2-scenarios/commit-hash.txt
#cd ../fem2-scenarios/
#git add *
#git add .
#git commit -m 'autocommit after main repo commit'
#git push
# rsync -avz --exclude-from 'rsync_exclude_list.txt' -e "ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" --progress ./ ubuntu@ec2-13-229-216-141.ap-southeast-1.compute.amazonaws.com:/home/ubuntu/FEM

