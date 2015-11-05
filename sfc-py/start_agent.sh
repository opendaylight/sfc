#!/bin/sh
# Need to instsall paramiko: sudo pip3.4 install paramiko

# auto-sff-name means agent will try to discover its SFF name dynamically during
# start-up and later when it receives a RSP request
python3.5 sfc/sfc_agent.py --rest --odl-ip-port 192.168.1.7:8181 --auto-sff-name
