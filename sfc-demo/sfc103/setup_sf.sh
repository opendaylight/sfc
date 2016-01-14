#!/bin/bash

apt-get update -y
apt-get install autoconf -y
apt-get install libtool -y
apt-get install git -y
apt-get install python3-flask requests netifaces -y
apt-get install libssl-dev openssl -y
apt-get install libnetfilter-queue-dev -y
apt-get install python3-pip -y
pip3 install sfc
apt-get install curl -y

cd /home/vagrant/; cp -r /sfc .

cd /home/vagrant/sfc/sfc-py
python3.4 sfc/sfc_agent.py --rest --odl-ip-port 192.168.1.5:8181
