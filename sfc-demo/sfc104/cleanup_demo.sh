#!/bin/bash

source ./env.sh

#Check if SFC is started
karaf=$(sshpass -p karaf ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 8101 -l karaf ${LOCALHOST} system:name)
if [ $? -eq 0 ] ;  then
    ./common/cleanup_sfc.py
fi

vagrant ssh ${CLASSIFIER1_NAME} -c "sudo /vagrant/common/cleanup_classifier.sh"

vagrant ssh ${SFF1_NAME} -c "sudo /vagrant/common/cleanup_sff.sh"

vagrant ssh ${SF1_NAME} -c "sudo /vagrant/common/cleanup_sf.sh"

vagrant ssh ${SF2_NAME} -c "sudo /vagrant/common/cleanup_sf.sh"

vagrant ssh ${SFF2_NAME} -c "sudo /vagrant/common/cleanup_sff.sh"

vagrant ssh ${CLASSIFIER2_NAME} -c "sudo /vagrant/common/cleanup_classifier.sh"
