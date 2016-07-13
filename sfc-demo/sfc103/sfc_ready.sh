#!/bin/bash

if [ -f sfc/sfc-karaf/target/assembly/bin/client ]
then
    sfc/sfc-karaf/target/assembly/bin/client -u karaf 'feature:list -i' 2>&1 | grep odl-sfc > /dev/null
    echo $?
else
    echo 1
fi
