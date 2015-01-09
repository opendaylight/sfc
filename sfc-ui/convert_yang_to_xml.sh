#!/bin/bash

FILES=../sfc-model/src/main/yang/*

for filePath in $FILES
do
  fileName=${filePath##*/}
  echo "Converting $filePath file to ${fileName}.xml"
  pyang $filePath --ignore-errors -f yin -o src/main/resources/pages/assets/yang2xml/${fileName}.xml
done
