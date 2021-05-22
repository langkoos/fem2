#!/bin/bash
pwd
echo "batch index is $AWS_BATCH_JOB_ARRAY_INDEX"
empty=''
if [[ "$AWS_BATCH_JOB_ARRAY_INDEX" == "$empty" ]]; then
  AWS_BATCH_JOB_ARRAY_INDEX=$FIX
fi

echo "batch index is $AWS_BATCH_JOB_ARRAY_INDEX"

a='data/cluster/config_'
at='data/cluster/inputs_'
b='.xml'
bt='.txt'
c="$a$AWS_BATCH_JOB_ARRAY_INDEX$b"
ct="$at$AWS_BATCH_JOB_ARRAY_INDEX$bt"
echo $c
mkdir data
d='s3://urap-fem2/'
e='/data'
out='/output/output'
f="$d$DATADIR$e"
echo $f
g="$d$DATADIR$out$AWS_BATCH_JOB_ARRAY_INDEX"
echo $g

#aws s3 cp $f data/ --recursive

inputsfile="$d$DATADIR/$ct"
inputslocal="inputs_$AWS_BATCH_JOB_ARRAY_INDEX$bt"
aws s3 cp $inputsfile ./

configfile="$d$DATADIR/$c"
aws s3 cp $configfile data/config.xml

while read p; do
  echo "$p"
  mypath="$f/$p"
  echo  $mypath
  aws s3 cp $mypath data/$p || aws s3 cp $mypath data/$p/ --recursive
done <$inputslocal

#mv $c data/config.xml
ls data
#aws s3 cp data/config.xml s3://urap-fem2/output$AWS_BATCH_JOB_ARRAY_INDEX  --recursive
java -Xmx8g -Djava.awt.headless=true -cp /usr/local/bin/app.jar femproto.run.RunFromSource data/config.xml
rm -rf output/output/ITERS
gzip output/output/*.log
gzip output/output/*.txt
gzip output/output/*.csv

aws s3 cp output/output $g --recursive