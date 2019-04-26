#!/bin/bash
#aws s3 cp ../scenarios/FEM2TestDataOctober18 s3://urap-fem2/data --recursive
docker build -t fem2-app .
docker tag fem2-app langkoos/fem2:latest
docker push langkoos/fem2:latest
