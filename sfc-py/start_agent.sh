#!/bin/sh
# Need to instsall paramiko: sudo pip3.4 install paramiko

# auto-sff-name means agent will try to discover its SFF name dynamically during
# start-up and later when it receives a RSP request
python3 sfc_agent.py --rest --odl-ip-port 172.16.85.14:8181 --auto-sff-name
