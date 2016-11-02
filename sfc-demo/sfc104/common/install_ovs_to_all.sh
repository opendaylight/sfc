#!/bin/bash

HTTPPROXY=$1
HTTPSPROXY=$2

source env.sh

vagrant ssh ${CLASSIFIER1_NAME} -c "sudo /vagrant/common/install_ovs.sh ${HTTPPROXY} ${HTTPSPROXY}"

if [ $? -ne 0 ] ; then
    echo "OVS installation failed on ${CLASSIFIER1_NAME}"
    exit -1
fi

vagrant ssh ${SFF1_NAME} -c "sudo /vagrant/common/install_ovs.sh ${HTTPPROXY} ${HTTPSPROXY}"

if [ $? -ne 0 ] ; then
    echo "OVS installation failed on ${SFF1_NAME}"
    exit -1
fi

vagrant ssh ${SFF2_NAME} -c "sudo /vagrant/common/install_ovs.sh ${HTTPPROXY} ${HTTPSPROXY}"

if [ $? -ne 0 ] ; then
    echo "OVS installation failed on ${SFF2_NAME}"
    exit -1
fi

vagrant ssh ${CLASSIFIER2_NAME} -c "sudo /vagrant/common/install_ovs.sh ${HTTPPROXY} ${HTTPSPROXY}"

if [ $? -ne 0 ] ; then
    echo "OVS installation failed on ${CLASSIFIER2_NAME}"
    exit -1
fi
exit 0
