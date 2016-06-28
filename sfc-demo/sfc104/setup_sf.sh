#!/bin/bash

apt-get update -y
apt-get install autoconf -y
apt-get install libtool -y
apt-get install git -y
apt-get install python3-flask requests netifaces -y
apt-get install libssl-dev openssl -y
apt-get install libnetfilter-queue-dev -y
apt-get install python3-pip python3-flask -y
pip3 install sfc paramiko flask
apt-get install curl -y

rm -rf /home/vagrant/sfc; cp -r /sfc /home/vagrant
cd /home/vagrant/sfc/sfc-py; python3.4 sfc/sfc_agent.py --rest --odl-ip-port 192.168.1.5:8181
