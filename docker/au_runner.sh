#!/bin/bash
pwd
a='data/cluster/config_'
b='.xml'
c="$a$AWS_BATCH_JOB_ARRAY_INDEX$b"
echo $c
mkdir data
d='s3://urap-fem2/'
e='/data'
out='/output/output'
f="$d$DATADIR$e"
echo $f
g="$d$DATADIR$out$AWS_BATCH_JOB_ARRAY_INDEX"
echo $g

aws s3 cp $f data/ --recursive
mv $c data/config.xml
ls data
#aws s3 cp data/config.xml s3://urap-fem2/output$AWS_BATCH_JOB_ARRAY_INDEX  --recursive
java -Xmx1800m -Djava.awt.headless=true -cp /usr/local/bin/app.jar femproto.run.RunFromSource data/config.xml
rm -rf output/output/ITERS
gzip output/output/*.log
gzip output/output/*.txt
gzip output/output/*.csv

aws s3 cp output/output $g --recursive