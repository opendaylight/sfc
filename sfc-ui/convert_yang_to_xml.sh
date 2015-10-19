#!/bin/bash

FILES=../sfc-model/src/main/yang/*

for filePath in $FILES
do
  fileName=${filePath##*/}
  echo "Converting $filePath file to ${fileName}.xml"
  pyang $filePath --ignore-errors -f yin -o module/src/main/resources/sfc/assets/yang2xml/${fileName}.xml
done