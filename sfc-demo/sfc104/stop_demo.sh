#!/bin/bash

if [ $# -ne 0 ] ; then
    echo "Usage: ./$(basename $0)"
    exit -1
fi

root_dir=$(dirname $0)
if [ "${root_dir}" != "." ] ; then
    echo "Please run ./stop_demo.sh"
    exit -1
fi

source ./env.sh
vagrant halt -f
