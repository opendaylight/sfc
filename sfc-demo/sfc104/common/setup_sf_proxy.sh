#!/bin/bash

source /vagrant/env.sh
/vagrant/common/cleanup_sf_proxy.sh

ifconfig eth2 0 down
INTFACE=eth1

python /vagrant/vxlan_tool.py -i ${INTFACE} --do=nsh_proxy -v on | tee /home/vagrant/vxlan_tool.log
