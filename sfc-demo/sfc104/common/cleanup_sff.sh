#!/bin/bash

source /vagrant/env.sh
/vagrant/vpp/stop_vpp.sh
/vagrant/ovs_dpdk/stop_ovs_dpdk.sh
rm -f ${OVS_CONF_DB}
rm -f /var/log/honeycomb/honeycomb.log
