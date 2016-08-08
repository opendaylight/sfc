#!/bin/bash

vagrant destroy -f
vagrant up
vagrant ssh -c /vagrant/run_demo.sh
