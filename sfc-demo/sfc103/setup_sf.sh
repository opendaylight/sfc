#!/bin/bash
set -eux

apt-get update
apt-get install -y python3-pip
rm -rf /home/vagrant/sfc; cp -r /sfc /home/vagrant
cd /home/vagrant/sfc/sfc-py;
pip3 install -r requirements.txt
nohup python3.4 sfc/sfc_agent.py --rest --odl-ip-port 192.168.1.5:8181 &
