#!/bin/bash

ovs-vsctl add-port br-sfc vxlangpe0 -- set interface vxlangpe0 type=vxlan options:exts=gpe options:remote_ip=flow options:dst_port=4790 options:key=flow
