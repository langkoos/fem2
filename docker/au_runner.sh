#!/bin/bash
pwd
a='data/cluster/config_'
b='.xml'
c="$a$AWS_BATCH_JOB_ARRAY_INDEX$b"
echo $c
mkdir data
aws s3 cp s3://urap-fem2/data data/ --recursive
mv $c data/config.xml
ls data
#aws s3 cp data/config.xml s3://urap-fem2/output$AWS_BATCH_JOB_ARRAY_INDEX  --recursive
java -Xmx1800m -Djava.awt.headless=true -cp /usr/local/bin/app.jar femproto.run.RunMatsim4FloodEvacuation data/config.xml
rm -rf data/output/ITERS
gzip output/*.log
gzip output/*.txt
gzip output/*.csv

aws s3 cp output s3://urap-fem2/output/output$AWS_BATCH_JOB_ARRAY_INDEX  --recursive