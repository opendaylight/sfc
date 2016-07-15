#!/bin/bash

vagrant destroy -f
vagrant up

inprog=1

while [ $inprog -ne 0 ]
do
    true &> /dev/null | vagrant ssh odl -c "cat sfc.prog"
    inprog=`true &> /dev/null | vagrant ssh odl -c "/vagrant/sfc_ready.sh"`
    sleep 60
done

true &> /dev/null | vagrant ssh odl -c "/vagrant/setup_sfc.py"

#wait for openflow effective
sleep 60

true &> /dev/null | vagrant ssh classifier1  -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
true &> /dev/null | vagrant ssh classifier2  -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
true &> /dev/null | vagrant ssh sff1 -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
true &> /dev/null | vagrant ssh sff2 -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
true &> /dev/null | vagrant ssh classifier1  -c "sudo ip netns exec app wget http://192.168.2.2"

#wait for openflow effective
sleep 60

true &> /dev/null | vagrant ssh odl -c "/vagrant/update_sfc.py"
true &> /dev/null | vagrant ssh classifier1  -c "sudo ip netns exec app wget http://192.168.2.2"
