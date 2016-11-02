#!/bin/bash

source /vagrant/env.sh

rm -rf $OVS_CONF_DB
/etc/init.d/openvswitch-switch start

ovs-vsctl set-manager "tcp:${ODL_CONTROLLER}:6640"

host=$(hostname)
if [ "${host}"  == "${CLASSIFIER1_NAME}" -o "${host}"  == "${CLASSIFIER2_NAME}" ] ; then
    ovs-vsctl add-br br-sfc -- set bridge br-sfc protocols=OpenFlow10,OpenFlow12,OpenFlow13
    #ovs-vsctl set-controller br-sfc "tcp:${ODL_CONTROLLER}:6653"
fi
