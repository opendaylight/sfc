#!/bin/bash
#vagrant up
#vagrant ssh odl -c "/vagrant/setup_odl.sh"
vagrant ssh classifier1  -c "sudo /vagrant/setup_classifier.sh"
vagrant ssh classifier2  -c "sudo /vagrant/setup_classifier.sh"
vagrant ssh sf1 -c "sudo /vagrant/setup_sf.sh"
vagrant ssh sf2 -c "sudo /vagrant/setup_sf.sh"
vagrant ssh sff1  -c "sudo /vagrant/setup_sff.sh"
vagrant ssh sff2  -c "sudo /vagrant/setup_sff.sh"
vagrant ssh odl -c "/vagrant/setup.sh"
vagrant ssh classifier1  -c "sudo ip netns exec app wget http://192.168.2.2"
