#!/bin/bash
sudo mn -c >& /dev/null
sudo ./sfc_topology_symmetric_chain.py |& grep CHAIN
