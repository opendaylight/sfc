#!/bin/bash

vagrant up
vagrant ssh -c /vagrant/run_demo.sh
rm -fr ovs-debs
