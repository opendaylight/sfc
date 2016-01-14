#!/bin/bash
vagrant up
vagrant ssh odl -c "nohup /vagrant/setup_odl.sh & sleep 1"
vagrant ssh classifier1  -c "nohup sudo /vagrant/setup_classifier.sh & sleep 1"
vagrant ssh classifier2  -c "nohup sudo /vagrant/setup_classifier.sh & sleep 1"
vagrant ssh sf1 -c "nohup sudo /vagrant/setup_sf.sh & sleep 1"
vagrant ssh sf2 -c "nohup sudo /vagrant/setup_sf.sh & sleep 1"
vagrant ssh sff1  -c "nohup sudo /vagrant/setup_sff.sh & sleep 1"
vagrant ssh sff2  -c "nohup sudo /vagrant/setup_sff.sh & sleep 1"

inprog=1

while [ $inprog -eq 1 ]
do
    echo "check system is ready"
    vagrant ssh odl -c "sfc/sfc-karaf/target/assembly/bin/client -u karaf 'log:display' 2>/dev/null | grep 'SfcScfOfRenderer successfully'"
    inprog=$?
    sleep 30
done

vagrant ssh odl -c "/vagrant/setup.py"
sleep 30
vagrant ssh classifier1  -c "sudo ip netns exec app wget http://192.168.2.2"
