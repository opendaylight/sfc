#!/bin/bash
name_hhe=$(ip addr | grep 'state UP' -A2 | tail -n1 | awk '{print $2}' | cut -f1  -d'/')
interface=$(ifconfig -a | grep eth0 | cut -d' ' -f 1)
server_port=8000
python3 ../sf_hhe/sf_hhe.py --eth_nsh -i ${interface} --nsh_spi 1 --port ${server_port}
