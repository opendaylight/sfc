This directory provides:

- An utility, 'dovs', to simulate neutron networks where compute nodes
  are containerized openvswitch instances and VMs are isolated network namespaces 
  connected to such instances.
- A vagrant driven virtual environment prepared to run dovs

Startup and connect to the virtual environment:

> vagrant up && vagrant ssh

This will:

- startup a CentOS virtual machine where
- OVS with nsh support is built & installed
- OVS docker image is built & installed
- install pipework, a well-known utility to help with docker networking
- setup dovsbr0 bridge which is used by dovs 

See dovs/README for more info on dovs

