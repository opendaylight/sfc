#!/bin/bash

platform='unknown'
unamestr=`uname`
if [[ "$unamestr" == 'Linux' ]]; then
   platform='linux'
elif [[ "$unamestr" == *CYGWIN* ]]; then
   platform='linux'
elif [[ "$unamestr" == 'Darwin' ]]; then
   platform='osx'
fi

if [[ $platform == 'linux' ]]; then
   fullpath=`readlink -f $0`
elif [[ $platform == 'osx' ]]; then
   TARGET_FILE=$0
   cd `dirname "$TARGET_FILE"`
   TARGET_FILE=`basename $TARGET_FILE`

   # Iterate down a (possible) chain of symlinks
   while [ -L "$TARGET_FILE" ]
   do
       TARGET_FILE=`readlink "$TARGET_FILE"`
       cd `dirname "$TARGET_FILE"`
       TARGET_FILE=`basename "$TARGET_FILE"`
   done

   # Compute the canonicalized name by finding the physical path
   # for the directory we're in and appending the target file.
   PHYS_DIR=`pwd -P`
   RESULT=$PHYS_DIR/$TARGET_FILE
   fullpath=$RESULT
fi

basedir=`dirname "${fullpath}"`

$basedir/../node_modules/karma-cli/bin/karma start $basedir/karma-unit.tpl.js
