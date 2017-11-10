#!/bin/bash

FILES=../sfc-model/src/main/yang/*

for filePath in $FILES
do
  fileName=${filePath##*/}
  echo "Converting $filePath file to ${fileName}.xml"
  pyang $filePath -f yin -p module/src/main/resources/sfc/assets/yang:`dirname ${filePath}` -o module/src/main/resources/sfc/assets/yang2xml/${fileName}.xml
done