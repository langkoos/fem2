#!/bin/bash
mvn clean install -DskipTests
cp target/*-jar-with-dependencies.jar ./
pandoc -f markdown -t html -o README.html README.md
rsync -av --exclude-from 'rsync_exclude_list.txt' ./ ~/Dropbox/FEM2_SNAPSHOT/
