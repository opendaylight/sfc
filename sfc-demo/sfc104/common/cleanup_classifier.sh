#!/bin/bash

source /vagrant/env.sh
/vagrant/vpp/stop_vpp.sh
/vagrant/common/cleanup_veth.sh
/vagrant/ovs_dpdk/stop_ovs_dpdk.sh
rm -f ${OVS_CONF_DB}
sed -i 's/#net.ipv4.ip_forward=1/net.ipv4.ip_forward=1/g' /etc/sysctl.conf
