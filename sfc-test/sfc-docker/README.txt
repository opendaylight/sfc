This directory provides:

- An utility, 'dovs', to setup neutron networks where compute nodes
  are containerized openvswitch instances and VMs are isolated network namespaces 
  connected to such instances.
- A vagrant driven virtual environment prepared to run dovs

Startup and connect to the virtual environment as usually done with vagrant:

> vagrant up && vagrant ssh

This will:

- startup a CentOS virtual machine where...
- OVS with nsh support is built & installed
- a docker image with OVS is built
- basic networking setup is done

See dovs/README.txt for more info on dovs

