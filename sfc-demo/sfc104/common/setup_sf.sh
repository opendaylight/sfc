#!/bin/bash

source /vagrant/env.sh
ifconfig eth2 0 down
INTFACE=eth1
if [ "${1}" == "vpp" ] ; then
    INTFACE=eth2
    host=$(hostname)
    if [ "${host}"  == "${SF1_NAME}" ] ; then
        ifconfig ${INTFACE} ${SF1_VPP_IP} netmask 255.255.255.0 up
    elif [ "${host}"  == "${SF2_NAME}" ] ; then
        ifconfig ${INTFACE} ${SF2_VPP_IP} netmask 255.255.255.0 up
    fi
fi

python /vagrant/vxlan_tool.py -i ${INTFACE} --do=forward -v on | tee /home/vagrant/vxlan_tool.log
