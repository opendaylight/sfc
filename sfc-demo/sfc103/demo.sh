#!/bin/bash

vagrant destroy -f
vagrant up

inprog=1

while [ $inprog -ne 0 ]
do
    true &> /dev/null | vagrant ssh odl -c "cat sfc.prog"
    true &> /dev/null | \
        vagrant ssh odl -c "true &> /dev/null | sfc/sfc-karaf/target/assembly/bin/client -u karaf 'feature:list -i'" | \
        grep odl-sfc
    inprog=$?

    sleep 60
done

vagrant ssh odl -c "/vagrant/setup.py"

#wait for openflow effective
sleep 60

vagrant ssh classifier1  -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh classifier2  -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh sff1 -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh sff2 -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh classifier1  -c "sudo ip netns exec app wget http://192.168.2.2"
