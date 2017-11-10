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

Tested with vagrant 1.9.1 and VirtualBox 5.1.14.

See dovs/README.txt for more info on dovs.

In case you want to use an ODL controller with dovs, make sure the controller
is reachable through the host only network on which this virtual environment is
connected.

It also contains a set of tools for testing Service Function Chaining (SFC)

- Simple Service Function HTTP Header Enrichment:
See `sf_hhe/README.asciidoc`
- Test traffic Logical SFF with dovs + SFC config + sf_hhe
See `logical_sff/README.asciidoc`
