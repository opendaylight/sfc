#!/bin/bash
vagrant destroy -f
vagrant up
vagrant ssh odl -c "nohup /vagrant/setup_odl.sh & sleep 1"
vagrant ssh classifier1  -c "nohup sudo /vagrant/setup_classifier.sh & sleep 1"
vagrant ssh classifier2  -c "nohup sudo /vagrant/setup_classifier.sh & sleep 1"
vagrant ssh sf1 -c "nohup sudo /vagrant/setup_sf.sh & sleep 1"
vagrant ssh sf2 -c "nohup sudo /vagrant/setup_sf.sh & sleep 1"
vagrant ssh sff1  -c "nohup sudo /vagrant/setup_sff.sh & sleep 1"
vagrant ssh sff2  -c "nohup sudo /vagrant/setup_sff.sh & sleep 1"

inprog=1

while [ $inprog -ne 0 ]
do
    echo "check system is ready"
    inprog=0
    vagrant ssh odl -c "sfc/sfc-karaf/target/assembly/bin/client -u karaf 'log:display' 2>/dev/null | grep 'Initialized RSP listener'"
    inprog+=$?
    vagrant ssh classifier1  -c "sudo ovs-vsctl show"
    inprog+=$?
    vagrant ssh classifier2  -c "sudo ovs-vsctl show"
    inprog+=$?
    vagrant ssh sff1  -c "sudo ovs-vsctl show"
    inprog+=$?
    vagrant ssh sff2  -c "sudo ovs-vsctl show"
    inprog+=$?
    vagrant ssh sf1  -c "ps -ef |grep sfc_agent.py"
    inprog+=$?
    vagrant ssh sf2  -c "ps -ef |grep sfc_agent.py"
    inprog+=$?
    sleep 30
done

vagrant ssh odl -c "/vagrant/setup.py"
sleep 60
vagrant ssh classifier1  -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh classifier2  -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh sff1 -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh sff2 -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh classifier1  -c "sudo ip netns exec app wget http://192.168.2.2"
