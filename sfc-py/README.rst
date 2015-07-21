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

1.- SFC needs to have Python 3.4 installed

2.- openssl-devel MUST BE INSTALLED as it is pip dependency 
    sudo apt-get install libssl-dev openssl
    
3.- Before you run installation from Pypi,
    be sure you have installed libnetfilter-queue.
    To run installation of libnetfilter-queue:
    sudo apt-get install libnetfilter-queue-dev

4.- be sure you have installed pip3
    to run installation of pip3 :
    sudo apt-get install python3-pip

5.- Finally the installation of the SFC package:
    sudo pip3 install sfc
    
All other dependencies are handled in the SFC setup.

There is still possibility to download 
the sfc-xxx.tar.gz file from Pypi repository,
unzip it , locate setup.py file and use the python installer
python3 setup.py install
 

Usage
=======
 
python3.4 sfc_agent --rest --nfq-class ---odl-ip-port=<ODL REST IP:port> --auto-sff-name

note:
root privileges are required if `--nfq-class` flag is used

SFC Agent

optional arguments:
  -h, --help            show this help message and exit
  --odl-get-sff         Get SFF from ODL
  --auto-sff-name       Automatically get SFF name
  --nfq-class           Flag to use NFQ Classifier
  -r, --rest            Flag to use REST
  --sff-name SFF_NAME   Set SFF name
  --odl-ip-port ODL_IP_PORT
                        Set ODL IP and port in form <IP>:<PORT>. Default is
                        localhost:8181
  --ovs-sff-cp-ip OVS_SFF_CP_IP
                        Set local SFF Open vSwitch IP. Default is 0.0.0.0
  --sff-os {XE,XR,OVS}  Set SFF switch OS
  --agent-port AGENT_PORT
                        Set SFC Agent port. Default is 5000

Example
=======
  sudo python3.4 sfc_agent.py --rest --odl-ip-port 192.168.33.11:8181 --auto-sff-name --nfq-class
  
  this command will run the sfc_agent using REST, trying to recognize its own SFF name and running 
  also NFQ classifier instance. 