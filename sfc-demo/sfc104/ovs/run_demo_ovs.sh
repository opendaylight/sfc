#!/bin/bash

HTTPPROXY=$1
HTTPSPROXY=$2
source env.sh

# Start all the VMs, except SF2_PROXY_NAME
vagrant up ${CLASSIFIER1_NAME}
vagrant up ${SFF1_NAME}
vagrant up ${SF1_NAME}
vagrant up ${SF2_NAME}
vagrant up ${SFF2_NAME}
vagrant up ${CLASSIFIER2_NAME}

./common/install_ovs_to_all.sh ${HTTPPROXY} ${HTTPSPROXY}
if [ $? -ne 0 ] ; then
    exit -1
fi

echo -e "\nSetting up ${CLASSIFIER1_NAME}"
vagrant ssh ${CLASSIFIER1_NAME} -c "sudo /vagrant/ovs/setup_classifier_ovs.sh"

echo -e "\nSetting up ${SFF1_NAME}"
vagrant ssh ${SFF1_NAME} -c "sudo /vagrant/ovs/setup_sff_ovs.sh"

echo -e "\nSetting up ${SF1_NAME}"
vagrant ssh ${SF1_NAME} -c "sudo nohup /vagrant/common/setup_sf.sh & sleep 1"

echo -e "\nSetting up ${SF2_NAME}"
vagrant ssh ${SF2_NAME} -c "sudo nohup /vagrant/common/setup_sf.sh & sleep 1"

echo -e "\nSetting up ${SFF2_NAME}"
vagrant ssh ${SFF2_NAME} -c "sudo /vagrant/ovs/setup_sff_ovs.sh"

echo -e "\nSetting up ${CLASSIFIER2_NAME}"
vagrant ssh ${CLASSIFIER2_NAME} -c "sudo /vagrant/ovs/setup_classifier_ovs.sh"
