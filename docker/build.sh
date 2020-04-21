#!/bin/bash
#aws s3 cp ../scenarios/FEM2TestDataOctober18 s3://urap-fem2/data --recursive
cd ../
mvn clean
mvn package -DskipTests=true
jar -uf target/au-flood-evacuation-0.11.0-SNAPSHOT-jar-with-dependencies.jar -C src/main/resources log4j.xml
cp target/au-flood-evacuation-0.11.0-SNAPSHOT-jar-with-dependencies.jar docker
cd docker
docker build -t fem2-app .
docker tag fem2-app langkoos/fem2:latest
docker push langkoos/fem2:latest
