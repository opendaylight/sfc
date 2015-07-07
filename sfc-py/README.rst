Service Function Chaining
=========================

Service Function Chaining provides the ability to define an ordered list of a
network services (e.g. firewalls, load balancers). These service are then
"stitched" together in the network to create a service chain. This project
provides the infrastructure (chaining logic, APIs) needed for ODL to provision
a service chain in the network and an end-user application for defining such
chains.

Instalation
===========

The installation has been tested on Ubuntu Linux.  

SFC needs to have Python 3.4 installed 

Before you run installation from Pypi,
be sure you have installed libnetfilter-queue

To run installation of libnetfilter-queue:

sudo apt-get install libnetfilter-queue-dev

all other dependecies are handeld in the SFC setup.

Example
=======  