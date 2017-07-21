#!/bin/bash
sudo mn -c >& /dev/null
sudo ./sfc_topology_nsh.py |& grep CHAIN
